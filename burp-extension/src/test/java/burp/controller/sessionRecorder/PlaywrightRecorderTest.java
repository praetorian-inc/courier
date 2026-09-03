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

import burp.model.NetworkInterface;
import burp.controller.LogController;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlaywrightRecorderTest {
    @TempDir
    Path temporaryHome;
    @Test
    void keepsRecordingDirectoryInsideConfiguredRoot() {
        Path directory = PlaywrightRecorder.recordingsDirectoryFor("../../outside");
        Path root = Path.of("recordings").toAbsolutePath().normalize();

        assertTrue(directory.startsWith(root));
        assertEquals("______outside", directory.getFileName().toString());
    }

    @Test
    void passesDiscoveredBrowserCacheThroughPlaywrightEnvironment() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                System.getenv("PLAYWRIGHT_BROWSERS_PATH") == null);
        String originalHome = System.getProperty("user.home");
        Path cache = temporaryHome.resolve(".cache/ms-playwright");
        java.nio.file.Files.createDirectories(cache);
        try {
            System.setProperty("user.home", temporaryHome.toString());
            Map<String, String> environment = Utils.findPlaywrightEnvironment(
                    org.mockito.Mockito.mock(LogController.class));
            assertEquals(cache.toString(), environment.get("PLAYWRIGHT_BROWSERS_PATH"));
            assertNull(System.getProperty("PLAYWRIGHT_BROWSERS_PATH"));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void debugInfoOmitsLocalPathsAndEnvironmentValues() {
        String debugInfo = Utils.getDebugInfo(mock(LogController.class), this);

        assertFalse(debugInfo.contains("Working directory:"));
        assertFalse(debugInfo.contains("User home:"));
        assertFalse(debugInfo.contains("PLAYWRIGHT_BROWSERS_PATH: "
                + System.getenv("PLAYWRIGHT_BROWSERS_PATH")));
    }

    @Test
    void acceptsBurpsMitmCertificateInsideRecordedBrowserContext() {
        Browser.NewContextOptions options =
                PlaywrightRecorder.browserContextOptions("http://127.0.0.1:8080");

        assertEquals(Boolean.TRUE, options.ignoreHTTPSErrors);
        assertEquals("http://127.0.0.1:8080", options.proxy.server);
    }

    @Test
    void fallsBackToInstalledChromeWhenManagedChromiumIsUnavailable() {
        BrowserType chromium = mock(BrowserType.class);
        Browser installedChrome = mock(Browser.class);
        when(chromium.launch(any(BrowserType.LaunchOptions.class)))
                .thenThrow(new RuntimeException("managed browser missing"))
                .thenReturn(installedChrome);

        Browser launched = PlaywrightRecorder.launchChromium(
                chromium, mock(LogController.class));

        assertSame(installedChrome, launched);
        var options = org.mockito.ArgumentCaptor.forClass(BrowserType.LaunchOptions.class);
        verify(chromium, times(2)).launch(options.capture());
        assertEquals("chrome", options.getAllValues().get(1).channel);
    }

    @Test
    void installsManagedChromiumOnFirstUseWhenNoBrowserExists() {
        BrowserType chromium = mock(BrowserType.class);
        Browser installed = mock(Browser.class);
        when(chromium.launch(any(BrowserType.LaunchOptions.class)))
                .thenThrow(new RuntimeException("Executable doesn't exist"))
                .thenThrow(new RuntimeException("Chrome is not installed"))
                .thenReturn(installed);
        java.util.concurrent.atomic.AtomicBoolean installerCalled =
                new java.util.concurrent.atomic.AtomicBoolean();

        Browser launched = PlaywrightRecorder.launchChromium(
                chromium, mock(LogController.class), () -> installerCalled.set(true));

        assertSame(installed, launched);
        assertTrue(installerCalled.get());
        verify(chromium, times(3)).launch(any(BrowserType.LaunchOptions.class));
    }

    @Test
    void reportsActionableBrowserInstallationFailure() {
        BrowserType chromium = mock(BrowserType.class);
        when(chromium.launch(any(BrowserType.LaunchOptions.class)))
                .thenThrow(new RuntimeException("Executable does not exist"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> PlaywrightRecorder.launchChromium(
                        chromium, mock(LogController.class),
                        () -> { throw new Exception("test installer disabled"); }));

        assertTrue(failure.getMessage().contains(
                "npx playwright@1.54.0 install chromium"));
    }

    @Test
    void requiresAnActiveBurpProxyListener() {
        assertThrows(IllegalStateException.class,
                () -> PlaywrightRecorder.activeProxyServer(Map.of()));
        assertEquals("http://127.0.0.1:8080", PlaywrightRecorder.activeProxyServer(
                Map.of(NetworkInterface.LOOPBACK, "http://127.0.0.1:8080")));
    }
}
