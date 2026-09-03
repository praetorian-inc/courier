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

package burp.controller;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.view.LoggerView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogControllerTest {
    @TempDir
    Path temporaryHome;

    @Test
    void batchesUiLoggingAndClosesItsOwnedFile() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", temporaryHome.toString());
        try {
            MontoyaApi api = mock(MontoyaApi.class);
            Logging logging = mock(Logging.class);
            when(api.logging()).thenReturn(logging);
            LoggerView view = new LoggerView(api, "test-build");
            view.createLoggerPanel();
            LogController controller = view.getLogController();

            controller.logInfo("first");
            controller.logInfo("second");
            SwingUtilities.invokeAndWait(() -> { });
            SwingUtilities.invokeAndWait(() -> { });

            assertTrue(view.getLogArea().getText().contains("first"));
            assertTrue(view.getLogArea().getText().contains("second"));
            verify(logging, times(2)).logToOutput(anyString());
            controller.close();
            try (var files = Files.walk(temporaryHome)) {
                Path logFile = files
                        .filter(path -> path.getFileName().toString().endsWith(".log"))
                        .findFirst()
                        .orElseThrow();
                assertTrue(Files.readString(logFile).contains("second"));
                if (Files.getFileStore(logFile).supportsFileAttributeView("posix")) {
                    assertEquals(java.util.Set.of(
                                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE),
                            Files.getPosixFilePermissions(logFile));
                }
            }
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
