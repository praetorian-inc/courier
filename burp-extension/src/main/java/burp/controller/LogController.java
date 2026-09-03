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
import burp.view.LoggerView;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import burp.utils.SecureFiles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogController implements AutoCloseable {
    public static final int LOG_LEVEL_ERROR = 0;
    public static final int LOG_LEVEL_INFO = 1;
    public static final int LOG_LEVEL_DEBUG = 2;

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int MAX_UI_CHARACTERS = 500_000;

    private final MontoyaApi api;
    private final Logs logs;
    private final LoggerView loggerView;
    private final ConcurrentLinkedQueue<String> pendingUiMessages = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean uiFlushScheduled = new AtomicBoolean();
    private volatile int currentLogLevel;

    public LogController(LoggerView loggerView, MontoyaApi api, String buildTimestamp, int currentLogLevel) {
        this.api = api;
        this.loggerView = loggerView;
        this.currentLogLevel = currentLogLevel;
        this.logs = new Logs(api);
        logs.initialize(buildTimestamp);
    }

    public void setCurrentLogLevel(int currentLogLevel) {
        this.currentLogLevel = currentLogLevel;
    }

    public int getCurrentLogLevel() {
        return currentLogLevel;
    }

    public void handleClearLog() {
        if (loggerView.getLogArea() != null) {
            loggerView.getLogArea().setText("");
        }
    }

    public void logMessage(String message) {
        logMessage(message, LOG_LEVEL_INFO);
    }

    public void logError(String message) {
        logMessage(message, LOG_LEVEL_ERROR);
    }

    public void logDebug(String message) {
        logMessage(message, LOG_LEVEL_DEBUG);
    }

    public void logInfo(String message) {
        logMessage(message, LOG_LEVEL_INFO);
    }

    private void logMessage(String message, int level) {
        if (level > currentLogLevel) {
            return;
        }
        String prefix = switch (level) {
            case LOG_LEVEL_ERROR -> "[ERROR] ";
            case LOG_LEVEL_DEBUG -> "[DEBUG] ";
            default -> "[INFO] ";
        };
        String formatted = "[" + LocalDateTime.now().format(DISPLAY_TIME) + "]" + prefix + message;
        api.logging().logToOutput(formatted);
        logs.write(formatted);
        pendingUiMessages.add(formatted);
        scheduleUiFlush();
    }

    private void scheduleUiFlush() {
        if (loggerView.getLogArea() == null || !uiFlushScheduled.compareAndSet(false, true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                StringBuilder batch = new StringBuilder();
                String message;
                while ((message = pendingUiMessages.poll()) != null) {
                    batch.append(message).append('\n');
                }
                loggerView.appendLogBatch(batch.toString());
                trimUiLog();
            } finally {
                uiFlushScheduled.set(false);
                if (!pendingUiMessages.isEmpty()) {
                    scheduleUiFlush();
                }
            }
        });
    }

    private void trimUiLog() {
        int excess = loggerView.getLogArea().getDocument().getLength() - MAX_UI_CHARACTERS;
        if (excess <= 0) {
            return;
        }
        try {
            loggerView.getLogArea().getDocument().remove(0, excess);
        } catch (BadLocationException ignored) {
        }
    }

    public void showLogFileLocation(ActionEvent event) {
        String logFilePath = logs.getLogFilePath();
        if (logFilePath == null || !logs.isEnabled()) {
            JOptionPane.showMessageDialog(loggerView.getLogPanel(),
                    "File logging is not currently available.",
                    "File Logging Not Available", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextArea textArea = new JTextArea("Log files are saved to:\n\n" + logFilePath
                + "\n\nDirectory: " + new File(logFilePath).getParent());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(loggerView.getLogPanel().getBackground());
        JOptionPane.showMessageDialog(loggerView.getLogPanel(), textArea,
                "Log File Location", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void close() {
        logs.close();
    }

    private static final class Logs implements AutoCloseable {
        private static final long MAX_LOG_FILE_SIZE = 10L * 1024 * 1024;
        private static final int MAX_LOG_FILES = 5;
        private static final int FLUSH_INTERVAL = 20;

        private final MontoyaApi api;
        private final Object lock = new Object();
        private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Courier-Log-Flusher");
            thread.setDaemon(true);
            return thread;
        });
        private BufferedWriter writer;
        private String logFilePath;
        private boolean enabled = true;
        private int writesSinceFlush;

        private Logs(MontoyaApi api) {
            this.api = api;
            flusher.scheduleWithFixedDelay(this::flushPending, 1, 1, TimeUnit.SECONDS);
        }

        void initialize(String buildTimestamp) {
            try {
                Path logDirectory = Paths.get(System.getProperty("user.home"), "BurpCourier", "logs");
                SecureFiles.createPrivateDirectories(logDirectory);
                String filename = "courier-" + LocalDateTime.now().format(FILE_TIME) + ".log";
                logFilePath = logDirectory.resolve(filename).toString();
                writer = SecureFiles.newPrivateAppendWriter(
                        Path.of(logFilePath), StandardCharsets.UTF_8);
                write("=== Courier Session Started ===");
                write("Build Timestamp: " + buildTimestamp);
                flush();
            } catch (Exception exception) {
                enabled = false;
                api.logging().logToError("Failed to initialize file logging: " + exception.getMessage());
            }
        }

        void write(String message) {
            if (!enabled || writer == null) {
                return;
            }
            synchronized (lock) {
                try {
                    writer.write(message);
                    writer.newLine();
                    writesSinceFlush++;
                    if (writesSinceFlush >= FLUSH_INTERVAL) {
                        flush();
                        rotateIfNeeded();
                    }
                } catch (IOException exception) {
                    enabled = false;
                    api.logging().logToError("File logging disabled: " + exception.getMessage());
                }
            }
        }

        private void flushPending() {
            synchronized (lock) {
                if (writer == null || writesSinceFlush == 0) {
                    return;
                }
                try {
                    flush();
                    rotateIfNeeded();
                } catch (IOException exception) {
                    enabled = false;
                    api.logging().logToError("Unable to flush Courier log: " + exception.getMessage());
                }
            }
        }

        private void flush() throws IOException {
            if (writer != null) {
                writer.flush();
                writesSinceFlush = 0;
            }
        }

        private void rotateIfNeeded() throws IOException {
            File current = new File(logFilePath);
            if (current.length() <= MAX_LOG_FILE_SIZE) {
                return;
            }
            writer.close();
            for (int index = MAX_LOG_FILES - 1; index >= 1; index--) {
                File source = new File(logFilePath + "." + index);
                File destination = new File(logFilePath + "." + (index + 1));
                if (source.exists()) {
                    Files.move(source.toPath(), destination.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Files.move(current.toPath(), Paths.get(logFilePath + ".1"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            SecureFiles.writePrivateString(
                    Path.of(logFilePath), "", StandardCharsets.UTF_8);
            writer = SecureFiles.newPrivateAppendWriter(
                    Path.of(logFilePath), StandardCharsets.UTF_8);
        }

        String getLogFilePath() {
            return logFilePath;
        }

        boolean isEnabled() {
            return enabled;
        }

        @Override
        public void close() {
            flusher.shutdownNow();
            synchronized (lock) {
                if (writer == null) {
                    return;
                }
                try {
                    writer.write("=== Courier Session Ended ===");
                    writer.newLine();
                    flush();
                    writer.close();
                } catch (IOException exception) {
                    api.logging().logToError("Error closing log file: " + exception.getMessage());
                } finally {
                    writer = null;
                }
            }
        }
    }
}
