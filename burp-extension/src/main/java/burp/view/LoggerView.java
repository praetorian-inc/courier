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

import burp.api.montoya.MontoyaApi;
import burp.controller.LogController;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LoggerView {
    private static final Pattern LOG_LINE = Pattern.compile("^(\\[[^]]+])(\\[(ERROR|INFO|DEBUG)])\\s+(.*)$");
    private final LogController logController;
    private JPanel logPanel;
    private JTextPane logArea;
    private Style timestampStyle;
    private Style messageStyle;
    private Style infoStyle;
    private Style errorStyle;
    private Style debugStyle;

    public LoggerView(MontoyaApi api, String buildTimestamp) {
        logController = new LogController(this, api, buildTimestamp, LogController.LOG_LEVEL_INFO);
    }

    public LogController getLogController() {
        return logController;
    }

    public JTextPane getLogArea() {
        return logArea;
    }

    public JPanel getLogPanel() {
        return logPanel;
    }

    public JPanel createLoggerPanel() {
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setBackground(CourierTheme.background());
        logArea.setForeground(CourierTheme.text());
        logArea.setCaretColor(CourierTheme.text());
        logArea.setFont(CourierTheme.monoFont(11));
        logArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 10, 9, 10));
        createStyles();

        JComboBox<String> logLevel = new JComboBox<>(new String[] {"Error", "Info", "Debug"});
        logLevel.setSelectedIndex(LogController.LOG_LEVEL_INFO);
        CourierTheme.styleCombo(logLevel);
        logLevel.setToolTipText("Minimum activity level shown and written by Courier");
        logLevel.addActionListener(event -> logController.setCurrentLogLevel(logLevel.getSelectedIndex()));

        JButton clearButton = new JButton("Clear");
        CourierTheme.styleSecondary(clearButton);
        clearButton.addActionListener(event -> logController.handleClearLog());
        JButton locationButton = new JButton("Open log folder");
        CourierTheme.styleSecondary(locationButton);
        locationButton.addActionListener(logController::showLogFileLocation);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(CourierTheme.scroll(logArea), BorderLayout.CENTER);
        logPanel = CourierTheme.card("Activity", content,
                CourierTheme.actionBar(logLevel, clearButton, locationButton));
        return logPanel;
    }

    public void appendLogBatch(String batch) {
        if (logArea == null || batch == null || batch.isEmpty()) {
            return;
        }
        StyledDocument document = logArea.getStyledDocument();
        for (String line : batch.split("\\R")) {
            if (line.isEmpty()) {
                continue;
            }
            appendLine(document, line);
        }
        logArea.setCaretPosition(document.getLength());
    }

    private void appendLine(StyledDocument document, String line) {
        Matcher matcher = LOG_LINE.matcher(line);
        try {
            if (!matcher.matches()) {
                document.insertString(document.getLength(), line + "\n", messageStyle);
                return;
            }
            document.insertString(document.getLength(), matcher.group(1), timestampStyle);
            document.insertString(document.getLength(), matcher.group(2), styleFor(matcher.group(3)));
            document.insertString(document.getLength(), " " + matcher.group(4) + "\n", messageStyle);
        } catch (BadLocationException ignored) {
        }
    }

    private Style styleFor(String level) {
        return switch (level) {
            case "ERROR" -> errorStyle;
            case "DEBUG" -> debugStyle;
            default -> infoStyle;
        };
    }

    private void createStyles() {
        timestampStyle = logArea.addStyle("timestamp", null);
        StyleConstants.setFontFamily(timestampStyle, FontNames.MONO);
        StyleConstants.setFontSize(timestampStyle, 13);
        StyleConstants.setForeground(timestampStyle, CourierTheme.muted());
        messageStyle = logArea.addStyle("message", timestampStyle);
        StyleConstants.setForeground(messageStyle, CourierTheme.text());
        infoStyle = logArea.addStyle("info", timestampStyle);
        StyleConstants.setForeground(infoStyle, new Color(93, 183, 255));
        StyleConstants.setBold(infoStyle, true);
        errorStyle = logArea.addStyle("error", timestampStyle);
        StyleConstants.setForeground(errorStyle, CourierTheme.ACCENT);
        StyleConstants.setBold(errorStyle, true);
        debugStyle = logArea.addStyle("debug", timestampStyle);
        StyleConstants.setForeground(debugStyle, CourierTheme.WARNING);
        StyleConstants.setBold(debugStyle, true);
    }

    private static final class FontNames {
        private static final String MONO = Font.MONOSPACED;
    }
}
