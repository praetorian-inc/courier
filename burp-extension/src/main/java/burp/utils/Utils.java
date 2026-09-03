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
import burp.controller.ConfigurationController;
import burp.controller.LogController;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class Utils {
    private static final HttpClient HTTP_CLIENT = SharedHttpClient.get();
    private static final List<String> BINARY_CONTENT_TYPE_PREFIXES = List.of("image/", "video/", "audio/");
    private static final Set<String> BINARY_CONTENT_TYPES = Set.of(
            "application/octet-stream", "application/pdf", "application/zip", "application/x-gzip",
            "application/x-bzip2", "application/x-tar", "application/x-rar-compressed",
            "application/x-7z-compressed", "application/x-java-archive", "application/x-shockwave-flash",
            "application/x-font-ttf", "application/x-font-woff", "application/x-font-woff2",
            "application/x-font-otf", "application/x-font-eot");
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "ttf", "woff", "woff2", "eot", "otf", "svg", "png", "jpg", "jpeg", "gif", "ico",
            "pdf", "mp3", "mp4", "webp", "webm", "bin", "swf", "bmp", "tif", "tiff", "dat");

    private Utils() {
    }

    public static boolean isBinaryContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return BINARY_CONTENT_TYPES.contains(normalized)
                || BINARY_CONTENT_TYPE_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    public static boolean isBinaryData(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        if (data.indexOf('\0') >= 0) {
            return true;
        }
        int checkedCharacters = Math.min(data.length(), 1000);
        int nonPrintableCharacters = 0;
        for (int index = 0; index < checkedCharacters; index++) {
            char character = data.charAt(index);
            if (character < 32 && character != '\n' && character != '\r' && character != '\t') {
                nonPrintableCharacters++;
            }
        }
        return nonPrintableCharacters * 100 / checkedCharacters > 10;
    }

    public static boolean isBinaryFileExtension(String extension) {
        if (extension == null) {
            return false;
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return BINARY_EXTENSIONS.contains(normalized);
    }

    public static boolean isInScope(String url, ConfigurationController configuration, MontoyaApi api) {
        return !configuration.isRespectScopeEnabled() || api.scope().isInScope(url);
    }

    public static CompletableFuture<Boolean> uploadJsonFile(String jsonData, String uploadUrl, String source,
            LogController logger) {
        String finalSource = source == null ? "unknown" : source;
        if (jsonData == null || jsonData.isBlank() || uploadUrl == null || uploadUrl.isBlank()) {
            logger.logError("JSON upload requires non-empty data and URL");
            return CompletableFuture.completedFuture(false);
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonData, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("X-Source", finalSource)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } catch (Exception exception) {
            logger.logError("JSON upload error for source '" + finalSource + "': " + exception.getMessage());
            return CompletableFuture.completedFuture(false);
        }

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                    if (!success) {
                        logger.logError("JSON upload failed for source '" + finalSource + "': HTTP "
                                + response.statusCode());
                    }
                    return success;
                })
                .exceptionally(exception -> {
                    logger.logError("JSON upload error for source '" + finalSource + "': " + exception.getMessage());
                    return false;
                });
    }

    public static CompletableFuture<String> getPresignedURL(String bearerToken, String endpoint, String name,
            LogController logger) {
        return getPresignedURL(bearerToken, endpoint, name, "", logger);
    }

    public static CompletableFuture<String> getPresignedURL(String bearerToken, String endpoint, String name,
            String account, LogController logger) {
        HttpRequest request;
        try {
            String encodedName = URLEncoder.encode(name == null ? "" : name, StandardCharsets.UTF_8);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/file?name=" + encodedName))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .header("Authorization", "Bearer " + bearerToken)
                    .timeout(Duration.ofSeconds(60));
            if (account != null && !account.isEmpty()) {
                builder.header("account", account);
            }
            request = builder.build();
        } catch (Exception exception) {
            logger.logError("Presigned URL error for name '" + name + "': " + exception.getMessage());
            return CompletableFuture.completedFuture(null);
        }

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.logError("Presigned URL error for name '" + name + "': HTTP "
                                + response.statusCode());
                        return null;
                    }
                    return new JSONObject(response.body()).getString("url");
                })
                .exceptionally(exception -> {
                    logger.logError("Presigned URL error for name '" + name + "': " + exception.getMessage());
                    return null;
                });
    }
}
