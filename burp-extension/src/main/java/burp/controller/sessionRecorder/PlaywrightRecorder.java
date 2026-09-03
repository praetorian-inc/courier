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

import com.microsoft.playwright.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import burp.model.Webflow;
import burp.model.WebflowActionMapper;
import java.nio.file.Paths;
import java.util.UUID;
import org.json.JSONObject;
import org.json.JSONException;
import burp.controller.LogController;
import burp.utils.SafeLogFormatter;
import burp.utils.SessionManager;
import java.util.Map;
import java.io.InputStream;
import burp.model.NetworkInterface;

public class PlaywrightRecorder {
    private static final long STARTUP_TIMEOUT_MINUTES = 5;
    private final AtomicInteger stepCounter = new AtomicInteger(0);
    private final AtomicBoolean isCleaningUp = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final CountDownLatch terminationLatch = new CountDownLatch(1);
    private final CompletableFuture<Void> startupSignal = new CompletableFuture<>();
    private final AtomicBoolean recordingStarted = new AtomicBoolean();
    private volatile ExecutorService recordingExecutor;
    private Thread shutdownHook;
    private Playwright playwright;
    private Browser browser;
    private Page page;
    private BrowserContext context;
    private Webflow webflow;
    private Consumer<Webflow.WebflowStep> onStepRecorded;
    private volatile boolean isRecording;
    private final String uniqueTrackingId;
    private final java.nio.file.Path recordingsDirectory;
    private final WebflowPersistenceService persistenceService;
    private LogController logger;
    private final SessionManager sessionManager;

    public PlaywrightRecorder(Webflow webflow, Consumer<Webflow.WebflowStep> onStepRecorded, LogController logger,
            SessionManager sessionManager) {
        this.webflow = webflow;
        this.onStepRecorded = onStepRecorded;
        // Generate a unique ID to prevent websites from injecting fake webflow actions
        this.uniqueTrackingId = "WEBFLOW_RECORDED_ACTION_" + UUID.randomUUID().toString().replace("-", "");
        this.recordingsDirectory = recordingsDirectoryFor(webflow.getProjectName());
        this.logger = logger;
        this.sessionManager = sessionManager;
        this.persistenceService = new WebflowPersistenceService(logger);
    }

