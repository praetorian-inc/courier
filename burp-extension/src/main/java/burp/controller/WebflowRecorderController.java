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

import burp.model.Webflow;
import burp.view.WebflowRecorderModalView;
import burp.view.WebflowRecorderView;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import burp.utils.SessionManager;


public class WebflowRecorderController implements AutoCloseable {
    private final DefaultListModel<Webflow> listModel;
    private final DefaultListModel<Webflow.WebflowStep> stepsModel;
    private final WebflowJsonFormatter jsonFormatter = new WebflowJsonFormatter();
    private final AtomicLong jsonRenderGeneration = new AtomicLong();
    private final AtomicReference<Thread> jsonWorker = new AtomicReference<>();
    private WebflowRecorderModalView modal;
    private Consumer<String> statusUpdateCallback;
    private Consumer<Webflow> webflowSelectionCallback;
    private final WebflowRecorderView webflowRecorderView;
    private String currentViewMode = "Pretty";
    private Webflow currentSelectedWebflow;
    private final LogController logger;
    private volatile SessionManager sessionManager;

    public WebflowRecorderController(WebflowRecorderView webflowRecorderView, LogController logger) {
        this.webflowRecorderView = webflowRecorderView;
        this.listModel = new DefaultListModel<>();
        this.stepsModel = new DefaultListModel<>();
        this.logger = logger;
    }
    
    /**
     * Sets the SessionManager for this controller
     * @param sessionManager The SessionManager instance
     */
    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    public void setStatusUpdateCallback(Consumer<String> callback) {
        this.statusUpdateCallback = callback;
    }
    
    public void setWebflowSelectionCallback(Consumer<Webflow> callback) {
        this.webflowSelectionCallback = callback;
    }
    
    public DefaultListModel<Webflow> getWebflowListModel() {
        return listModel;
    }
    
    public DefaultListModel<Webflow.WebflowStep> getStepsModel() {
        return stepsModel;
    }
    
    public List<Webflow> getWebflows() {
        List<Webflow> webflows = new ArrayList<>();
        for (int index = 0; index < listModel.size(); index++) {
            webflows.add(listModel.get(index));
        }
        return List.copyOf(webflows);
    }
    
    /**
     * Handles webflow selection and updates the content display
     */
    private WebflowDisplayData displayWebflow(Webflow selectedWebflow) {
        stepsModel.clear();
        if (selectedWebflow == null) {
            return webflowDisplayData(null);
        }
        stepsModel.addAll(selectedWebflow.getSteps());
        return webflowDisplayData(selectedWebflow);
    }

    private WebflowDisplayData webflowDisplayData(Webflow webflow) {
        if (webflow == null) {
            return new WebflowDisplayData("Select a webflow to view details", "", "", "", "", "");
        }
        return new WebflowDisplayData(
                webflow.getName(),
                "Description: " + webflow.getDescription(),
                "Project: " + webflow.getProjectName(),
                "Browser: Playwright Default (Chromium)",
                "Created: " + webflow.getCreatedAt(),
                "Modified: " + webflow.getLastModified());
    }
    
    /**
     * Shows the record modal for creating new webflows
     */
    public void showRecordModal(Component parentComponent) {
        if (modal != null) {
            modal.dispose();
        }
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(parentComponent);
        modal = new WebflowRecorderModalView(parentFrame, this::onWebflowCreated,
                this::onStepRecorded, this::onRecordingCompleted, logger, sessionManager);
        modal.showModal();
    }
    
    /**
     * Callback for when a new webflow is created
     */
    private void onWebflowCreated(Webflow webflow) {
        SwingUtilities.invokeLater(() -> {
            listModel.addElement(webflow);
            if (webflowSelectionCallback != null) {
                webflowSelectionCallback.accept(webflow);
            }
            updateWebflowDisplay(displayWebflow(webflow));
            updateStatus("Created webflow: " + webflow.getName());
            scheduleStatusReset();
        });
    }
    
    /**
     * Callback for when a step is recorded during webflow recording
     */
    private void onStepRecorded(Webflow.WebflowStep step) {
        // Update the steps display in real-time if the current webflow is being displayed
        SwingUtilities.invokeLater(() -> {
            // Check if the current displayed webflow is the one being recorded
            Webflow latestWebflow = getLatestWebflow();
            if (latestWebflow != null) {
                // Refresh the steps model to show the new step
                stepsModel.clear();
                for (Webflow.WebflowStep webflowStep : latestWebflow.getSteps()) {
                    stepsModel.addElement(webflowStep);
                }
                
                // Update status to show recording progress
                updateStatus("Recording step " + latestWebflow.getSteps().size() + ": " + step.getDescription());
            }
        });
    }
    
    void onRecordingCompleted() {
        updateStatus("Recording completed");
    }

