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

package burp.utils;

import burp.api.montoya.MontoyaApi;
import burp.controller.LogController;
import burp.model.NetworkInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SessionManager {
    private final AtomicReference<String> sessionJwt = new AtomicReference<>("");
    private final String chariotApiEndpoint;
    private final String apiKey;
    private final String apiSecret;
    private final String projectName;
    private final String targetApplication;
    private final Lock tokenLock = new ReentrantLock(true);
    private final MontoyaApi api;
    private final HttpClient httpClient;

    private volatile String selectedAccount = "";
    private volatile boolean enabled;

    public SessionManager(String chariotApiEndpoint, String apiKey, String apiSecret, String projectName,
            String targetApplication, MontoyaApi api) {
        this(chariotApiEndpoint, apiKey, apiSecret, projectName, targetApplication, api, SharedHttpClient.get());
    }

    SessionManager(String chariotApiEndpoint, String apiKey, String apiSecret, String projectName,
            String targetApplication, MontoyaApi api, HttpClient httpClient) {
        this.chariotApiEndpoint = requireSecureEndpoint(chariotApiEndpoint);
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.projectName = projectName;
        this.targetApplication = targetApplication;
        this.api = api;
        this.httpClient = httpClient;
    }

    public Map<NetworkInterface, String> getProxyServerUrls() throws Exception {
        Map<NetworkInterface, String> proxyServerUrls = new EnumMap<>(NetworkInterface.class);
        try {
            String listeners = api.burpSuite().exportProjectOptionsAsJson("proxy.request_listeners");
            JSONArray listenersArray = new JSONObject(listeners)
                    .getJSONObject("proxy")
                    .getJSONArray("request_listeners");

            for (int index = 0; index < listenersArray.length(); index++) {
                JSONObject listener = listenersArray.getJSONObject(index);
                if (!listener.optBoolean("running", false)) {
                    continue;
                }

                int port = listener.optInt("listener_port", 0);
                switch (listener.optString("listen_mode", "")) {
                    case "loopback_only" -> proxyServerUrls.put(NetworkInterface.LOOPBACK,
                            "http://127.0.0.1:" + port);
                    case "all_interfaces" -> proxyServerUrls.put(NetworkInterface.ALL_INTERFACES,
                            "http://127.0.0.1:" + port);
                    case "specific_address" -> proxyServerUrls.put(NetworkInterface.SPECIFIC_ADDRESS,
                            "http://" + proxyHost(listener.optString(
                                    "listen_specific_address", "127.0.0.1")) + ":" + port);
                    default -> {
                    }
                }
            }
        } catch (Exception exception) {
            throw new Exception("Error getting proxy server URLs: " + exception.getMessage(), exception);
        }
        return proxyServerUrls;
    }

    public static boolean isJwtValid(String jwt) {
        if (jwt == null || jwt.isEmpty()) {
            return false;
        }
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            JSONObject payload = new JSONObject(new String(
                    Base64.getUrlDecoder().decode(padBase64(parts[1])), StandardCharsets.UTF_8));
            if (!payload.has("exp")) {
                return true;
            }
            return Instant.now().plusSeconds(60).isBefore(Instant.ofEpochSecond(payload.getLong("exp")));
        } catch (Exception exception) {
            return false;
        }
    }

    public String getChariotToken(LogController logger) {
        tokenLock.lock();
        try {
            String cachedToken = sessionJwt.get();
            if (!cachedToken.isEmpty() && isJwtValid(cachedToken)) {
                return cachedToken;
            }

            String requestBody = "grant_type=client_credentials&client_id="
                    + encodeFormValue(apiKey) + "&client_secret=" + encodeFormValue(apiSecret);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chariotApiEndpoint + "/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    logger.logError("Error getting Guard token: HTTP " + response.statusCode());
                    sessionJwt.set("");
                    return "";
                }

                JSONObject body = new JSONObject(response.body());
                String token = body.optString("access_token",
                        body.optString("token", body.optString("IdToken", "")));
                sessionJwt.set(token);
                if (!token.isEmpty()) {
                    logger.logInfo("New Guard token retrieved successfully");
                }
                return token;
            } catch (Exception exception) {
                logger.logError("Error getting Guard token: " + exception.getMessage());
                sessionJwt.set("");
                return "";
            }
        } finally {
            tokenLock.unlock();
        }
    }

    public List<String> fetchAccounts(LogController logger) {
        List<String> accountNames = new ArrayList<>();
        String token = getChariotToken(logger);
        if (token.isEmpty()) {
            logger.logError("Cannot fetch accounts: no valid token");
            return accountNames;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(chariotApiEndpoint + "/my?key=%23account"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.logError("Failed to fetch accounts: HTTP " + response.statusCode());
                return accountNames;
            }

            JSONArray accounts = new JSONObject(response.body()).optJSONArray("accounts");
            if (accounts == null) {
                return accountNames;
            }
            LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
            for (int index = 0; index < accounts.length(); index++) {
                String name = accounts.getJSONObject(index).optString("name", "");
                if (!name.isEmpty()) {
                    uniqueNames.add(name);
                }
            }
            accountNames.addAll(uniqueNames);
            logger.logInfo("Fetched " + accountNames.size() + " accessible accounts");
        } catch (Exception exception) {
            logger.logError("Error fetching accounts: " + exception.getMessage());
        }
        return accountNames;
    }

    public String getChariotApiEndpoint() {
        return chariotApiEndpoint;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getTargetApplication() {
        return targetApplication;
    }

    public String getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(String account) {
        selectedAccount = account == null ? "" : account;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    static String requireSecureEndpoint(String endpoint) {
        URI uri;
        try {
            uri = URI.create(endpoint == null ? "" : endpoint.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Guard endpoint must be a valid HTTPS URL", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Guard endpoint must be a valid HTTPS URL");
        }
        String normalized = uri.toString();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static String encodeFormValue(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String proxyHost(String address) {
        return address != null && address.contains(":")
                && !(address.startsWith("[") && address.endsWith("]"))
                ? "[" + address + "]" : address;
    }

    private static String padBase64(String value) {
        int padding = (4 - value.length() % 4) % 4;
        return value + "=".repeat(padding);
    }
}
