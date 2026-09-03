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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.net.URI;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class GeneralView {
    static final String REPOSITORY_URL = "https://github.com/praetorian-inc/courier";

    private GeneralView() {
    }

    public static Shell createShell(String buildTimestamp) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CourierTheme.background());
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 2, 10, 2));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        JLabel mark = new JLabel(CourierIcons.mercuryHelmet());
        mark.setToolTipText("Mercury's winged helmet");
        mark.setHorizontalAlignment(JLabel.CENTER);
        mark.setOpaque(true);
        mark.setBackground(CourierTheme.elevatedSurface());
        mark.setBorder(new javax.swing.border.CompoundBorder(
                CourierTheme.cardBorder(), new EmptyBorder(3, 3, 3, 3)));
        JPanel copy = new JPanel(new BorderLayout(0, 1));
        copy.setOpaque(false);
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        ((FlowLayout) titleRow.getLayout()).setAlignOnBaseline(true);
        JLabel title = new JLabel("Courier");
        title.setFont(CourierTheme.titleFont(16));
        JLabel byline = new JLabel("by Praetorian");
        byline.setFont(CourierTheme.bodyFont(10));
        byline.setForeground(CourierTheme.muted());
        byline.setBorder(new EmptyBorder(0, 5, 0, 0));
        titleRow.add(title);
        titleRow.add(byline);
        JLabel subtitle = new JLabel("Guard bridge for Burp Suite");
        subtitle.setFont(CourierTheme.bodyFont(10));
        subtitle.setForeground(CourierTheme.muted());
        copy.add(titleRow, BorderLayout.NORTH);
        copy.add(subtitle, BorderLayout.CENTER);
        brand.add(mark);
        brand.add(Box.createHorizontalStrut(9));
        brand.add(copy);

        JLabel connectionStatus = CourierTheme.statusPill("Disconnected", CourierTheme.muted());
        JLabel build = new JLabel("Build " + buildTimestamp);
        build.setForeground(CourierTheme.muted());
        build.setFont(CourierTheme.bodyFont(10));
        JButton about = new JButton("About");
        CourierTheme.styleSecondary(about);
        about.setFont(CourierTheme.bodyFont(10));
        about.addActionListener(event -> showAboutDialog(root, buildTimestamp));
        header.add(brand, BorderLayout.WEST);
        header.add(CourierTheme.actionBar(build, connectionStatus, about), BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new CourierTabbedPaneUI());
        tabs.setBackground(CourierTheme.background());
        tabs.setForeground(CourierTheme.text());
        tabs.setFont(CourierTheme.bodyFont(11).deriveFont(java.awt.Font.BOLD));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        Dashboard dashboard = new Dashboard();
        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        return new Shell(root, tabs, connectionStatus, dashboard);
    }

    private static void showAboutDialog(java.awt.Component parent, String buildTimestamp) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "About Courier", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setContentPane(createAboutContent(buildTimestamp, dialog::dispose));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    static JPanel createAboutContent(String buildTimestamp, Runnable closeAction) {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(CourierTheme.background());
        content.setBorder(new EmptyBorder(20, 20, 16, 20));
        content.setPreferredSize(new Dimension(560, 420));

        JPanel hero = CourierTheme.roundedPanel(new BorderLayout(18, 0), CourierTheme.surface());
        hero.setBorder(new javax.swing.border.CompoundBorder(
                CourierTheme.cardBorder(), new EmptyBorder(16, 18, 16, 18)));
        JPanel logoFrame = CourierTheme.roundedPanel(new BorderLayout(), CourierTheme.elevatedSurface());
        logoFrame.setBorder(new javax.swing.border.CompoundBorder(
                CourierTheme.roundedBorder(CourierTheme.borderColor(), 18),
                new EmptyBorder(10, 10, 10, 10)));
        logoFrame.add(new JLabel(CourierIcons.mercuryHelmet(72)), BorderLayout.CENTER);
        hero.add(logoFrame, BorderLayout.WEST);

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JPanel aboutTitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        aboutTitleRow.setOpaque(false);
        ((FlowLayout) aboutTitleRow.getLayout()).setAlignOnBaseline(true);
        JLabel aboutTitle = new JLabel("Courier");
        aboutTitle.setFont(CourierTheme.titleFont(24));
        JLabel aboutByline = new JLabel("by Praetorian");
        aboutByline.setFont(CourierTheme.bodyFont(11));
        aboutByline.setForeground(CourierTheme.muted());
        aboutByline.setBorder(new EmptyBorder(0, 7, 0, 0));
        aboutTitleRow.add(aboutTitle);
        aboutTitleRow.add(aboutByline);
        aboutTitleRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel aboutSubtitle = new JLabel("Guard bridge for Burp Suite");
        aboutSubtitle.setFont(CourierTheme.bodyFont(12));
        aboutSubtitle.setForeground(CourierTheme.muted());
        aboutSubtitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        identity.add(Box.createVerticalGlue());
        identity.add(aboutTitleRow);
        identity.add(Box.createVerticalStrut(3));
        identity.add(aboutSubtitle);
        identity.add(Box.createVerticalGlue());
        hero.add(identity, BorderLayout.CENTER);
        content.add(hero, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JTextArea description = aboutText(
                "Courier is a Java 17 extension for Burp Suite built on the Montoya API. "
                        + "It connects Burp to Guard for authorized client workflows, provides "
                        + "Planner chat, and records browser webflows with Playwright.");
        description.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        description.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        body.add(description);
        body.add(Box.createVerticalStrut(12));

        JPanel details = new JPanel(new GridLayout(1, 3, 9, 0));
        details.setOpaque(false);
        details.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        details.add(aboutDetail("Integration", "Burp Suite + Guard"));
        details.add(aboutDetail("Automation", "Playwright webflows"));
        details.add(aboutDetail("License", "Apache 2.0"));
        body.add(details);
        body.add(Box.createVerticalStrut(12));

        JSeparator separator = new JSeparator();
        separator.setForeground(CourierTheme.borderColor());
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        body.add(separator);
        body.add(Box.createVerticalStrut(10));
        JTextArea licensing = aboutText(
                "Copyright Praetorian Security Inc.\n"
                        + "Third-party license notices are included in release JARs under "
                        + "META-INF/third-party-licenses/.");
        licensing.setFont(CourierTheme.bodyFont(9));
        licensing.setForeground(CourierTheme.muted());
        licensing.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        licensing.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        body.add(licensing);
        content.add(body, BorderLayout.CENTER);

        JLabel build = new JLabel("Build " + buildTimestamp);
        build.setFont(CourierTheme.monoFont(9));
        build.setForeground(CourierTheme.muted());
        JButton repository = new JButton("GitHub repository ↗");
        CourierTheme.styleSecondary(repository);
        repository.setToolTipText(REPOSITORY_URL);
        repository.addActionListener(event -> openRepository(content));
        JButton close = new JButton("Close");
        CourierTheme.styleSecondary(close);
        close.addActionListener(event -> closeAction.run());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(build, BorderLayout.WEST);
        footer.add(CourierTheme.actionBar(repository, close), BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    private static void openRepository(java.awt.Component parent) {
        try {
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new UnsupportedOperationException("Browser integration is unavailable");
            }
            Desktop.getDesktop().browse(URI.create(REPOSITORY_URL));
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(parent, REPOSITORY_URL,
                    "Courier repository", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static JTextArea aboutText(String text) {
        JTextArea content = new JTextArea(text);
        content.setEditable(false);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        content.setOpaque(false);
        content.setForeground(CourierTheme.text());
        content.setFont(CourierTheme.bodyFont(10));
        content.setBorder(null);
        return content;
    }

    private static JPanel aboutDetail(String title, String value) {
        JPanel card = CourierTheme.roundedPanel(new BorderLayout(0, 4), CourierTheme.elevatedSurface());
        card.setBorder(new javax.swing.border.CompoundBorder(
                CourierTheme.roundedBorder(CourierTheme.borderColor(), 12),
                new EmptyBorder(10, 11, 10, 11)));
        JLabel titleLabel = new JLabel(title.toUpperCase(Locale.ROOT));
        titleLabel.setFont(CourierTheme.bodyFont(8).deriveFont(java.awt.Font.BOLD));
        titleLabel.setForeground(CourierTheme.muted());
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(CourierTheme.bodyFont(10).deriveFont(java.awt.Font.BOLD));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public static JPanel createControlPanel(Shell shell,
            JPanel connectionPanel, JPanel optionsPanel, JPanel loggerPanel) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(CourierTheme.background());
        page.setBorder(new EmptyBorder(14, 8, 8, 8));
        page.add(CourierTheme.pageHeader("Control center",
                "Connection, capture policy, and operational activity.", null), BorderLayout.NORTH);

        JSplitPane settings = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, connectionPanel, optionsPanel);
        settings.setResizeWeight(0.66);
        settings.setDividerLocation(0.66);
        settings.setPreferredSize(new Dimension(900, 375));
        CourierTheme.configureSplitPane(settings);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, settings, loggerPanel);
        vertical.setResizeWeight(0.52);
        vertical.setDividerLocation(375);
        CourierTheme.configureSplitPane(vertical);

        JPanel content = new JPanel(new BorderLayout(0, CourierTheme.GAP));
        content.setOpaque(false);
        content.add(shell.dashboard().panel(), BorderLayout.NORTH);
        content.add(vertical, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    public record Shell(JPanel root, JTabbedPane tabs, JLabel connectionStatus, Dashboard dashboard) {
        public void setConnectionStatus(String state) {
            Color color = statusColor(state);
            CourierTheme.setStatus(connectionStatus, state, color);
            dashboard.setConnectionState(state);
        }

        public void close() {
            dashboard.close();
        }

        private static Color statusColor(String state) {
            return switch (state) {
                case "Connected" -> CourierTheme.SUCCESS;
                case "Connecting" -> CourierTheme.WARNING;
                default -> CourierTheme.muted();
            };
        }
    }

    public static final class Dashboard implements AutoCloseable {
        private final JPanel panel = new JPanel(new GridLayout(1, 4, CourierTheme.GAP, 0));
        private final JLabel connectionValue = value("Disconnected");
        private final JLabel connectionMeta = meta("Guard session inactive");
        private final JLabel captureValue = value("All traffic");
        private final JLabel captureMeta = meta("Scope filtering disabled");
        private final JLabel pendingValue = value("0");
        private final JLabel pendingMeta = meta("Proxy · HTTP · Issues");
        private final JLabel lastSyncValue = value("Never");
        private final JLabel lastSyncMeta = meta("Waiting for first upload");
        private final Timer refreshTimer;
        private IntSupplier pendingSupplier = () -> 0;
        private Supplier<String> lastSyncSupplier = () -> "Never";

        private Dashboard() {
            panel.setOpaque(false);
            panel.add(metric("Guard connection", connectionValue, connectionMeta));
            panel.add(metric("Capture policy", captureValue, captureMeta));
            panel.add(metric("Pending records", pendingValue, pendingMeta));
            panel.add(metric("Last upload", lastSyncValue, lastSyncMeta));
            refreshTimer = new Timer(1000, event -> refresh());
            refreshTimer.start();
        }

        JPanel panel() {
            return panel;
        }

        public void bindMetrics(IntSupplier pending, Supplier<String> lastSync) {
            pendingSupplier = pending == null ? () -> 0 : pending;
            lastSyncSupplier = lastSync == null ? () -> "Never" : lastSync;
            refresh();
        }

        public void setConnectionState(String state) {
            connectionValue.setText(state);
            connectionValue.setForeground("Connected".equals(state)
                    ? CourierTheme.SUCCESS : "Connecting".equals(state)
                    ? CourierTheme.WARNING : CourierTheme.text());
            connectionMeta.setText("Connected".equals(state)
                    ? "Guard session active" : "Guard session inactive");
        }

        public void setCapturePolicy(boolean inScope) {
            captureValue.setText(inScope ? "In scope" : "All traffic");
            captureMeta.setText(inScope ? "Burp target scope enabled" : "Scope filtering disabled");
        }

        private void refresh() {
            pendingValue.setText(Integer.toString(Math.max(0, pendingSupplier.getAsInt())));
            String description = lastSyncSupplier.get();
            lastSyncValue.setText(description == null ? "Never" : description);
            lastSyncMeta.setText("Never".equals(description)
                    ? "Waiting for first upload" : "Latest successful batch");
        }

        @Override
        public void close() {
            refreshTimer.stop();
        }

        private static JPanel metric(String title, JLabel value, JLabel detail) {
            JPanel card = CourierTheme.roundedPanel(null, CourierTheme.surface());
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new javax.swing.border.CompoundBorder(
                    CourierTheme.cardBorder(), new EmptyBorder(10, 12, 10, 12)));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            JLabel titleLabel = new JLabel(title.toUpperCase(Locale.ROOT));
            titleLabel.setForeground(CourierTheme.muted());
            titleLabel.setFont(CourierTheme.bodyFont(9).deriveFont(java.awt.Font.BOLD));
            card.add(titleLabel);
            card.add(Box.createVerticalStrut(4));
            card.add(value);
            card.add(Box.createVerticalStrut(2));
            card.add(detail);
            return card;
        }

        private static JLabel value(String text) {
            JLabel label = new JLabel(text);
            label.setFont(CourierTheme.titleFont(16));
            return label;
        }

        private static JLabel meta(String text) {
            JLabel label = new JLabel(text);
            label.setForeground(CourierTheme.muted());
            label.setFont(CourierTheme.bodyFont(9));
            return label;
        }
    }
}
