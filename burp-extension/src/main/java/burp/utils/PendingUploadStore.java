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

import burp.controller.LogController;
import burp.serialization.util.JsonMapperUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PendingUploadStore {
    static final int MAX_PENDING_FILES = 256;
    private static final Object STORE_LOCK = new Object();

    private PendingUploadStore() {
    }

    public static void queue(Map<String, Object> payload, String mapType,
            SessionManager session, LogController logger) {
        try {
            ObjectNode envelope = JsonMapperUtil.getConfiguredMapper().createObjectNode();
            envelope.put("remoteName", remoteName(session, mapType));
            envelope.put("account", session == null ? "" : session.getSelectedAccount());
            envelope.set("payload", JsonMapperUtil.getConfiguredMapper().valueToTree(payload));
            synchronized (STORE_LOCK) {
                Path file = pendingDirectory().resolve(
                        Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".pending.json");
                SecureFiles.writePrivateString(file,
                        JsonMapperUtil.getConfiguredMapper().writeValueAsString(envelope),
                        StandardCharsets.UTF_8);
                enforceRetention(logger);
            }
            logger.logError("Queued failed " + mapType + " upload for the next connection");
        } catch (Exception exception) {
            logger.logError("Unable to preserve failed upload: " + exception.getMessage());
        }
    }

    public static CompletableFuture<Void> retryAll(SessionManager session, LogController logger) {
        if (session == null || !session.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> pendingFiles(logger))
                .thenCompose(files -> retrySequentially(files, 0, session, logger));
    }

    static Path pendingDirectory() {
        return Path.of(System.getProperty("user.home"), "BurpCourier", "pending-uploads");
    }

    private static List<Path> pendingFiles(LogController logger) {
        Path directory = pendingDirectory();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".pending.json"))
                    .sorted(Comparator.comparing(PendingUploadStore::lastModified)
                            .thenComparing(Path::toString))
                    .toList();
        } catch (Exception exception) {
            logger.logError("Unable to read pending uploads: " + exception.getMessage());
            return List.of();
        }
    }

    private static FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (Exception exception) {
            return FileTime.fromMillis(0);
        }
    }

    private static CompletableFuture<Void> retrySequentially(List<Path> files, int index,
            SessionManager session, LogController logger) {
        if (index >= files.size() || !session.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        Path file = files.get(index);
        return retry(file, session, logger)
                .thenCompose(ignored -> retrySequentially(files, index + 1, session, logger));
    }

    private static CompletableFuture<Void> retry(Path file, SessionManager session,
            LogController logger) {
        JsonNode envelope;
        try {
            envelope = JsonMapperUtil.getConfiguredMapper().readTree(file.toFile());
        } catch (JsonProcessingException exception) {
            logger.logError("Unable to parse pending upload " + file.getFileName() + ": "
                    + exception.getMessage());
            deleteInvalidFile(file, logger);
            return CompletableFuture.completedFuture(null);
        } catch (Exception exception) {
            logger.logError("Unable to read pending upload " + file.getFileName() + ": "
                    + exception.getMessage());
            return CompletableFuture.completedFuture(null);
        }
        JsonNode remoteNameNode = envelope == null ? null : envelope.path("remoteName");
        JsonNode accountNode = envelope == null ? null : envelope.path("account");
        JsonNode payload = envelope == null ? null : envelope.path("payload");
        if (envelope == null || !envelope.isObject()
                || remoteNameNode == null || !remoteNameNode.isTextual()
                || remoteNameNode.asText().isEmpty()
                || accountNode == null || !accountNode.isTextual()
                || payload == null || !payload.isObject()) {
            logger.logError("Pending upload is malformed: " + file.getFileName());
            deleteInvalidFile(file, logger);
            return CompletableFuture.completedFuture(null);
        }
        String remoteName = remoteNameNode.asText();
        String account = accountNode.asText();

        return CompletableFuture.supplyAsync(() -> session.getChariotToken(logger))
                .thenCompose(token -> token == null || token.isEmpty()
                        ? CompletableFuture.completedFuture(false)
                        : Utils.getPresignedURL(token, session.getChariotApiEndpoint(), remoteName,
                                        account, logger)
                                .thenCompose(url -> url == null || url.isEmpty()
                                        ? CompletableFuture.completedFuture(false)
                                        : Utils.uploadJsonFile(payload.toString(), url,
                                                "PendingUpload", logger)))
                .thenAccept(success -> {
                    if (!success) {
                        logger.logError("Pending upload retry failed: " + file.getFileName());
                        return;
                    }
                    try {
                        Files.deleteIfExists(file);
                        logger.logInfo("Uploaded queued Courier batch successfully");
                    } catch (Exception exception) {
                        logger.logError("Unable to remove completed pending upload: "
                                + exception.getMessage());
                    }
                });
    }

    private static void enforceRetention(LogController logger) {
        List<Path> files = pendingFiles(logger);
        int filesToDelete = Math.max(0, files.size() - MAX_PENDING_FILES);
        for (int index = 0; index < filesToDelete; index++) {
            try {
                Files.deleteIfExists(files.get(index));
            } catch (Exception exception) {
                logger.logError("Unable to evict old pending upload: " + exception.getMessage());
            }
        }
    }

    private static void deleteInvalidFile(Path file, LogController logger) {
        try {
            Files.deleteIfExists(file);
        } catch (Exception exception) {
            logger.logError("Unable to remove invalid pending upload " + file.getFileName() + ": "
                    + exception.getMessage());
        }
    }

    private static String remoteName(SessionManager session, String mapType) {
        String projectName = session == null || session.getProjectName().isEmpty()
                ? "tempProject" : session.getProjectName();
        return "courier/" + projectName + "/" + Instant.now().toEpochMilli()
                + "-" + mapType + ".courier";
    }
}