    public void startRecording() throws Exception {
        if (isRecording) {
            throw new IllegalStateException("Recording is already in progress");
        }

        if (sessionManager == null || !sessionManager.isEnabled()) {
            throw new IllegalStateException("Connect to Guard before starting a webflow recording");
        }
        isRecording = true;
        recordingExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Courier-Playwright-Recorder");
            thread.setDaemon(true);
            return thread;
        });
        logger.logInfo("Starting Playwright recording session in background thread...");

        recordingExecutor.submit(this::runRecordingSession);
        try {
            startupSignal.get(STARTUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            logger.logInfo("Recording session started successfully");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            requestStop();
            awaitTermination(5, TimeUnit.SECONDS);
            throw new Exception("Recording startup was interrupted", exception);
        } catch (TimeoutException exception) {
            requestStop();
            awaitTermination(5, TimeUnit.SECONDS);
            throw new Exception("Timed out while starting Playwright", exception);
        } catch (ExecutionException exception) {
            requestStop();
            awaitTermination(5, TimeUnit.SECONDS);
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new Exception(rootMessage(cause), cause);
        }
    }


    private void runRecordingSession() {
        try {
            playwright = PlaywrightEnvironment.create(logger, this);
            if (stopRequested.get()) {
                throw new IllegalStateException("Recording was cancelled during startup");
            }
            beginSession();
        } catch (Exception exception) {
            startupSignal.completeExceptionally(exception);
            if (!stopRequested.get()) {
                logger.logError("Recording session failed: " + exception.getMessage());
            }
        } finally {
            cleanupOnRecordingThread();
        }
    }

    private void beginSession() throws Exception {
            String proxyServer = activeProxyServer(sessionManager.getProxyServerUrls());
            browser = launchChromium(playwright.chromium(), logger,
                    () -> PlaywrightEnvironment.installChromium(this, logger));

            Browser.NewContextOptions contextOptions = browserContextOptions(proxyServer);

            // Set up video recording if directory exists
            try {
                burp.utils.SecureFiles.createPrivateDirectories(recordingsDirectory);
                contextOptions.setRecordVideoDir(recordingsDirectory);
            } catch (Exception e) {
                // Continue without video recording if setup fails
                logger.logError("Warning: Could not set up video recording: " + e.getMessage());
            }

            context = browser.newContext(contextOptions);

            // Create new page
            page = context.newPage();

            // Set up comprehensive event tracking
            setupEventTracking();

            // Set up network event handlers
            setupNetworkTracking();

            // Set up browser and page close handlers
            setupCloseHandlers();

            // Navigate to initial URL
            page.navigate(webflow.getStartUrl());

            // Record the initial navigation step
            recordInitialNavigationStep(webflow.getStartUrl());
            recordingStarted.set(true);
            startupSignal.complete(null);

            logger.logInfo("Webflow recording started. Interact with the browser...");
            logger.logInfo("Close the browser window to stop code generation and cleanup.");

            waitForRecordingEnd();
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void stopRecording() {
        logger.logInfo("Stopping recording session...");
        requestStop();
    }

    public void stopRecordingAndWait() {
        stopRecording();
        if (recordingExecutor != null) {
            awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Records the initial navigation step when the browser navigates to the start
     * URL
     * 
     * @param url The URL being navigated to
     */
    private void recordInitialNavigationStep(String url) {
        if (!isRecording) {
            return;
        }

        try {
            // Get next step order
            int stepOrder = stepCounter.incrementAndGet();

            // Create the initial navigation step
            String description = String.format("Navigate to %s", url);
            Webflow.WebflowStep step = new Webflow.WebflowStep(
                    Webflow.WebflowStep.StepType.NAVIGATION,
                    "", // No selector for navigation
                    url, // URL as the value
                    description,
                    stepOrder);
            step.setTimestamp(System.currentTimeMillis());
            step.setUrl(url);

            // Add step to the webflow
            webflow.addStep(step);

            // Trigger callback if provided
            if (onStepRecorded != null) {
                onStepRecorded.accept(step);
            }

            logger.logDebug("Recorded navigation step " + stepOrder + " to "
                    + SafeLogFormatter.origin(url));

        } catch (Exception e) {
            logger.logError("Error recording initial navigation step: " + e.getMessage());
        }
    }

    private void setupCloseHandlers() {
        // Handle browser close event (may not always trigger on manual close)
        browser.onDisconnected(b -> {
            logger.logDebug("Browser disconnected event triggered");
            requestStop();
        });

        page.onClose(p -> {
            logger.logDebug("Page closed event triggered");
            requestStop();
        });

        shutdownHook = new Thread(() -> {
            requestStop();
            awaitTermination(30, TimeUnit.SECONDS);
        }, "Courier-Playwright-Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void waitForRecordingEnd() {
        while (!stopRequested.get()) {
            try {
                if (!browser.isConnected() || page.isClosed()) {
                    break;
                }
                page.waitForTimeout(250);
            } catch (Exception exception) {
                if (!stopRequested.get()) {
                    logger.logDebug("Browser is no longer accessible");
                }
                break;
            }
        }
    }

    private void cleanupOnRecordingThread() {
        if (!isCleaningUp.compareAndSet(false, true)) {
            return;
        }

        isRecording = false;
        startupSignal.completeExceptionally(new IllegalStateException("Recording stopped during startup"));

        logger.logInfo("Starting cleanup process...");

        if (recordingStarted.get()) {
            persistenceService.saveAndUpload(webflow, recordingsDirectory, sessionManager);
        }

        try {
            // Close page if still open
            if (page != null && !page.isClosed()) {
                page.close();
            }
        } catch (Exception e) {
            logger.logError("Error closing page: " + e.getMessage());
        }

        try {
            // Close browser if still open
            if (browser != null && browser.isConnected()) {
                browser.close();
            }
        } catch (Exception e) {
            logger.logError("Error closing browser: " + e.getMessage());
        }

        try {
            // Close playwright
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception e) {
            logger.logError("Error closing playwright: " + e.getMessage());
        }

        if (recordingExecutor != null) {
            recordingExecutor.shutdown();
        }
        removeShutdownHook();
        terminationLatch.countDown();
        logger.logInfo("Code generation completed and resources cleaned up!");
    }

    private void setupEventTracking() {// Page page) {
        // Set up console message handler to capture our tracking events
        page.onConsoleMessage(msg -> {
            String text = msg.text();
            String expectedPrefix = uniqueTrackingId + ":";
            if (text.startsWith(expectedPrefix)) {
                try {
                    // Parse the action and generate corresponding code
                    String jsonAction = text.substring(expectedPrefix.length());

                    // Create WebflowStep from the action and add to Webflow
                    Webflow.WebflowStep step = createWebflowStepFromAction(jsonAction);
                    if (step != null) {
                        // Add step to the webflow
                        webflow.addStep(step);

                        // Trigger callback if provided
                        if (onStepRecorded != null) {
                            onStepRecorded.accept(step);
                        }

                        logger.logDebug("Recorded " + step.getAction() + " step " + step.getOrder());
                    }
                } catch (Exception e) {
                    logger.logError("Error processing action: " + e.getMessage());
                }
            }
        });
        // Inject comprehensive tracking script
        try (InputStream in = getClass().getResourceAsStream("/initScript.js")) {
            if (in != null) {
                String scriptContents = new String(in.readAllBytes());
                page.addInitScript(scriptContents.replace("{UNIQUETRACKINGID}", uniqueTrackingId));
            }
        } catch (Exception e) {
            logger.logError("Error setting up event tracking: " + e.getMessage());
        }
    }

    private void setupNetworkTracking() {
        try {
            // Track HTTP requests
            page.onRequestFinished(request -> {
                try {
                    logger.logDebug("Captured HTTP request: " + request.method() + " "
                            + SafeLogFormatter.origin(request.url()));

                    // Try to correlate with recent webflow steps
                    burp.controller.sessionRecorder.Utils.correlateRequestResponseWithSteps(request, logger, webflow);

                } catch (Exception e) {
                    logger.logError("Error handling request event: " + e.getMessage());
                }
            });
            page.onRequestFailed(request -> {
                try {
                    logger.logDebug("Captured failed HTTP request: " + request.method() + " "
                            + SafeLogFormatter.origin(request.url()));
                    Utils.correlateRequestResponseWithSteps(request, logger, webflow);
                } catch (Exception exception) {
                    logger.logError("Error handling failed request event: " + exception.getMessage());
                }
            });

        } catch (Exception e) {
            logger.logError("Error setting up network tracking: " + e.getMessage());
        }
    }

    private Webflow.WebflowStep createWebflowStepFromAction(String jsonAction) {
        try {
            JSONObject action = new JSONObject(jsonAction);

            String type = action.optString("type", "");
            String selector = action.optString("selector", "");
            String value = action.optString("value", action.optString("key", ""));
            String text = action.optString("text", "");
            long timestamp = action.optLong("timestamp", System.currentTimeMillis());

            // Get current URL from the page if possible
            String currentUrl = "";
            try {
                if (page != null && !page.isClosed()) {
                    currentUrl = page.url();
                }
            } catch (Exception e) {
                // Ignore URL retrieval errors
            }

            // Determine step type and create appropriate WebflowStep
            Webflow.WebflowStep.StepType stepType = WebflowActionMapper.toStepType(type);

            // Get next step order
            int stepOrder = stepCounter.incrementAndGet();

            // Create description based on action type
            String description = WebflowActionMapper.describe(type, selector, value, text);

            // Create the WebflowStep
            Webflow.WebflowStep step = new Webflow.WebflowStep(stepType, selector, value, description, stepOrder);
            step.setTimestamp(timestamp);
            step.setUrl(currentUrl);
            step.setElementText(text);

            return step;

        } catch (JSONException e) {
            logger.logError("Error parsing JSON action: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.logError("Error creating WebflowStep from action: " + e.getMessage());
            return null;
        }
    }

    static Browser.NewContextOptions browserContextOptions(String proxyServer) {
        return new Browser.NewContextOptions()
                .setProxy(proxyServer)
                .setIgnoreHTTPSErrors(true);
    }

    static Browser launchChromium(BrowserType chromium, LogController logger) {
        return launchChromium(chromium, logger, () -> { });
    }

    static Browser launchChromium(BrowserType chromium, LogController logger,
            BrowserInstaller installer) {
        Exception managedBrowserFailure;
        try {
            return chromium.launch(new BrowserType.LaunchOptions().setHeadless(false));
        } catch (Exception exception) {
            managedBrowserFailure = exception;
        }

        logger.logDebug("Playwright-managed Chromium unavailable; trying installed Chrome");
        Exception installedChromeFailure;
        try {
            return chromium.launch(new BrowserType.LaunchOptions()
                    .setHeadless(false)
                    .setChannel("chrome"));
        } catch (Exception exception) {
            installedChromeFailure = exception;
        }

        if (browserIsMissing(managedBrowserFailure)) {
            try {
                logger.logInfo("Chromium is not installed; downloading the Playwright browser");
                installer.install();
                return chromium.launch(new BrowserType.LaunchOptions().setHeadless(false));
            } catch (Exception installationFailure) {
                if (installationFailure != installedChromeFailure) {
                    installedChromeFailure.addSuppressed(installationFailure);
                }
            }
        }

        throw new IllegalStateException(
                "Unable to launch Chromium. Install a Playwright-compatible browser with "
                        + "'npx playwright@1.54.0 install chromium', or install Google Chrome. "
                        + "Playwright reported: " + rootMessage(managedBrowserFailure),
                installedChromeFailure);
    }

    private static boolean browserIsMissing(Throwable failure) {
        String message = rootMessage(failure).toLowerCase(java.util.Locale.ROOT);
        return message.contains("executable doesn't exist")
                || message.contains("executable does not exist")
                || message.contains("browser was not found");
    }

    @FunctionalInterface
    interface BrowserInstaller {
        void install() throws Exception;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause.getMessage() == null || cause.getMessage().isBlank())
                && cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName() : message;
    }

    static java.nio.file.Path recordingsDirectoryFor(String projectName) {
        String safeName = projectName == null ? "" : projectName.trim()
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeName.isBlank()) {
            safeName = "Default_Project";
        }
        java.nio.file.Path base = Paths.get("recordings").toAbsolutePath().normalize();
        java.nio.file.Path resolved = base.resolve(safeName).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Invalid webflow project name");
        }
        return resolved;
    }

    static String activeProxyServer(Map<NetworkInterface, String> proxyServers) {
        if (proxyServers != null) {
            for (NetworkInterface networkInterface : new NetworkInterface[] {
                    NetworkInterface.LOOPBACK,
                    NetworkInterface.ALL_INTERFACES,
                    NetworkInterface.SPECIFIC_ADDRESS}) {
                String server = proxyServers.get(networkInterface);
                if (server != null && !server.isBlank()) {
                    return server;
                }
            }
        }
        throw new IllegalStateException(
                "No active Burp Proxy listener found; recording was not started");
    }

    private void requestStop() {
        stopRequested.set(true);
    }

    private void awaitTermination(long timeout, TimeUnit unit) {
        try {
            if (!terminationLatch.await(timeout, unit)) {
                logger.logError("Timed out waiting for Playwright recording to stop");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void removeShutdownHook() {
        Thread hook = shutdownHook;
        shutdownHook = null;
        if (hook == null || Thread.currentThread() == hook) {
            return;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // JVM shutdown is already in progress.
        }
    }
}
