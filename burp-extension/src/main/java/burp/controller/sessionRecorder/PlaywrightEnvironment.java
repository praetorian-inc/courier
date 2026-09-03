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
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.impl.driver.Driver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class PlaywrightEnvironment {
    private static final long INSTALL_TIMEOUT_MINUTES = 10;
    private static final long PROGRESS_INTERVAL_SECONDS = 15;

    private PlaywrightEnvironment() {
    }

    static Playwright create(LogController logger, Object owner) throws Exception {
        java.util.Map<String, String> environment = Utils.findPlaywrightEnvironment(logger);
        logger.logDebug(Utils.getDebugInfo(logger, owner));

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader playwrightClassLoader = owner.getClass().getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(playwrightClassLoader);
            return Playwright.create(new Playwright.CreateOptions().setEnv(environment));
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    static void installChromium(Object owner, LogController logger) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Process process = null;
        try {
            Thread.currentThread().setContextClassLoader(owner.getClass().getClassLoader());
            Map<String, String> environment = Utils.findPlaywrightEnvironment(logger);
            ProcessBuilder processBuilder = Driver.ensureDriverInstalled(environment, false)
                    .createProcessBuilder();
            processBuilder.command().addAll(List.of("install", "chromium"));
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            logger.logInfo("Installing Playwright Chromium in a separate process");
            process = processBuilder.start();
            long deadline = System.nanoTime()
                    + TimeUnit.MINUTES.toNanos(INSTALL_TIMEOUT_MINUTES);
            while (process.isAlive()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    process.destroyForcibly();
                    throw new TimeoutException("Timed out installing Playwright Chromium");
                }
                long waitNanos = Math.min(remaining,
                        TimeUnit.SECONDS.toNanos(PROGRESS_INTERVAL_SECONDS));
                if (!process.waitFor(waitNanos, TimeUnit.NANOSECONDS)) {
                    logger.logInfo("Playwright Chromium installation is still in progress");
                }
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "Playwright Chromium installation failed with exit code " + process.exitValue());
            }
            logger.logInfo("Playwright Chromium installation completed");
        } catch (InterruptedException exception) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw exception;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