    /**
     * Gets the most recently created webflow
     */
    public Webflow getLatestWebflow() {
        return listModel.isEmpty() ? null : listModel.get(listModel.size() - 1);
    }
    
    /**
     * Updates the status message
     */
    private void updateStatus(String message) {
        if (statusUpdateCallback != null) {
            statusUpdateCallback.accept(message);
        }
    }

    private void scheduleStatusReset() {
        Timer timer = new Timer(3000, event -> updateStatus("Ready to record webflows"));
        timer.setRepeats(false);
        timer.start();
    }
    
    /**
     * Clears all webflows from the list
     */
    public void clearAllWebflows() {
        cancelJsonRendering();
        webflowRecorderView.showPrettySteps();
        currentSelectedWebflow = null;
        listModel.clear();
        stepsModel.clear();
        updateStatus("All webflows cleared");
        scheduleStatusReset();
    }
    
    /**
     * Removes selected webflows from the list
     * @param selectedWebflows List of webflows to remove
     */
    public void removeWebflows(List<Webflow> selectedWebflows) {
        if (selectedWebflows == null || selectedWebflows.isEmpty()) {
            return;
        }
        cancelJsonRendering();
        webflowRecorderView.showPrettySteps();
        
        for (Webflow webflow : selectedWebflows) {
            listModel.removeElement(webflow);
        }
        
        // Clear steps display if current selection was removed
        stepsModel.clear();
        
        String message = selectedWebflows.size() == 1 ? 
            "Removed webflow: " + selectedWebflows.get(0).getName() :
            "Removed " + selectedWebflows.size() + " webflows";
        updateStatus(message);
        scheduleStatusReset();
    }
    
    /**
     * Data class for webflow display information
     */
    public static class WebflowDisplayData {
        private final String name;
        private final String description;
        private final String projectName;
        private final String browserPath;
        private final String createdAt;
        private final String lastModified;
        
        public WebflowDisplayData(String name, String description, String projectName, 
                                 String browserPath, String createdAt, String lastModified) {
            this.name = name;
            this.description = description;
            this.projectName = projectName;
            this.browserPath = browserPath;
            this.createdAt = createdAt;
            this.lastModified = lastModified;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getProjectName() { return projectName; }
        public String getBrowserPath() { return browserPath; }
        public String getCreatedAt() { return createdAt; }
        public String getLastModified() { return lastModified; }
    }


    /**
     * Clears the webflow display (used for multi-selection)
     */
    public void clearWebflowDisplay() {
        cancelJsonRendering();
        webflowRecorderView.showPrettySteps();
        webflowRecorderView.showMultipleSelection();
        stepsModel.clear();
    }
    
    /**
     * Handles clear all webflows button click with confirmation
     */
    public void handleClearAllWebflows() {
        if (listModel.isEmpty()) {
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(
            null,
            "Are you sure you want to clear all webflows?",
            "Confirm Clear All",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            clearAllWebflows();
        }
    }
    
    /**
     * Handles remove selected webflows with confirmation
     * @param selectedWebflows List of webflows to remove
     */
    public void handleRemoveSelectedWebflows(List<Webflow> selectedWebflows) {
        if (selectedWebflows == null || selectedWebflows.isEmpty()) {
            return;
        }
        
        String message = selectedWebflows.size() == 1 ?
            "Are you sure you want to remove the selected webflow?" :
            "Are you sure you want to remove " + selectedWebflows.size() + " selected webflows?";
        
        int result = JOptionPane.showConfirmDialog(
            null,
            message,
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            removeWebflows(selectedWebflows);
        }
    }
    
    /**
     * Handles webflow list selection changes
     * @param selectedIndices Array of selected indices
     */
    public void handleWebflowSelectionChange(int[] selectedIndices) {
        if (selectedIndices.length == 1) {
            // Single selection - display webflow details
            Webflow selectedWebflow = listModel.getElementAt(selectedIndices[0]);
            currentSelectedWebflow = selectedWebflow;
            
            // Update view mode buttons and refresh display with current view mode
            updateViewModeButtons(currentViewMode);
            refreshWebflowDisplay(selectedWebflow);
        } else {
            // Multi-selection or no selection - clear details
            currentSelectedWebflow = null;
            clearWebflowDisplay();
        }
    }
    
    /**
     * Updates the webflow display with the given data
     */
    private void updateWebflowDisplay(WebflowDisplayData displayData) {
        webflowRecorderView.showWebflow(displayData);
    }
    
    /**
     * Creates and returns a context menu for the webflow list
     * @param webflowList The webflow list component
     * @return Configured JPopupMenu
     */
    public JPopupMenu createWebflowContextMenu(JList<Webflow> webflowList) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem removeMenuItem = new JMenuItem("Remove Selected");
        removeMenuItem.addActionListener(e -> {
            java.util.List<Webflow> selectedWebflows = webflowList.getSelectedValuesList();
            handleRemoveSelectedWebflows(selectedWebflows);
        });
        
        contextMenu.add(removeMenuItem);
        return contextMenu;
    }
    
