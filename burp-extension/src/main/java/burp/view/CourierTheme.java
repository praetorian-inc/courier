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

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

public final class CourierTheme {
    public static final Color ACCENT = new Color(238, 54, 87);
    public static final Color ACCENT_HOVER = new Color(255, 86, 72);
    public static final Color SUCCESS = new Color(45, 190, 118);
    public static final Color WARNING = new Color(225, 165, 62);
    public static final int GAP = 12;

    private CourierTheme() {
    }

    public static boolean isDarkTheme() {
        return isDark(uiColor("Panel.background", new Color(32, 34, 38)));
    }

    public static Color background() {
        Color burpBackground = uiColor("Panel.background", new Color(32, 34, 38));
        return isDark(burpBackground) ? new Color(9, 10, 12) : new Color(239, 241, 244);
    }

    public static Color surface() {
        return isDark(background()) ? new Color(18, 20, 24) : Color.WHITE;
    }

    public static Color elevatedSurface() {
        return isDark(background()) ? new Color(26, 29, 34) : new Color(245, 246, 248);
    }

    public static Color borderColor() {
        return isDark(background()) ? new Color(50, 54, 62) : new Color(198, 202, 209);
    }

    public static Color text() {
        return isDark(background()) ? new Color(242, 243, 245) : new Color(31, 34, 40);
    }

    public static Color muted() {
        return isDark(background()) ? new Color(164, 171, 182) : new Color(96, 104, 116);
    }

    public static Color codeBackground() {
        return isDark(background()) ? new Color(7, 8, 10) : new Color(248, 249, 251);
    }

    public static Color selectionBackground() {
        return isDark(background()) ? new Color(43, 22, 29) : new Color(252, 232, 237);
    }

    public static Font titleFont(float size) {
        return baseFont().deriveFont(Font.BOLD, size + 2f);
    }

    public static Font bodyFont(float size) {
        return baseFont().deriveFont(Font.PLAIN, size + 3f);
    }

