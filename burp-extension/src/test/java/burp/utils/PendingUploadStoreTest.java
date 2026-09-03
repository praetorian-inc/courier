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
import burp.controller.ConfigurationController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PendingUploadStoreTest {
    @TempDir
    Path temporaryHome;

    @Test
    void monitoredMapQueuesBatchWhenAllShutdownUploadsFail() throws Exception {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", temporaryHome.toString());
            SessionManager session = mock(SessionManager.class);
            when(session.isEnabled()).thenReturn(true);
            when(session.getChariotToken(any())).thenReturn("");
            when(session.getProjectName()).thenReturn("project");
            when(session.getTargetApplication()).thenReturn("");
            ConfigurationController configuration = mock(ConfigurationController.class);
            when(configuration.getExcludedExtensions()).thenReturn(java.util.Set.of());
            MonitoredHashMap<Integer, String> map = new MonitoredHashMap<>(
                    10, 1, TimeUnit.DAYS, session, mock(LogController.class),
                    "proxy", configuration);
            map.put(1, "value");

            map.close();

            try (var files = Files.list(PendingUploadStore.pendingDirectory())) {
                assertEquals(1, files.count());
            }
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void evictsOldestFilesWhenRetentionLimitIsExceeded() throws Exception {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", temporaryHome.toString());
            Path directory = PendingUploadStore.pendingDirectory();
            Files.createDirectories(directory);
            Path oldest = directory.resolve("0-old.pending.json");
            Files.writeString(oldest, "{}");
            SessionManager session = mock(SessionManager.class);
            when(session.getProjectName()).thenReturn("project");
            LogController logger = mock(LogController.class);

            for (int index = 0; index < PendingUploadStore.MAX_PENDING_FILES; index++) {
                PendingUploadStore.queue(Map.of("data", index), "proxy", session, logger);
            }

            assertFalse(Files.exists(oldest));
            try (var files = Files.list(directory)) {
                assertEquals(PendingUploadStore.MAX_PENDING_FILES, files.count());
            }
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void removesStructurallyInvalidFilesDuringRetry() throws Exception {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", temporaryHome.toString());
            Path directory = PendingUploadStore.pendingDirectory();
            Files.createDirectories(directory);
            Path invalid = directory.resolve("0-invalid.pending.json");
            Files.writeString(invalid, "not-json");
            SessionManager session = mock(SessionManager.class);
            when(session.isEnabled()).thenReturn(true);

            PendingUploadStore.retryAll(session, mock(LogController.class)).join();

            assertFalse(Files.exists(invalid));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void preservesFailedBatchInOwnerOnlyRetryFile() throws Exception {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", temporaryHome.toString());
            SessionManager session = mock(SessionManager.class);
            when(session.getProjectName()).thenReturn("project");

            PendingUploadStore.queue(
                    Map.of("metadata", Map.of("mapType", "proxy"),
                            "data", Map.of("1", "value")),
                    "proxy", session, mock(LogController.class));

            Path directory = PendingUploadStore.pendingDirectory();
            Path file;
            try (var files = Files.list(directory)) {
                file = files.findFirst().orElseThrow();
            }
            var envelope = new ObjectMapper().readTree(file.toFile());
            assertTrue(envelope.get("remoteName").asText().contains("-proxy.courier"));
            assertEquals("value", envelope.get("payload").get("data").get("1").asText());
            if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
                assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                        Files.getPosixFilePermissions(file));
            }
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
