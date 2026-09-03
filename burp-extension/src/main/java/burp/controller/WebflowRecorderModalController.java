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

import burp.controller.sessionRecorder.PlaywrightRecorder;
import burp.model.Webflow;
import java.net.URI;
import java.util.function.Consumer;
import burp.utils.SessionManager;

public class WebflowRecorderModalController {
    private Consumer<Webflow> onWebflowCreated;
    private Consumer<Webflow.WebflowStep> onStepRecorded;
    private volatile PlaywrightRecorder currentRecorder;
    private LogController logger;
    private final SessionManager sessionManager;

    public WebflowRecorderModalController(Consumer<Webflow> onWebflowCreated, Consumer<Webflow.WebflowStep> onStepRecorded, LogController logger, SessionManager sessionManager) {
        this.onWebflowCreated = onWebflowCreated;
        this.onStepRecorded = onStepRecorded;
        this.logger = logger;
        this.sessionManager = sessionManager;
    }
    
    
    
    /**
     * Validates if a URL is properly formatted
     * @param url The URL to validate
     * @return ValidationResult containing success status and message
     */
    public ValidationResult validateStartUrl(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return new ValidationResult(false, "Start URL cannot be empty");
            }
            
            String trimmedUrl = url.trim();
            
            // Check if URL starts with http:// or https://
            if (!trimmedUrl.toLowerCase().startsWith("http://") && 
                !trimmedUrl.toLowerCase().startsWith("https://")) {
                return new ValidationResult(false, "URL must start with http:// or https://");
            }
            
            // Try to create a URI object to validate format
            new URI(trimmedUrl);
            
            return new ValidationResult(true, "URL format is valid");
            
        } catch (Exception e) {
            return new ValidationResult(false, "Invalid URL format: " + e.getMessage());
        }
    }
    
    /**
     * Validates webflow creation inputs
     * @param name Webflow name
     * @param description Webflow description
     * @param startUrl Start URL for the webflow
     * @return ValidationResult containing success status and message
     */
    public ValidationResult validateWebflowInputs(String name, String description, String startUrl) {
        if (name == null || name.trim().isEmpty()) {
            return new ValidationResult(false, "Webflow name is required");
        }
        
        if (name.trim().length() < 3) {
            return new ValidationResult(false, "Webflow name must be at least 3 characters long");
        }
        
        if (description == null || description.trim().isEmpty()) {
            return new ValidationResult(false, "Webflow description is required");
        }
        
        ValidationResult urlValidation = validateStartUrl(startUrl);
        if (!urlValidation.isSuccess()) {
            return urlValidation;
        }
        
        return new ValidationResult(true, "All inputs are valid");
    }
    
    /**
     * Creates a new webflow with the provided inputs and starts recording
     * @param name Webflow name
     * @param description Webflow description
     * @param projectName Project name (optional)
     * @param startUrl Start URL for the webflow
     */
    public void createWebflow(String name, String description, String projectName, String startUrl) {
        ValidationResult validation = validateWebflowInputs(name, description, startUrl);
        if (!validation.isSuccess()) {
            throw new IllegalArgumentException(validation.getMessage());
        }
        
        Webflow webflow = new Webflow(name.trim(), description.trim(), 
                                     projectName != null ? projectName.trim() : "Default Project", 
                                     startUrl.trim());
        
        // Generate a simple ID
        webflow.setId(System.currentTimeMillis() + "_" + name.replaceAll("[^a-zA-Z0-9]", "_"));
        
        // Start Playwright recording session
        try {
            startRecordingSession(webflow);
        } catch (Exception e) {
            throw new RuntimeException(rootMessage(e), e);
        }
        
        if (onWebflowCreated != null) {
            onWebflowCreated.accept(webflow);
        }
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

    /**
     * Starts a Playwright recording session for the given webflow
     * @param webflow The webflow to record
     * @throws Exception if recording fails to start
     */
    public synchronized void startRecordingSession(Webflow webflow) throws Exception {
        stopRecordingSession();

        PlaywrightRecorder recorder = new PlaywrightRecorder(
                webflow, onStepRecorded, logger, sessionManager);
        currentRecorder = recorder;
        try {
            recorder.startRecording();
        } catch (Exception exception) {
            recorder.stopRecordingAndWait();
            if (currentRecorder == recorder) {
                currentRecorder = null;
            }
            throw exception;
        }
    }
    
    /**
     * Stops the current recording session
     */
    public synchronized void stopRecordingSession() {
        PlaywrightRecorder recorder = currentRecorder;
        if (recorder == null) {
            return;
        }
        recorder.stopRecordingAndWait();
        if (currentRecorder == recorder) {
            currentRecorder = null;
        }
    }
    
    /**
     * Checks if a recording session is currently active
     * @return true if recording is active, false otherwise
     */
    public synchronized boolean isRecording() {
        return currentRecorder != null && currentRecorder.isRecording();
    }
    
    /**
     * Result class for validation operations
     */
    public static class ValidationResult {
        private final boolean success;
        private final String message;
        
        public ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
