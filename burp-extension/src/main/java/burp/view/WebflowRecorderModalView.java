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

package burp.view;

import burp.controller.WebflowRecorderModalController;
import burp.model.Webflow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import burp.controller.LogController;
import burp.utils.SessionManager;

public class WebflowRecorderModalView extends JDialog {
    private WebflowRecorderModalController controller;
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JTextField projectNameField;
    private JTextField startUrlField;
    private JButton createButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    private ExecutorService executorService;
    private Timer loadingAnimationTimer;
    private String originalButtonText;
    private Color originalButtonColor;
    private boolean isRecording = false;
    private final Runnable onRecordingCompleted;
    private volatile boolean disposed;
    
    public WebflowRecorderModalView(Frame parent, Consumer<Webflow> onWebflowCreated, LogController logger, SessionManager sessionManager) {
        this(parent, onWebflowCreated, null, () -> { }, logger, sessionManager);
    }
    
    public WebflowRecorderModalView(Frame parent, Consumer<Webflow> onWebflowCreated,
            Consumer<Webflow.WebflowStep> onStepRecorded, Runnable onRecordingCompleted,
            LogController logger, SessionManager sessionManager) {
        super(parent, "Record a new webflow", true);
        getContentPane().setBackground(CourierTheme.background());
        getRootPane().putClientProperty("apple.awt.windowAppearance",
                CourierTheme.isDarkTheme() ? "NSAppearanceNameDarkAqua" : "NSAppearanceNameAqua");
        getRootPane().putClientProperty("apple.awt.transparentTitleBar", Boolean.FALSE);
        this.controller = new WebflowRecorderModalController(onWebflowCreated, onStepRecorded, logger, sessionManager);
        this.onRecordingCompleted = onRecordingCompleted == null ? () -> { } : onRecordingCompleted;
        
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 570);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(parent);
        
