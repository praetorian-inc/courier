/*
 * Copyright Praetorian Security Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package burp.controller.sessionRecorder;

import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import burp.serialization.dto.HttpRequestDto;
import burp.serialization.dto.HttpResponseDto;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Base64;
import burp.controller.LogController;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import burp.model.Webflow;
import burp.utils.SafeLogFormatter;



public class Utils {

    private static final long CORRELATION_WINDOW_MS = 100;
    private static final int MAX_NETWORK_BODY_BYTES = 1024 * 1024;

    static HttpRequestDto convertPlaywrightRequestToDto(Request request, LogController logger) {
        try {
            String method = request.method();
            String url = request.url();
            
            // Extract path from URL
            String path = "/";
            try {
                java.net.URI uri = java.net.URI.create(url);
                path = uri.getPath();
                if (path == null || path.isEmpty()) {
                    path = "/";
                }
                if (uri.getQuery() != null) {
                    path += "?" + uri.getQuery();
                }
            } catch (Exception e) {
                logger.logDebug("Failed to parse URL for path extraction: " + e.getMessage());
            }
            
            Map<String, String> headers = safeHeaders(request::allHeaders, logger, "request");
            List<Map<String, String>> headersList = headerList(headers);
            String body = requestBody(request, headers, logger);
            
            // Create HttpRequestDto
            HttpRequestDto dto = new HttpRequestDto(
                body,           // body
                0,              // messageId (not applicable for Playwright requests)
                true,           // inScope (assume in scope for webflow recording)
                method,         // method
                path,           // path
                headersList,    // headers
                url             // url
            );
            
            logger.logDebug("Successfully converted Playwright request to HttpRequestDto: "
                    + method + " " + SafeLogFormatter.origin(url));
            return dto;
            
        } catch (Exception e) {
            logger.logError("Error converting Playwright request to HttpRequestDto: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Converts Playwright Response to HttpResponseDto for serialization and upload to Guard
     */
    static HttpResponseDto convertPlaywrightResponseToDto(Response response, LogController logger) {
        try {
            int status = response.status();
            String url = response.url();
            
            Map<String, String> headers = safeHeaders(response::allHeaders, logger, "response");
            List<Map<String, String>> headersList = headerList(headers);
            String body = responseBody(response, headers, logger);
            
            // Extract path from URL
            String path = "/";
            try {
                java.net.URI uri = java.net.URI.create(url);
                path = uri.getPath();
                if (path == null || path.isEmpty()) {
                    path = "/";
                }
                if (uri.getQuery() != null) {
                    path += "?" + uri.getQuery();
                }
            } catch (Exception e) {
                logger.logDebug("Failed to parse URL for path extraction: " + e.getMessage());
            }
            
            // Create HttpResponseDto
            // Note: HttpResponseDto uses the same structure as HttpRequestDto
            HttpResponseDto dto = new HttpResponseDto(
                body,           // body
                0,              // messageId (not applicable for Playwright responses)
                true,           // inScope (assume in scope for webflow recording)
                response.request() == null ? "" : response.request().method(),
                path,           // path
                headersList,    // headers
                url,            // url
                status          // statusCode
            );
            
            logger.logDebug("Successfully converted Playwright response to HttpResponseDto: "
                    + status + " " + SafeLogFormatter.origin(url));
            return dto;
            
        } catch (Exception e) {
            logger.logError("Error converting Playwright response to HttpResponseDto: " + e.getMessage());
            return null;
        }
    }

    private static Map<String, String> safeHeaders(HeaderSupplier supplier,
            LogController logger, String messageType) {
        try {
            Map<String, String> headers = supplier.get();
            return headers == null ? Map.of() : Map.copyOf(headers);
        } catch (Exception exception) {
            logger.logDebug("Failed to extract headers from " + messageType + ": "
                    + exception.getMessage());
            return Map.of();
        }
    }

    @FunctionalInterface
    private interface HeaderSupplier {
        Map<String, String> get();
    }

    private static List<Map<String, String>> headerList(Map<String, String> headers) {
        List<Map<String, String>> result = new ArrayList<>();
        headers.forEach((name, value) -> {
            Map<String, String> header = new LinkedHashMap<>();
            header.put(name, value);
            result.add(header);
        });
        return result;
    }

    private static String requestBody(Request request, Map<String, String> headers,
            LogController logger) {
        if (exceedsBodyLimit(contentLength(headers))) {
            logger.logDebug("Request body exceeds capture limit; omitting body");
            return "";
        }
        try {
            byte[] bytes = request.postDataBuffer();
            return encodeBody(bytes, contentType(headers), logger, "request");
        } catch (Exception exception) {
            logger.logDebug("Failed to extract body from request: " + exception.getMessage());
            return "";
        }
    }

    private static String responseBody(Response response, Map<String, String> headers,
            LogController logger) {
        long declaredLength = contentLength(headers);
        long measuredLength = -1;
        try {
            if (response.request() != null && response.request().sizes() != null) {
                measuredLength = response.request().sizes().responseBodySize;
            }
        } catch (Exception exception) {
            logger.logDebug("Failed to inspect response size: " + exception.getMessage());
        }
        if (exceedsBodyLimit(declaredLength) || exceedsBodyLimit(measuredLength)) {
            logger.logDebug("Response body exceeds capture limit; omitting body");
            return "";
        }
        if (declaredLength < 0 && measuredLength < 0) {
            logger.logDebug("Response body size is unknown; omitting body");
            return "";
        }
        try {
            return encodeBody(response.body(), contentType(headers), logger, "response");
        } catch (Exception exception) {
            logger.logDebug("Failed to extract body from response: " + exception.getMessage());
            return "";
        }
    }

    private static String encodeBody(byte[] bytes, String contentType,
            LogController logger, String messageType) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (bytes.length > MAX_NETWORK_BODY_BYTES) {
            logger.logDebug("Captured " + messageType + " body exceeded limit; omitting body");
            return "";
        }
        return burp.utils.Utils.isBinaryContentType(contentType)
                ? Base64.getEncoder().encodeToString(bytes)
                : new String(bytes, StandardCharsets.UTF_8);
    }

    private static long contentLength(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if ("content-length".equalsIgnoreCase(header.getKey())) {
                try {
                    return Long.parseLong(header.getValue());
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static String contentType(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if ("content-type".equalsIgnoreCase(header.getKey())) {
                return header.getValue();
            }
        }
        return "";
    }

    private static boolean exceedsBodyLimit(long length) {
        return length > MAX_NETWORK_BODY_BYTES;
    }

    /**
     * Correlates an HTTP request with recent webflow steps based on timestamp
     */
    public static void correlateRequestResponseWithSteps(Request request_playwright, LogController logger, Webflow webflow) {
        try {

            logger.logDebug("Correlating request with steps from "
                    + SafeLogFormatter.origin(request_playwright.url()));
            
            double requestTimestamp = requestTimestamp(request_playwright);
            logger.logDebug("Network request: " + request_playwright.method() + " "
                    + SafeLogFormatter.origin(request_playwright.url())
                    + (Double.isNaN(requestTimestamp)
                            ? " (timing unavailable)"
                            : " (request timestamp: " + requestTimestamp + "ms)"));

            // Convert Playwright Request to HttpRequestDto
            HttpRequestDto httpRequest = convertPlaywrightRequestToDto(request_playwright, logger);
            Response playwrightResponse = request_playwright.response();
            HttpResponseDto httpResponse = playwrightResponse == null
                    ? null : convertPlaywrightResponseToDto(playwrightResponse, logger);
            if (httpRequest == null) {
                logger.logError("Failed to convert Playwright request to HttpRequestDto");
                return;
            }
            
            // Correlate with WebflowSteps within the correlation window
            List<Webflow.WebflowStep> steps = webflow.getSteps();
            if (Double.isNaN(requestTimestamp) || steps == null || steps.isEmpty()) {
                // No steps to correlate with, add to uncorrelated requests
                webflow.addUncorrelatedRequest(httpRequest);
                if (httpResponse != null) {
                    webflow.addUncorrelatedResponse(httpResponse);
                }
                logger.logDebug("Added uncorrelated request/response from "
                        + SafeLogFormatter.origin(request_playwright.url()));
                return;
            }
            
            boolean correlated = false;
            for (int i = steps.size() - 1; i >= 0; i--) { // Check most recent steps first
                Webflow.WebflowStep step = steps.get(i);
                double step_timestamp = step.getTimestamp();
                if (step_timestamp == 0) continue;
                
                // Calculate time difference between browser timestamp and step timestamp
                double timeDiff = Math.abs(requestTimestamp - step_timestamp);
                
                if (timeDiff <= CORRELATION_WINDOW_MS) {
                    step.addCorrelatedRequest(httpRequest);
                    if (httpResponse != null) {
                        step.addCorrelatedResponse(httpResponse);
                    }
                    logger.logDebug("Correlated request with step " + step.getOrder() + 
                                   " (time diff: " + String.format("%.2f", timeDiff) + "ms)");
                    correlated = true;
                    break;
                }
            }
            
            if (!correlated) {
                // No correlation found, add to uncorrelated requests
                webflow.addUncorrelatedRequest(httpRequest);
                if (httpResponse != null) {
                    webflow.addUncorrelatedResponse(httpResponse);
                }
                logger.logDebug("Added uncorrelated request/response: "
                        + request_playwright.method() + " "
                        + SafeLogFormatter.origin(request_playwright.url()));
            }
            
        } catch (Exception e) {
            logger.logError("Error correlating request: " + e.getMessage());
        }
    }

    private static double requestTimestamp(Request request) {
        try {
            com.microsoft.playwright.options.Timing timing = request.timing();
            return timing == null ? Double.NaN : timing.requestStart + timing.startTime;
        } catch (Exception exception) {
            return Double.NaN;
        }
    }

    public static Map<String, String> findPlaywrightEnvironment(LogController logger) {
        String configuredPath = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
        if (configuredPath != null && !configuredPath.isBlank()) {
            logger.logDebug("Using PLAYWRIGHT_BROWSERS_PATH from the process environment");
            return Map.of("PLAYWRIGHT_BROWSERS_PATH", configuredPath);
        }

        String userHome = System.getProperty("user.home");
        String[] possibleBrowserPaths = {
                Path.of(userHome, ".cache", "ms-playwright").toString(),
                Path.of(userHome, "Library", "Caches", "ms-playwright").toString(),
                "/usr/local/lib/node_modules/playwright/.local-browsers",
                "/usr/local/lib/node_modules/@playwright/test/.local-browsers"
        };

        logger.logDebug("Checking for Playwright browsers");
        for (String path : possibleBrowserPaths) {
            if (new java.io.File(path).isDirectory()) {
                logger.logDebug("Using discovered Playwright browser cache");
                return Map.of("PLAYWRIGHT_BROWSERS_PATH", path);
            }
        }
        logger.logDebug("No external Playwright browser cache found; using Playwright defaults");
        return Map.of();
    }

    public static String getDebugInfo(LogController logger, Object who) {
        // Debug: Print system and environment info before Playwright initialization
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("=== PLAYWRIGHT DEBUG INFO ===\n");
        debugInfo.append("Java version: ").append(System.getProperty("java.version")).append('\n');
        debugInfo.append("OS: ").append(System.getProperty("os.name")).append(' ')
                .append(System.getProperty("os.arch")).append('\n');
        debugInfo.append("ClassLoader: ").append(who.getClass().getClassLoader().getClass().getName()).append('\n');
        debugInfo.append("Context ClassLoader: ")
                .append(Thread.currentThread().getContextClassLoader().getClass().getName()).append('\n');

        // Check for Playwright environment variables
        String[] playwrightEnvVars = {
                "PLAYWRIGHT_BROWSERS_PATH",
                "PLAYWRIGHT_DRIVER_PATH",
                "PLAYWRIGHT_NODEJS_PATH",
                "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD",
                "PLAYWRIGHT_EXECUTABLE_PATH"
        };

        for (String var : playwrightEnvVars) {
            debugInfo.append("ENV ").append(var).append(": ")
                    .append(System.getenv(var) == null ? "(not set)" : "(set)")
                    .append('\n');
        }
        return debugInfo.toString();
   }
}