    public static Font monoFont(float size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size + 3f));
    }

    public static JPanel pageHeader(String title, String subtitle, JComponent actions) {
        JPanel header = new JPanel(new BorderLayout(GAP, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(2, 2, 14, 2));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleFont(18));
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(muted());
        subtitleLabel.setFont(bodyFont(11));
        copy.add(titleLabel);
        copy.add(Box.createVerticalStrut(2));
        copy.add(subtitleLabel);
        header.add(copy, BorderLayout.CENTER);
        if (actions != null) {
            header.add(actions, BorderLayout.EAST);
        }
        return header;
    }

    public static JPanel card(String title, JComponent content) {
        return card(title, content, null);
    }

    public static JPanel card(String title, JComponent content, JComponent trailing) {
        JPanel card = roundedPanel(new BorderLayout(), surface());
        card.setBorder(cardBorder());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(elevatedSurface());
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor()),
                new EmptyBorder(9, 12, 9, 12)));
        JLabel label = new JLabel(title);
        label.setFont(titleFont(12));
        header.add(label, BorderLayout.WEST);
        if (trailing != null) {
            header.add(trailing, BorderLayout.EAST);
        }
        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    public static JPanel roundedPanel(LayoutManager layout, Color background) {
        return new RoundedPanel(layout, background);
    }

    public static JPanel paddedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        return panel;
    }

    public static JPanel actionBar(Component... components) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        for (Component component : components) {
            actions.add(component);
        }
        return actions;
    }

    public static JLabel chip(String text) {
        JLabel label = new JLabel("  " + text + "  ");
        label.setForeground(text());
        label.setBackground(selectionBackground());
        label.setOpaque(true);
        label.setFont(bodyFont(9).deriveFont(Font.BOLD));
        label.setBorder(new RoundedBorder(ACCENT, 9));
        return label;
    }

    public static JLabel statusPill(String text, Color color) {
        JLabel label = new JLabel(text);
        setStatus(label, text, color);
        return label;
    }

    public static void setStatus(JLabel label, String text, Color color) {
        label.setText("  ●  " + text + "  ");
        label.setForeground(color);
        label.setFont(bodyFont(10).deriveFont(Font.BOLD));
        label.setOpaque(true);
        label.setBackground(withAlphaBlend(color, background(), 0.12f));
        label.setBorder(new RoundedBorder(withAlphaBlend(color, borderColor(), 0.45f), 12));
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        stylePrimary(button);
        return button;
    }

    public static void stylePrimary(JButton button) {
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFont(bodyFont(11).deriveFont(Font.BOLD));
        button.setBorder(new CompoundBorder(new RoundedBorder(ACCENT, 10), new EmptyBorder(6, 11, 6, 11)));
        button.setFocusPainted(false);
        button.setOpaque(true);
    }

    public static void styleSecondary(JButton button) {
        button.setBackground(elevatedSurface());
        button.setForeground(text());
        button.setFont(bodyFont(11).deriveFont(Font.BOLD));
        button.setBorder(new CompoundBorder(new RoundedBorder(borderColor(), 10), new EmptyBorder(6, 10, 6, 10)));
        button.setFocusPainted(false);
        button.setOpaque(true);
    }

    public static void styleSegment(AbstractButton button, boolean selected) {
        button.setFont(bodyFont(10).deriveFont(Font.BOLD));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(selected ? ACCENT : elevatedSurface());
        button.setForeground(selected ? Color.WHITE : muted());
        button.setBorder(new EmptyBorder(5, 10, 5, 10));
    }

    public static JPanel segmentedControl(AbstractButton... buttons) {
        JPanel panel = roundedPanel(new GridLayout(1, buttons.length, 0, 0), elevatedSurface());
        panel.setBorder(new RoundedBorder(borderColor(), 10));
        for (AbstractButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }

    public static void styleInput(JTextField input) {
        input.setFont(bodyFont(10));
        input.setBackground(elevatedSurface());
        input.setOpaque(false);
        input.setForeground(text());
        input.setCaretColor(text());
        input.setBorder(new CompoundBorder(new RoundedBorder(borderColor(), 9), new EmptyBorder(5, 8, 5, 8)));
        input.setMinimumSize(new Dimension(120, 36));
    }

    public static void styleCombo(JComboBox<?> comboBox) {
        comboBox.setFont(bodyFont(10));
        comboBox.setBackground(elevatedSurface());
        comboBox.setForeground(text());
        comboBox.setPreferredSize(new Dimension(comboBox.getPreferredSize().width, 34));
    }

    public static void configureTable(JTable table) {
        table.setFont(bodyFont(10));
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(borderColor());
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(surface());
        table.setForeground(text());
        table.setSelectionBackground(selectionBackground());
        table.setSelectionForeground(text());
        table.getTableHeader().setBackground(elevatedSurface());
        table.getTableHeader().setForeground(muted());
        table.getTableHeader().setFont(bodyFont(10).deriveFont(Font.BOLD));
        table.setFillsViewportHeight(true);
    }

    public static void configureTextArea(JTextArea area, boolean monospace) {
        area.setBackground(isDark(background()) ? darken(background(), 8) : elevatedSurface());
        area.setForeground(text());
        area.setCaretColor(text());
        area.setFont(monospace ? monoFont(11) : bodyFont(12));
        area.setBorder(new EmptyBorder(9, 10, 9, 10));
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(new RoundedBorder(borderColor(), 10));
        scroll.getViewport().setBackground(surface());
        return scroll;
    }

    public static void configureSplitPane(JSplitPane splitPane) {
        splitPane.setUI(new CourierSplitPaneUI());
        splitPane.setBackground(background());
        splitPane.setBorder(null);
        splitPane.setDividerSize(9);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(false);
    }

    public static Border cardBorder() {
        return roundedBorder(borderColor(), 16);
    }

    public static Border roundedBorder(Color color, int arc) {
        return new RoundedBorder(color, arc);
    }

    private static Font baseFont() {
        Font font = UIManager.getFont("Label.font");
        return font == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 12) : font;
    }

    private static Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color == null ? fallback : color;
    }

    private static boolean isDark(Color color) {
        return color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114 < 128;
    }

    private static Color brighten(Color color, int amount) {
        return new Color(Math.min(255, color.getRed() + amount), Math.min(255, color.getGreen() + amount),
                Math.min(255, color.getBlue() + amount));
    }

    private static Color darken(Color color, int amount) {
        return new Color(Math.max(0, color.getRed() - amount), Math.max(0, color.getGreen() - amount),
                Math.max(0, color.getBlue() - amount));
    }

    private static Color withAlphaBlend(Color foreground, Color background, float alpha) {
        float inverse = 1f - alpha;
        return new Color(Math.round(foreground.getRed() * alpha + background.getRed() * inverse),
                Math.round(foreground.getGreen() * alpha + background.getGreen() * inverse),
                Math.round(foreground.getBlue() * alpha + background.getBlue() * inverse));
    }

    private static final class RoundedPanel extends JPanel {
        private static final int ARC = 16;
        private final Color fill;

        private RoundedPanel(LayoutManager layout, Color fill) {
            super(layout);
            this.fill = fill;
            setOpaque(false);
            setBackground(fill);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            } finally {
                g.dispose();
            }
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                super.paintChildren(g);
            } finally {
                g.dispose();
            }
        }
    }

    private record RoundedBorder(Color color, int arc) implements Border {
        @Override
        public void paintBorder(Component component, Graphics graphics,
                int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(color);
                g.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            } finally {
                g.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
