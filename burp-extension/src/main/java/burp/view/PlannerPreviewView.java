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

import burp.controller.PlannerController;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

final class PlannerPreviewView {
    private final PlannerController controller;
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JTextPane requestArea = createTextPane("No request selected");
    private final JTextPane responseArea = createTextPane("No response available");
    private final JButton prettyButton = modeButton("Pretty");
    private final JButton rawButton = modeButton("Raw");
    private final JButton hexButton = modeButton("Hex");
    private JScrollPane requestScroll;
    private JScrollPane responseScroll;

    PlannerPreviewView(PlannerController controller) {
        this.controller = controller;
    }

    JPanel createPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                messagePanel("Request", requestArea), messagePanel("Response", responseArea));
        split.setResizeWeight(0.5);
        split.setDividerLocation(0.5);
        CourierTheme.configureSplitPane(split);
        panel.setOpaque(false);
        panel.add(CourierTheme.card("Evidence preview", split, modePanel()), BorderLayout.CENTER);
        selectMode("Pretty");
        return panel;
    }

    JTextPane requestArea() {
        return requestArea;
    }

    JTextPane responseArea() {
        return responseArea;
    }

    void hide() {
        clear();
    }

    void show() {
        panel.setVisible(true);
    }

    void clear() {
        requestArea.setText("No request selected");
        responseArea.setText("No response available");
    }

    void selectMode(String mode) {
        boolean pretty = "Pretty".equals(mode);
        CourierTheme.styleSegment(prettyButton, pretty);
        CourierTheme.styleSegment(rawButton, "Raw".equals(mode));
        CourierTheme.styleSegment(hexButton, "Hex".equals(mode));
        ((WrappingTextPane) requestArea).setWrap(pretty);
        ((WrappingTextPane) responseArea).setWrap(pretty);
        int policy = pretty ? JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                : JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        if (requestScroll != null) {
            requestScroll.setHorizontalScrollBarPolicy(policy);
            responseScroll.setHorizontalScrollBarPolicy(policy);
        }
    }

    void resetCaret() {
        requestArea.setCaretPosition(0);
        responseArea.setCaretPosition(0);
    }

    private JPanel modePanel() {
        return CourierTheme.segmentedControl(prettyButton, rawButton, hexButton);
    }

    private JButton modeButton(String mode) {
        JButton button = new JButton(mode);
        button.setPreferredSize(new Dimension(64, 27));
        button.addActionListener(event -> controller.handleViewModeChange(mode));
        return button;
    }

    private JPanel messagePanel(String title, JTextPane textArea) {
        JPanel message = new JPanel(new BorderLayout());
        message.setBackground(CourierTheme.surface());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CourierTheme.surface());
        header.setBorder(new EmptyBorder(6, 9, 6, 9));
        JButton collapse = new JButton("▼");
        collapse.setBorderPainted(false);
        collapse.setContentAreaFilled(false);
        collapse.setFocusPainted(false);
        collapse.setPreferredSize(new Dimension(22, 20));
        JLabel label = new JLabel(title.toUpperCase());
        label.setFont(CourierTheme.bodyFont(9).deriveFont(java.awt.Font.BOLD));
        label.setForeground(CourierTheme.muted());
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(collapse);
        titlePanel.add(Box.createHorizontalStrut(5));
        titlePanel.add(label);
        header.add(titlePanel, BorderLayout.WEST);

        JScrollPane scroll = CourierTheme.scroll(textArea);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (textArea == requestArea) {
            requestScroll = scroll;
        } else {
            responseScroll = scroll;
        }
        message.add(header, BorderLayout.NORTH);
        message.add(scroll, BorderLayout.CENTER);
        collapse.addActionListener(event -> {
            boolean visible = !scroll.isVisible();
            scroll.setVisible(visible);
            collapse.setText(visible ? "▼" : "▶");
            message.revalidate();
        });
        return message;
    }

    private static JTextPane createTextPane(String initialText) {
        JTextPane area = new WrappingTextPane();
        area.setEditable(false);
        area.setFont(CourierTheme.monoFont(11));
        area.setBackground(CourierTheme.codeBackground());
        area.setForeground(CourierTheme.text());
        area.setCaretColor(CourierTheme.text());
        area.setText(initialText);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        return area;
    }

    private static final class WrappingTextPane extends JTextPane {
        private boolean wrap = true;

        void setWrap(boolean wrap) {
            this.wrap = wrap;
            revalidate();
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return wrap;
        }
    }
}
