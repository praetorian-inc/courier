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

import burp.controller.LogController;
import burp.controller.WebflowRecorderController;
import burp.model.Webflow;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

public class WebflowRecorderView {
    private static final String PRETTY_STEPS_CARD = "pretty-steps";
    private static final String JSON_STEPS_CARD = "json-steps";
    private final LogController logger;
    private WebflowRecorderController controller;
    private JList<Webflow> webflowList;
    private JLabel statusLabel;
    private JLabel webflowCountLabel;
    private JLabel webflowNameLabel;
    private JLabel webflowDescriptionLabel;
    private JLabel projectNameLabel;
    private JLabel browserPathLabel;
    private JLabel createdAtLabel;
    private JLabel lastModifiedLabel;
    private WebflowStepsTable stepsTable;
    private JTextArea jsonArea;
    private JScrollPane jsonScroll;
    private CardLayout stepsCardLayout;
    private JPanel stepsCards;
    private JButton prettyButton;
    private JButton jsonButton;
    private boolean jsonLoading;

    public WebflowRecorderView(LogController logger) {
        this.logger = logger;
    }

    public void setWebflowRecorderController(WebflowRecorderController controller) {
        this.controller = controller;
    }

    public void showWebflow(WebflowRecorderController.WebflowDisplayData data) {
        webflowNameLabel.setText(data.getName());
        webflowDescriptionLabel.setText(stripPrefix(data.getDescription(), "Description: "));
        projectNameLabel.setText(stripPrefix(data.getProjectName(), "Project: "));
        browserPathLabel.setText(stripPrefix(data.getBrowserPath(), "Browser: "));
        createdAtLabel.setText(stripPrefix(data.getCreatedAt(), "Created: "));
        lastModifiedLabel.setText(stripPrefix(data.getLastModified(), "Modified: "));
    }

    public void showMultipleSelection() {
        webflowNameLabel.setText("Multiple webflows selected");
        webflowDescriptionLabel.setText("Select one webflow to inspect its metadata and steps.");
        projectNameLabel.setText("—");
        browserPathLabel.setText("—");
        createdAtLabel.setText("—");
        lastModifiedLabel.setText("—");
    }

    public void selectViewMode(String mode) {
        CourierTheme.styleSegment(prettyButton, "Pretty".equals(mode));
        CourierTheme.styleSegment(jsonButton, "JSON".equals(mode));
    }

    public void setJsonLoading(boolean loading) {
        jsonLoading = loading;
        if (jsonButton != null) {
            jsonButton.setEnabled(!loading);
            jsonButton.setText(loading ? "Loading..." : "JSON");
        }
        if (statusLabel != null) {
            setStatus(loading ? "Loading JSON" : "Recorder ready",
                    loading ? CourierTheme.WARNING : CourierTheme.SUCCESS);
        }
        if (loading && jsonArea != null) {
            replaceJsonArea("Loading...");
            stepsCardLayout.show(stepsCards, JSON_STEPS_CARD);
        }
    }

    public void showJsonText(String json) {
        replaceJsonArea(json == null ? "" : json);
        stepsCardLayout.show(stepsCards, JSON_STEPS_CARD);
    }

    public void cancelJsonRendering() {
        // JSON creation is cancelled by the controller generation; no UI task is retained.
    }

    private void replaceJsonArea(String content) {
        JTextArea replacement = createJsonArea();
        replacement.setText(content);
        replacement.setCaretPosition(0);
        jsonArea = replacement;
        if (jsonScroll != null) {
            jsonScroll.setViewportView(jsonArea);
        }
    }

    public void showPrettySteps() {
        if (stepsCardLayout != null && stepsCards != null) {
            stepsCardLayout.show(stepsCards, PRETTY_STEPS_CARD);
        }
    }

