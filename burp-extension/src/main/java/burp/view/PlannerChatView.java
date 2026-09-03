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

import burp.model.ChatMessage;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class PlannerChatView {
    private static final int COLLAPSE_CHARACTERS = 1_000;
    private static final int COLLAPSE_LINES = 14;
    private final JPanel messages = new JPanel();
    private final JScrollPane scrollPane;
    private final StringBuilder transcript = new StringBuilder();
    private JPanel pendingRow;
    private Timer pendingTimer;
    private int nextMessageRow;

    public PlannerChatView() {
        messages.setLayout(new GridBagLayout());
        messages.setBackground(CourierTheme.background());
        messages.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel viewport = new JPanel(new BorderLayout());
        viewport.setBackground(CourierTheme.background());
        viewport.add(messages, BorderLayout.NORTH);
        scrollPane = CourierTheme.scroll(viewport);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
    }

    public JScrollPane component() {
        return scrollPane;
    }

    public void append(String sender, String content, ChatMessage.MessageType type, String timestamp) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> append(sender, content, type, timestamp));
            return;
        }
        String safeContent = content == null ? "" : content;
        transcript.append(timestamp == null || timestamp.isEmpty() ? "" : "[" + timestamp + "] ")
                .append('<').append(sender).append("> ").append(safeContent).append('\n');
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel bubble = bubble(sender, safeContent, type, timestamp);
        if (type == ChatMessage.MessageType.USER) {
            row.add(Box.createHorizontalStrut(80), BorderLayout.WEST);
            row.add(bubble, BorderLayout.EAST);
        } else {
            row.add(bubble, BorderLayout.WEST);
            row.add(Box.createHorizontalStrut(80), BorderLayout.EAST);
        }
        addMessageRow(row);
        messages.revalidate();
        messages.repaint();
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar()
                .setValue(scrollPane.getVerticalScrollBar().getMaximum()));
    }

    public void showGuardPending() {
        Runnable show = () -> {
            resolveGuardPending();
            pendingRow = new JPanel(new BorderLayout());
            pendingRow.setOpaque(false);
            JPanel bubble = CourierTheme.roundedPanel(new BorderLayout(8, 0), CourierTheme.surface());
            bubble.setBorder(new CompoundBorder(
                    CourierTheme.roundedBorder(CourierTheme.borderColor(), 16),
                    new EmptyBorder(8, 10, 8, 10)));
            JLabel sender = new JLabel("Guard");
            sender.setForeground(CourierTheme.muted());
            sender.setFont(CourierTheme.bodyFont(8).deriveFont(java.awt.Font.BOLD));
            JLabel spinner = new JLabel("⠋  Guard is working…");
            spinner.setForeground(CourierTheme.muted());
            spinner.setFont(CourierTheme.bodyFont(9));
            bubble.add(sender, BorderLayout.NORTH);
            bubble.add(spinner, BorderLayout.CENTER);
            pendingRow.add(bubble, BorderLayout.WEST);
            addMessageRow(pendingRow);
            String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
            int[] frame = {0};
            pendingTimer = new Timer(90, event -> {
                spinner.setText(frames[frame[0]++ % frames.length] + "  Guard is working…");
            });
            pendingTimer.start();
            messages.revalidate();
            messages.repaint();
            scrollToBottom();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            SwingUtilities.invokeLater(show);
        }
    }

    public void resolveGuardPending() {
        Runnable resolve = () -> {
            if (pendingTimer != null) {
                pendingTimer.stop();
                pendingTimer = null;
            }
            if (pendingRow != null) {
                messages.remove(pendingRow);
                pendingRow = null;
                messages.revalidate();
                messages.repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            resolve.run();
        } else {
            SwingUtilities.invokeLater(resolve);
        }
    }

    public void clear() {
        Runnable clear = () -> {
            resolveGuardPending();
            transcript.setLength(0);
            nextMessageRow = 0;
            messages.removeAll();
            messages.revalidate();
            messages.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            clear.run();
        } else {
            SwingUtilities.invokeLater(clear);
        }
    }

    public String getTranscriptText() {
        return transcript.toString();
    }

    private JPanel bubble(String sender, String content, ChatMessage.MessageType type, String timestamp) {
        Color border = type == ChatMessage.MessageType.USER
                ? CourierTheme.ACCENT : type == ChatMessage.MessageType.ERROR
                ? CourierTheme.WARNING : CourierTheme.borderColor();
        Color background = type == ChatMessage.MessageType.USER
                ? CourierTheme.selectionBackground() : CourierTheme.surface();
        JPanel bubble = CourierTheme.roundedPanel(new BorderLayout(0, 5), background);
        bubble.setBorder(new CompoundBorder(CourierTheme.roundedBorder(border, 16),
                new EmptyBorder(8, 10, 8, 10)));
        JLabel who = new JLabel(sender + (timestamp == null || timestamp.isEmpty() ? "" : "  ·  " + timestamp));
        who.setForeground(type == ChatMessage.MessageType.ERROR
                ? CourierTheme.WARNING : CourierTheme.muted());
        who.setFont(CourierTheme.bodyFont(8).deriveFont(java.awt.Font.BOLD));
        bubble.add(who, BorderLayout.NORTH);
        CollapsibleMarkdown markdown = new CollapsibleMarkdown(content, background);
        markdown.setLayoutChanged(() -> {
            bubble.setPreferredSize(null);
            bubble.revalidate();
            if (bubble.getParent() instanceof JPanel row) {
                row.setPreferredSize(null);
                row.revalidate();
            }
            messages.revalidate();
            messages.repaint();
        });
        bubble.add(markdown, BorderLayout.CENTER);
        return bubble;
    }

    private static final class CollapsibleMarkdown extends JPanel {
        private final String content;
        private final Color background;
        private final boolean collapsible;
        private final JEditorPane editor = new JEditorPane();
        private final JScrollPane editorScroll = new JScrollPane(editor);
        private final JButton toggle = new JButton();
        private Runnable layoutChanged = () -> { };
        private boolean expanded;

        private CollapsibleMarkdown(String content, Color background) {
            super(new BorderLayout(0, 5));
            this.content = content;
            this.background = background;
            this.collapsible = content.length() > COLLAPSE_CHARACTERS
                    || content.lines().count() > COLLAPSE_LINES;
            setOpaque(false);
            editor.setEditable(false);
            editor.setEditorKit(new HTMLEditorKit());
            editor.setOpaque(false);
            editor.setBorder(null);
            editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            editor.setFont(CourierTheme.bodyFont(9));
            editorScroll.setBorder(null);
            editorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            editorScroll.getViewport().setOpaque(false);
            editorScroll.setOpaque(false);
            add(editorScroll, BorderLayout.CENTER);
            if (collapsible) {
                CourierTheme.styleSecondary(toggle);
                toggle.setText("Expand");
                toggle.addActionListener(event -> {
                    expanded = !expanded;
                    render();
                    revalidate();
                    repaint();
                    layoutChanged.run();
                });
                JPanel action = new JPanel(new BorderLayout());
                action.setOpaque(false);
                action.add(toggle, BorderLayout.WEST);
                add(action, BorderLayout.SOUTH);
            }
            render();
        }

        private void setLayoutChanged(Runnable callback) {
            layoutChanged = callback == null ? () -> { } : callback;
        }

        private void render() {
            String visible = expanded || !collapsible ? content : collapsedContent();
            editor.setText(new MarkdownRenderer().render(visible, background));
            editor.setSize(new Dimension(600, Short.MAX_VALUE));
            Dimension preferred = editor.getPreferredSize();
            editor.setPreferredSize(new Dimension(600, preferred.height));
            editor.setMaximumSize(new Dimension(600, preferred.height));
            int visibleHeight = expanded && collapsible
                    ? Math.min(420, preferred.height) : preferred.height;
            editorScroll.setPreferredSize(new Dimension(605, Math.max(40, visibleHeight)));
            editorScroll.setVerticalScrollBarPolicy(expanded && collapsible
                    ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                    : JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            editor.setCaretPosition(0);
            if (collapsible) {
                toggle.setText(expanded ? "Collapse" : "Expand");
            }
        }

        private String collapsedContent() {
            int end = Math.min(content.length(), COLLAPSE_CHARACTERS);
            String preview = content.substring(0, end);
            int lines = 1;
            int index = 0;
            while (lines <= COLLAPSE_LINES && (index = preview.indexOf('\n', index)) >= 0) {
                lines++;
                index++;
                if (lines > COLLAPSE_LINES) {
                    preview = preview.substring(0, index - 1);
                }
            }
            return preview + "\n\n…";
        }
    }

    private void addMessageRow(JPanel row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = nextMessageRow++;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 8, 0);
        messages.add(row, constraints);
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar()
                .setValue(scrollPane.getVerticalScrollBar().getMaximum()));
    }
}
