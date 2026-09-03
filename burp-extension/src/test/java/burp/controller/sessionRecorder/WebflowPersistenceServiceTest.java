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
import burp.utils.SessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebflowPersistenceServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesSanitizedWebflowAndSkipsUploadWithoutActiveSession() throws Exception {
        LogController logger = mock(LogController.class);
        WebflowPersistenceService service = new WebflowPersistenceService(logger);
        Webflow webflow = new Webflow("Login / test", "description", "project", "https://example.test");

        Path output = service.save(webflow, temporaryDirectory);

        assertTrue(Files.exists(output));
        assertTrue(output.getFileName().toString().startsWith("webflow_Login___test_"));
        assertTrue(Files.readString(output).contains("\"name\" : \"Login / test\""));
        if (Files.getFileStore(output).supportsFileAttributeView("posix")) {
            assertEquals(java.util.Set.of(
                            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE),
                    Files.getPosixFilePermissions(output));
        }
        assertFalse(service.upload(webflow, null).join());
        assertFalse(service.saveAndUpload(null, temporaryDirectory, null).join());

        SessionManager disabled = mock(SessionManager.class);
        when(disabled.isEnabled()).thenReturn(false);
        assertFalse(service.upload(webflow, disabled).join());
    }
}