    public JPanel createWebflowRecorderPanel() {
        initializeComponents();
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(CourierTheme.background());
        page.setBorder(new EmptyBorder(14, 8, 8, 8));
        JButton clear = new JButton("Clear all");
        CourierTheme.styleSecondary(clear);
        clear.addActionListener(event -> controller.handleClearAllWebflows());
        JButton record = CourierTheme.primaryButton("＋ Record webflow");
        record.addActionListener(event -> controller.showRecordModal(record));
        statusLabel = CourierTheme.statusPill("Recorder ready", CourierTheme.SUCCESS);
        page.add(CourierTheme.pageHeader("Webflows",
                "Record repeatable browser workflows and correlate actions with network evidence.",
                CourierTheme.actionBar(statusLabel, clear, record)), BorderLayout.NORTH);
        page.add(createWorkspace(), BorderLayout.CENTER);
        setupController();
        logger.logInfo("Webflow Recorder panel created successfully");
        return page;
    }

    private void initializeComponents() {
        webflowList = new JList<>(controller.getWebflowListModel());
        webflowList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        webflowList.setCellRenderer(new WebflowListCellRenderer());
        webflowList.setBackground(CourierTheme.surface());
        webflowList.setSelectionBackground(CourierTheme.selectionBackground());
        webflowList.setSelectionForeground(CourierTheme.text());
        webflowList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                controller.handleWebflowSelectionChange(webflowList.getSelectedIndices());
            }
        });
        controller.getWebflowListModel().addListDataListener(new javax.swing.event.ListDataListener() {
            @Override public void intervalAdded(javax.swing.event.ListDataEvent event) { updateWebflowCount(); }
            @Override public void intervalRemoved(javax.swing.event.ListDataEvent event) { updateWebflowCount(); }
            @Override public void contentsChanged(javax.swing.event.ListDataEvent event) { updateWebflowCount(); }
        });
        addWebflowListContextMenu();

        stepsTable = new WebflowStepsTable(controller.getStepsModel());

        jsonArea = createJsonArea();

        prettyButton = new JButton("Pretty");
        jsonButton = new JButton("JSON");
        prettyButton.addActionListener(event -> controller.handleViewModeChange("Pretty"));
        jsonButton.addActionListener(event -> controller.handleViewModeChange("JSON"));
    }

    private JPanel createWorkspace() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createWebflowNavigator(), createWebflowContent());
        split.setResizeWeight(0.25);
        split.setDividerLocation(285);
        CourierTheme.configureSplitPane(split);
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);
        workspace.add(split, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createWebflowNavigator() {
        webflowCountLabel = CourierTheme.statusPill(
                Integer.toString(controller.getWebflowListModel().size()), CourierTheme.muted());
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(CourierTheme.scroll(webflowList), BorderLayout.CENTER);
        return CourierTheme.card("Recorded webflows", body, webflowCountLabel);
    }

    private JPanel createWebflowContent() {
        JPanel content = new JPanel(new BorderLayout(0, CourierTheme.GAP));
        content.setOpaque(false);
        content.add(createMetadataStrip(), BorderLayout.NORTH);

        stepsCardLayout = new CardLayout();
        stepsCards = new JPanel(stepsCardLayout);
        stepsCards.add(stepsTable.component(), PRETTY_STEPS_CARD);
        jsonScroll = CourierTheme.scroll(jsonArea);
        stepsCards.add(jsonScroll, JSON_STEPS_CARD);
        JPanel modes = CourierTheme.segmentedControl(prettyButton, jsonButton);
        content.add(CourierTheme.card("Recorded steps", stepsCards, modes), BorderLayout.CENTER);
        return content;
    }

    private JPanel createMetadataStrip() {
        JPanel metadata = CourierTheme.roundedPanel(
                new GridLayout(1, 4, 1, 1), CourierTheme.borderColor());
        webflowNameLabel = valueLabel("Select a webflow");
        webflowDescriptionLabel = valueLabel("Choose a recording from the left.");
        webflowDescriptionLabel.setForeground(CourierTheme.muted());
        webflowDescriptionLabel.setFont(CourierTheme.bodyFont(9));
        projectNameLabel = valueLabel("—");
        browserPathLabel = valueLabel("—");
        createdAtLabel = valueLabel("—");
        lastModifiedLabel = valueLabel("—");
        metadata.add(metadataCell("Webflow", webflowNameLabel, webflowDescriptionLabel));
        metadata.add(metadataCell("Project", projectNameLabel));
        metadata.add(metadataCell("Browser", browserPathLabel));
        metadata.add(metadataCell("Last modified", lastModifiedLabel));
        metadata.setBorder(CourierTheme.cardBorder());
        return metadata;
    }

    private JPanel metadataCell(String title, JLabel... values) {
        JPanel cell = new JPanel();
        cell.setBackground(CourierTheme.surface());
        cell.setBorder(new EmptyBorder(8, 10, 8, 10));
        cell.setLayout(new javax.swing.BoxLayout(cell, javax.swing.BoxLayout.Y_AXIS));
        JLabel label = new JLabel(title.toUpperCase());
        label.setForeground(CourierTheme.muted());
        label.setFont(CourierTheme.bodyFont(9).deriveFont(Font.BOLD));
        cell.add(label);
        cell.add(javax.swing.Box.createVerticalStrut(3));
        for (JLabel value : values) {
            cell.add(value);
        }
        return cell;
    }

    private JLabel valueLabel(String value) {
        JLabel label = new JLabel(value);
        label.setFont(CourierTheme.bodyFont(11));
        return label;
    }

    private JTextArea createJsonArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        CourierTheme.configureTextArea(area, true);
        area.setBackground(CourierTheme.codeBackground());
        return area;
    }

    private void addWebflowListContextMenu() {
        JPopupMenu menu = controller.createWebflowContextMenu(webflowList);
        webflowList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent event) { show(event); }
            @Override public void mouseReleased(java.awt.event.MouseEvent event) { show(event); }
            private void show(java.awt.event.MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }
                int index = webflowList.locationToIndex(event.getPoint());
                controller.handleContextMenuTrigger(webflowList, index, menu, event.getX(), event.getY());
            }
        });
    }

    private void setupController() {
        controller.setStatusUpdateCallback(message -> {
            if (!jsonLoading) {
                setStatus(message, message.toLowerCase().contains("error")
                        ? CourierTheme.WARNING : CourierTheme.SUCCESS);
            }
        });
        controller.setWebflowSelectionCallback(webflow -> {
            if (webflow != null) {
                webflowList.setSelectedValue(webflow, true);
            }
        });
        controller.updateViewModeButtons("Pretty");
    }

    private void setStatus(String text, java.awt.Color color) {
        CourierTheme.setStatus(statusLabel, text, color);
    }

    private void updateWebflowCount() {
        if (webflowCountLabel != null) {
            CourierTheme.setStatus(webflowCountLabel,
                    Integer.toString(controller.getWebflowListModel().size()), CourierTheme.muted());
        }
    }

    private static String stripPrefix(String value, String prefix) {
        return value != null && value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static class WebflowListCellRenderer implements ListCellRenderer<Webflow> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Webflow> list, Webflow webflow,
                int index, boolean selected, boolean focused) {
            JPanel row = CourierTheme.roundedPanel(null,
                    selected ? CourierTheme.selectionBackground() : CourierTheme.surface());
            row.setLayout(new javax.swing.BoxLayout(row, javax.swing.BoxLayout.Y_AXIS));
            row.setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.LineBorder(selected
                            ? CourierTheme.ACCENT : CourierTheme.surface(), 1, true),
                    new EmptyBorder(8, 9, 8, 9)));
            JLabel name = new JLabel(webflow == null ? "Unnamed webflow" : webflow.getName());
            name.setFont(CourierTheme.bodyFont(11).deriveFont(Font.BOLD));
            name.setForeground(CourierTheme.text());
            String project = webflow == null || webflow.getProjectName() == null
                    ? "No project" : webflow.getProjectName();
            int stepCount = webflow == null || webflow.getSteps() == null ? 0 : webflow.getSteps().size();
            JLabel metadata = new JLabel(project + " · " + stepCount + " steps");
            metadata.setFont(CourierTheme.bodyFont(9));
            metadata.setForeground(CourierTheme.muted());
            row.add(name);
            row.add(javax.swing.Box.createVerticalStrut(3));
            row.add(metadata);
            return row;
        }
    }
}
