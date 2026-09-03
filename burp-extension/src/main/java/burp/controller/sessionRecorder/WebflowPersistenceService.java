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

import burp.controller.LogController;
import burp.model.Webflow;
import burp.serialization.util.JsonMapperUtil;
import burp.utils.SessionManager;
import burp.utils.SecureFiles;
import burp.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public final class WebflowPersistenceService {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final LogController logger;
    private final ObjectMapper objectMapper;

    public WebflowPersistenceService(LogController logger) {
        this.logger = logger;
        this.objectMapper = JsonMapperUtil.getConfiguredMapper();
    }

    public Path save(Webflow webflow, Path recordingDirectory) throws Exception {
        SecureFiles.createPrivateDirectories(recordingDirectory);
        String name = webflow.getName() == null
                ? "recording"
                : webflow.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
        Path output = recordingDirectory.resolve(
                "webflow_" + name + "_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".courier");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(webflow);
        SecureFiles.writePrivateString(output, json, StandardCharsets.UTF_8);
        logger.logInfo("Webflow saved as JSON: " + output);
        return output;
    }

    public CompletableFuture<Boolean> upload(Webflow webflow, SessionManager sessionManager) {
        if (sessionManager == null || !sessionManager.isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> sessionManager.getChariotToken(logger))
                .thenCompose(token -> {
                    if (token == null || token.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }
                    String projectName = sessionManager.getProjectName().isEmpty()
                            ? "tempProject" : sessionManager.getProjectName();
                    String filename = "courier/" + projectName + "/" + Instant.now().toEpochMilli()
                            + "-webflow.courier";
                    return Utils.getPresignedURL(token, sessionManager.getChariotApiEndpoint(), filename,
                                    sessionManager.getSelectedAccount(), logger)
                            .thenCompose(url -> {
                                if (url == null || url.isEmpty()) {
                                    return CompletableFuture.completedFuture(false);
                                }
                                try {
                                    return Utils.uploadJsonFile(objectMapper.writeValueAsString(webflow), url,
                                            "Webflow", logger);
                                } catch (Exception exception) {
                                    logger.logError("Unable to serialize webflow: " + exception.getMessage());
                                    return CompletableFuture.completedFuture(false);
                                }
                            });
                })
                .exceptionally(exception -> {
                    logger.logError("Unable to upload webflow: " + exception.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> saveAndUpload(
            Webflow webflow, Path recordingDirectory, SessionManager sessionManager) {
        if (webflow == null) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            save(webflow, recordingDirectory);
        } catch (Exception exception) {
            logger.logError("Unable to save webflow: " + exception.getMessage());
        }
        return upload(webflow, sessionManager).thenApply(success -> {
            if (sessionManager != null && sessionManager.isEnabled()) {
                if (success) {
                    logger.logInfo("Successfully synced Webflow to Guard");
                } else {
                    logger.logError("Failed to sync Webflow to Guard");
                }
            }
            return success;
        });
    }
}