        // Store original button properties for reset
        originalButtonText = createButton.getText();
        originalButtonColor = createButton.getBackground();
    }
    
    private void initializeComponents() {
        // Input fields - let layout manager handle sizing
        nameField = new JTextField();
        nameField.setToolTipText("Enter a descriptive name for your webflow");
        
        descriptionArea = new JTextArea(5, 40);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setToolTipText("Describe what this webflow will test or accomplish");
        
        projectNameField = new JTextField();
        projectNameField.setText("Default Project");
        projectNameField.setToolTipText("Project name for organizing webflows");
        
        startUrlField = new JTextField();
        startUrlField.setText("https://");
        startUrlField.setToolTipText("The initial URL where the webflow recording will start");
        
        CourierTheme.styleInput(nameField);
        CourierTheme.styleInput(projectNameField);
        CourierTheme.styleInput(startUrlField);
        CourierTheme.configureTextArea(descriptionArea, false);

        createButton = CourierTheme.primaryButton("Start recording");
        cancelButton = new JButton("Cancel");
        CourierTheme.styleSecondary(cancelButton);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(CourierTheme.WARNING);
        statusLabel.setFont(CourierTheme.bodyFont(10));
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Main content panel with flexible layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(CourierTheme.background());
        mainPanel.setBorder(new EmptyBorder(22, 26, 18, 26));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("Record a new webflow", JLabel.LEFT);
        titleLabel.setFont(CourierTheme.titleFont(18));
        titleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Info section
        JTextArea infoArea = new JTextArea(
            "Courier launches Chromium through Burp, records browser interactions, correlates network " +
            "evidence, and uploads the completed workflow to Guard."
        );
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(CourierTheme.bodyFont(11));
        infoArea.setForeground(CourierTheme.muted());
        infoArea.setBorder(new EmptyBorder(0, 0, 18, 0));
        headerPanel.add(infoArea, BorderLayout.CENTER);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Form panel using BoxLayout for natural scaling
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        
        // Create form rows
        formPanel.add(createFormRow("Webflow Name:", nameField));
        formPanel.add(Box.createVerticalStrut(15));
        
        // Description row (special handling for text area)
        JPanel descPanel = new JPanel(new BorderLayout(10, 0));
        descPanel.setOpaque(false);
        descPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descPanel.setMinimumSize(new Dimension(400, 125));
        descPanel.setPreferredSize(new Dimension(600, 125));
        descPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        JLabel descLabel = new JLabel("Description:");
        descLabel.setPreferredSize(new Dimension(120, 28));
        descLabel.setForeground(CourierTheme.muted());
        descLabel.setFont(CourierTheme.bodyFont(10));
        descPanel.add(descLabel, BorderLayout.WEST);
        JScrollPane descScrollPane = CourierTheme.scroll(descriptionArea);
        descScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        descScrollPane.setPreferredSize(new Dimension(600, 110));
        descScrollPane.setMinimumSize(new Dimension(300, 100));
        descScrollPane.getViewport().setBackground(CourierTheme.elevatedSurface());
        descPanel.add(descScrollPane, BorderLayout.CENTER);
        formPanel.add(descPanel);
        formPanel.add(Box.createVerticalStrut(15));
        
        formPanel.add(createFormRow("Project Name:", projectNameField));
        formPanel.add(Box.createVerticalStrut(15));
        
        formPanel.add(createFormRow("Start URL:", startUrlField));
        formPanel.add(Box.createVerticalStrut(20));
        
        // Status label
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(statusLabel);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(CourierTheme.background());
        buttonPanel.setBorder(new EmptyBorder(10, 20, 16, 20));
        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createFormRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 28));
        label.setForeground(CourierTheme.muted());
        label.setFont(CourierTheme.bodyFont(10));
        row.add(label, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }
    
    private void setupEventHandlers() {
        // Create button
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createWebflow();
            }
        });
        
        // Cancel button
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        // Start URL validation on focus lost
        startUrlField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                validateStartUrl();
            }
        });
    }
    
    
    private void createWebflow() {
        if (isRecording) {
            return; // Prevent multiple clicks while recording
        }
        
        try {
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            String projectName = projectNameField.getText().trim();
            String startUrl = startUrlField.getText().trim();
            
            // Start loading animation and disable form
            startLoadingAnimation();
            setFormEnabled(false);
            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "Courier-Webflow-Modal");
                    thread.setDaemon(true);
                    return thread;
                });
            }
            // Run webflow creation in background thread
            CompletableFuture.supplyAsync(() -> {
                try {
                    controller.createWebflow(name, description, projectName, startUrl);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }, executorService).thenAccept(result -> {
                SwingUtilities.invokeLater(() -> {
                    if (disposed) {
                        controller.stopRecordingSession();
                        return;
                    }
                    if (result instanceof Exception) {
                        // Handle error
                        Exception e = (Exception) result;
                        if (e instanceof IllegalArgumentException) {
                            setStatusMessage(e.getMessage(), false);
                        } else {
                            setStatusMessage("Error creating webflow: " + e.getMessage(), false);
                        }
                        // Reset UI state on error
                        stopLoadingAnimation();
                        setFormEnabled(true);
                    } else {
                        // Success - start monitoring for recording completion
                        monitorRecordingCompletion();
                    }
                });
            });
            
        } catch (Exception e) {
            setStatusMessage("Error starting webflow creation: " + e.getMessage(), false);
            stopLoadingAnimation();
            setFormEnabled(true);
        }
    }
    
    private void validateStartUrl() {
        String startUrl = startUrlField.getText().trim();
        if (!startUrl.isEmpty()) {
            WebflowRecorderModalController.ValidationResult result = 
                controller.validateStartUrl(startUrl);
            
            if (result.isSuccess()) {
                setStatusMessage(result.getMessage(), true);
            } else {
                setStatusMessage(result.getMessage(), false);
            }
        }
    }
    
    private void setStatusMessage(String message, boolean isSuccess) {
        statusLabel.setText(message);
        statusLabel.setForeground(isSuccess ? CourierTheme.SUCCESS : CourierTheme.WARNING);
    }
    
    private void startLoadingAnimation() {
        isRecording = true;
        setStatusMessage("Starting webflow recording...", true);
        
        // Create hourglass animation
        String[] frames = {"⏳", "⌛"};
        final int[] currentFrame = {0};
        
        loadingAnimationTimer = new Timer(500, e -> {
            String frameText = frames[currentFrame[0] % frames.length];
            createButton.setText(frameText + " Recording...");
            currentFrame[0]++;
        });
        
        loadingAnimationTimer.start();
        createButton.setEnabled(false);
    }
    
    private void stopLoadingAnimation() {
        isRecording = false;
        
        if (loadingAnimationTimer != null) {
            loadingAnimationTimer.stop();
            loadingAnimationTimer = null;
        }
        
        // Reset button to original state
        createButton.setText(originalButtonText);
        createButton.setBackground(originalButtonColor);
        createButton.setEnabled(true);
    }
    
    private void setFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        descriptionArea.setEnabled(enabled);
        projectNameField.setEnabled(enabled);
        startUrlField.setEnabled(enabled);
        createButton.setEnabled(enabled && !isRecording);
        // Keep cancel button always enabled
    }
    
    private void monitorRecordingCompletion() {
        // Monitor the recording session in a background thread
        CompletableFuture.runAsync(() -> {
            try {
                // Poll the controller to check if recording is still active
                while (controller.isRecording()) {
                    Thread.sleep(1000); // Check every second
                }
                
                // Recording completed - close modal on EDT
                SwingUtilities.invokeLater(() -> {
                    if (disposed) {
                        return;
                    }
                    stopLoadingAnimation();
                    setStatusMessage("Recording completed", true);
                    onRecordingCompleted.run();
                    
                    // Close modal after a brief delay to show success message
                    Timer closeTimer = new Timer(1500, e -> dispose());
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(() -> {
                    stopLoadingAnimation();
                    setFormEnabled(true);
                    setStatusMessage("Recording monitoring interrupted", false);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    stopLoadingAnimation();
                    setFormEnabled(true);
                    setStatusMessage("Error monitoring recording: " + e.getMessage(), false);
                });
            }
        }, executorService);
    }
    
    public void showModal() {
        // Run modal in separate thread to prevent blocking main UI
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
    
    public void stopRecording() {
        controller.stopRecordingSession();
    }

    @Override
    public void dispose() {
        disposed = true;
        controller.stopRecordingSession();
        if (loadingAnimationTimer != null) {
            loadingAnimationTimer.stop();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        setFormEnabled(true);
        super.dispose();
    }
}