    /**
     * Handles context menu trigger for webflow list
     * @param webflowList The webflow list
     * @param index The clicked index
     * @param contextMenu The context menu to show
     * @param x X coordinate for menu
     * @param y Y coordinate for menu
     */
    public void handleContextMenuTrigger(JList<Webflow> webflowList, int index, 
                                       JPopupMenu contextMenu, int x, int y) {
        if (index >= 0) {
            // If the clicked item is not already selected, select it
            if (!webflowList.isSelectedIndex(index)) {
                webflowList.setSelectedIndex(index);
            }
            
            // Only show context menu if there are selected items
            if (webflowList.getSelectedIndices().length > 0) {
                contextMenu.show(webflowList, x, y);
            }
        }
    }
    
    /**
     * Handles view mode change (Pretty, JSON)
     * @param newMode The new view mode
     */
    public void handleViewModeChange(String newMode) {
        currentViewMode = newMode;
        updateViewModeButtons(currentViewMode);
        
        // Refresh the content display with the new view mode
        if (currentSelectedWebflow != null) {
            refreshWebflowDisplay(currentSelectedWebflow);
        }
    }
    
    /**
     * Update view mode button styles - internal implementation
     */
    public void updateViewModeButtons(String currentViewMode) {
        webflowRecorderView.selectViewMode(currentViewMode);
    }
    
    /**
     * Refresh the webflow display with the current view mode
     * @param webflow The webflow to display
     */
    private void refreshWebflowDisplay(Webflow webflow) {
        if (webflow == null) {
            return;
        }
        if ("JSON".equals(currentViewMode)) {
            displayWebflowAsJsonAsync(webflow);
            return;
        }

        cancelJsonRendering();
        webflowRecorderView.showPrettySteps();
        updateWebflowDisplay(displayWebflow(webflow));
    }

    private void cancelJsonRendering() {
        jsonRenderGeneration.incrementAndGet();
        Thread worker = jsonWorker.getAndSet(null);
        if (worker != null) {
            worker.interrupt();
        }
        webflowRecorderView.cancelJsonRendering();
        webflowRecorderView.setJsonLoading(false);
    }

    private void displayWebflowAsJsonAsync(Webflow webflow) {
        long generation = jsonRenderGeneration.incrementAndGet();
        Thread previousWorker = jsonWorker.getAndSet(null);
        if (previousWorker != null) {
            previousWorker.interrupt();
        }
        webflowRecorderView.setJsonLoading(true);

        Thread worker = new Thread(() -> {
            long startedAt = System.nanoTime();
            String jsonContent;
            Throwable failure = null;
            try {
                jsonContent = jsonFormatter.format(webflow);
                logger.logDebug("Prepared webflow JSON preview off-EDT in "
                        + ((System.nanoTime() - startedAt) / 1_000_000) + "ms ("
                        + jsonContent.length() + " characters)");
            } catch (Throwable throwable) {
                failure = throwable;
                jsonContent = "JSON Serialization Error: " + throwable.getMessage();
            }
            String result = jsonContent;
            Throwable renderingFailure = failure;
            Thread completedWorker = Thread.currentThread();
            SwingUtilities.invokeLater(() -> {
                jsonWorker.compareAndSet(completedWorker, null);
                if (generation != jsonRenderGeneration.get()
                        || !"JSON".equals(currentViewMode)
                        || currentSelectedWebflow != webflow) {
                    return;
                }
                updateWebflowDisplay(webflowDisplayData(webflow));
                if (renderingFailure != null) {
                    logger.logError("Unable to display webflow JSON: " + renderingFailure.getMessage());
                    webflowRecorderView.setJsonLoading(false);
                    updateStatus("Unable to display webflow JSON");
                }
                webflowRecorderView.showJsonText(result);
                if (renderingFailure == null) {
                    webflowRecorderView.setJsonLoading(false);
                }
            });
        }, "Courier-Webflow-JSON-" + generation);
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY);
        jsonWorker.set(worker);
        worker.start();
    }

    /**
     * Custom WebflowStep class for displaying JSON lines
     */
    public void stopRecording() {
        if (modal != null) {
            modal.stopRecording();
        }
    }

    @Override
    public void close() {
        jsonRenderGeneration.incrementAndGet();
        Thread worker = jsonWorker.getAndSet(null);
        if (worker != null) {
            worker.interrupt();
        }
        Runnable closeUi = () -> {
            webflowRecorderView.cancelJsonRendering();
            if (modal != null) {
                modal.dispose();
                modal = null;
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            closeUi.run();
        } else {
            SwingUtilities.invokeLater(closeUi);
        }
        sessionManager = null;
    }

}
