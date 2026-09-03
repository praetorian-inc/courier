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
import burp.controller.LogController;
import burp.model.HttpRequestResponsePair;
import burp.model.PlannerRequestTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class PlannerView {
    private final LogController logger;
    private PlannerController plannerController;
    private JTextField messageInputField;
    private JTable requestsTable;
    private PlannerRequestTableModel tableModel;
    private JButton queryModeButton;
    private JButton agentModeButton;
    private String selectedMode = "query";
    private JTabbedPane conversationTabs;
    private JSplitPane chatSplitPane;
    private PlannerPreviewView previewView;
    private JLabel selectionCountLabel;
    private JPanel attachmentPanel;

    public PlannerView(LogController logger) {
        this.logger = logger;
    }

    public void setPlannerController(PlannerController plannerController) {
        this.plannerController = plannerController;
    }

    public PlannerRequestTableModel getTableModel() {
        return tableModel;
    }

    public List<HttpRequestResponsePair> getSelectedRequests() {
        if (requestsTable == null || tableModel == null) {
            return List.of();
        }
        List<HttpRequestResponsePair> selectedRequests = new ArrayList<>();
        for (int selectedRow : requestsTable.getSelectedRows()) {
            HttpRequestResponsePair request = tableModel.getRequestAt(
                    requestsTable.convertRowIndexToModel(selectedRow));
            if (request != null) {
                selectedRequests.add(request);
            }
        }
        return List.copyOf(selectedRequests);
    }

    public JTextField getMessageInputField() {
        return messageInputField;
    }

    public JTextPane getRequestPreviewArea() {
        return previewView.requestArea();
    }

    public JTextPane getResponsePreviewArea() {
        return previewView.responseArea();
    }

    public void showPreviewPanel() {
        previewView.show();
        chatSplitPane.setDividerLocation(0.52);
    }

    public void selectViewMode(String mode) {
        previewView.selectMode(mode);
    }

    public void resetPreviewCaret() {
        previewView.resetCaret();
    }

    public JPanel createPlannerPanel() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(CourierTheme.background());
        page.setBorder(new EmptyBorder(14, 8, 8, 8));
        page.add(CourierTheme.pageHeader("Guard Planner",
                "Queue evidence, select what matters, and send it to the active conversation.", null),
                BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createRequestsPanel(), createChatWorkspace());
        split.setResizeWeight(0.38);
        split.setDividerLocation(275);
        CourierTheme.configureSplitPane(split);
        page.add(split, BorderLayout.CENTER);
        logger.logInfo("Planner panel created successfully");
        return page;
    }

    private JPanel createRequestsPanel() {
        tableModel = new PlannerRequestTableModel();
        requestsTable = new RequestTable(tableModel);
        requestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        requestsTable.getTableHeader().setReorderingAllowed(false);
        requestsTable.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        CourierTheme.configureTable(requestsTable);
        requestsTable.getColumnModel().getColumn(0).setCellRenderer(new SelectionRadioRenderer());
        requestsTable.getColumnModel().getColumn(2).setCellRenderer(new MethodRenderer());
        requestsTable.getColumnModel().getColumn(3).setCellRenderer(new UrlRenderer());
        requestsTable.getColumnModel().getColumn(6).setCellRenderer(new LeftRenderer());
        requestsTable.getColumnModel().getColumn(7).setCellRenderer(new LeftRenderer());
        applyProportionalColumnWidths(1_200);
        for (int column = 0; column < requestsTable.getColumnCount(); column++) {
            requestsTable.getColumnModel().getColumn(column).setResizable(false);
        }
        requestsTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionState();
                previewLeadSelection();
            }
        });
        addTablePopupMenu();

        JButton clearSelection = new JButton("Clear selection");
        CourierTheme.styleSecondary(clearSelection);
        clearSelection.addActionListener(event -> plannerController.handleRequestDeselection());
        JButton clear = new JButton("Clear queue");
        CourierTheme.styleSecondary(clear);
        clear.addActionListener(event -> plannerController.handleClearRequests());
        selectionCountLabel = CourierTheme.statusPill("0 selected", CourierTheme.muted());
        JPanel trailing = CourierTheme.actionBar(selectionCountLabel, clearSelection, clear);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        JScrollPane requestsScroll = CourierTheme.scroll(requestsTable);
        requestsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        requestsScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                applyProportionalColumnWidths(requestsScroll.getViewport().getWidth());
            }
        });
        body.add(requestsScroll, BorderLayout.CENTER);
        return CourierTheme.card("Request queue", body, trailing);
    }

    private JPanel createChatWorkspace() {
        chatSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        chatSplitPane.setResizeWeight(0.52);
        chatSplitPane.setDividerLocation(0.52);
        CourierTheme.configureSplitPane(chatSplitPane);

        JPanel conversation = CourierTheme.card("Conversations", createConversationPanel(),
                createConversationActions());
        previewView = new PlannerPreviewView(plannerController);
        chatSplitPane.setLeftComponent(conversation);
        chatSplitPane.setRightComponent(previewView.createPanel());

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);
        workspace.add(chatSplitPane, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createConversationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        conversationTabs = new JTabbedPane();
        conversationTabs.setUI(new CourierTabbedPaneUI());
        conversationTabs.setBackground(CourierTheme.surface());
        conversationTabs.setForeground(CourierTheme.text());
        conversationTabs.setFont(CourierTheme.bodyFont(10).deriveFont(java.awt.Font.BOLD));
        ChatTab initial = createChatTab();
        conversationTabs.addTab("Chat 1", initial.panel());
        plannerController.registerChatView(0, initial.chatView());
        panel.add(conversationTabs, BorderLayout.CENTER);
        panel.add(createComposer(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createConversationActions() {
        queryModeButton = new JButton("Query");
        agentModeButton = new JButton("Agent");
        queryModeButton.addActionListener(event -> {
            selectedMode = "query";
            updateModeButtons();
        });
        agentModeButton.addActionListener(event -> {
            selectedMode = "agent";
            updateModeButtons();
        });
        updateModeButtons();
        JButton clear = new JButton("Clear chat");
        CourierTheme.styleSecondary(clear);
        clear.addActionListener(event -> plannerController.handleClearChat());
        JButton create = new JButton("＋ New chat");
        CourierTheme.styleSecondary(create);
        create.addActionListener(event -> plannerController.handleNewConversation());
        return CourierTheme.actionBar(
                CourierTheme.segmentedControl(queryModeButton, agentModeButton), clear, create);
    }

    private void updateModeButtons() {
        CourierTheme.styleSegment(queryModeButton, "query".equals(selectedMode));
        CourierTheme.styleSegment(agentModeButton, "agent".equals(selectedMode));
    }

    private ChatTab createChatTab() {
        PlannerChatView chatView = new PlannerChatView();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(chatView.component(), BorderLayout.CENTER);
        return new ChatTab(panel, chatView);
    }

    private JPanel createComposer() {
        JPanel composer = new JPanel(new BorderLayout(8, 7));
        composer.setBorder(new EmptyBorder(9, 10, 10, 10));
        composer.setBackground(CourierTheme.surface());
        attachmentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        attachmentPanel.setOpaque(false);
        composer.add(attachmentPanel, BorderLayout.NORTH);
        updateAttachmentChips();

        messageInputField = new JTextField();
        messageInputField.setToolTipText("Ask Guard about the selected evidence");
        CourierTheme.styleInput(messageInputField);
        messageInputField.addActionListener(event -> plannerController.handleSendMessage());

        JButton send = CourierTheme.primaryButton("Send");
        send.addActionListener(event -> plannerController.handleSendMessage());
        JPanel actions = CourierTheme.actionBar(send);
        composer.add(messageInputField, BorderLayout.CENTER);
        composer.add(actions, BorderLayout.EAST);
        return composer;
    }

    private void applyProportionalColumnWidths(int availableWidth) {
        double[] proportions = {0.04, 0.09, 0.07, 0.39, 0.12, 0.08, 0.09, 0.12};
        int effectiveWidth = Math.max(480, availableWidth);
        int remaining = effectiveWidth;
        for (int column = 0; column < proportions.length; column++) {
            int width = column == proportions.length - 1
                    ? remaining : (int) Math.round(effectiveWidth * proportions[column]);
            width = Math.max(column == 0 ? 30 : 48, width);
            var tableColumn = requestsTable.getColumnModel().getColumn(column);
            tableColumn.setMinWidth(column == 0 ? 30 : 40);
            tableColumn.setMaxWidth(Integer.MAX_VALUE);
            tableColumn.setPreferredWidth(width);
            tableColumn.setWidth(width);
            remaining -= width;
        }
    }

    private void previewLeadSelection() {
        int leadRow = requestsTable.getSelectionModel().getLeadSelectionIndex();
        if (leadRow < 0 || !requestsTable.isRowSelected(leadRow)) {
            if (requestsTable.getSelectedRowCount() == 0) {
                plannerController.handleRequestSelection(null);
            }
            return;
        }
        HttpRequestResponsePair selected = tableModel.getRequestAt(
                requestsTable.convertRowIndexToModel(leadRow));
        plannerController.handleRequestSelection(selected);
    }

    private void updateSelectionState() {
        int count = requestsTable == null ? 0 : requestsTable.getSelectedRowCount();
        if (selectionCountLabel != null) {
            CourierTheme.setStatus(selectionCountLabel, count + " selected",
                    count == 0 ? CourierTheme.muted() : CourierTheme.ACCENT);
        }
        updateAttachmentChips();
    }

    private void updateAttachmentChips() {
        if (attachmentPanel == null) {
            return;
        }
        attachmentPanel.removeAll();
        List<HttpRequestResponsePair> selected = getSelectedRequests();
        JLabel hint = new JLabel(selected.isEmpty()
                ? "No requests selected for attachment"
                : "Will attach unsent requests to next message:");
        hint.setForeground(CourierTheme.muted());
        hint.setFont(CourierTheme.bodyFont(9));
        attachmentPanel.add(hint);
        for (int index = 0; index < Math.min(3, selected.size()); index++) {
            HttpRequestResponsePair pair = selected.get(index);
            burp.api.montoya.http.message.requests.HttpRequest request = pair.getOriginalRequest();
            String reference = request == null ? "Unknown request"
                    : request.method() + " " + compactPath(request.path());
            attachmentPanel.add(CourierTheme.chip(reference));
        }
        if (selected.size() > 3) {
            attachmentPanel.add(CourierTheme.chip("+" + (selected.size() - 3) + " more"));
        }
        attachmentPanel.revalidate();
        attachmentPanel.repaint();
    }

    private static String compactPath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.length() <= 32 ? path : path.substring(0, 29) + "…";
    }

    private void addTablePopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem remove = new JMenuItem("Remove request");
        remove.addActionListener(event -> {
            int selectedRow = requestsTable.getSelectedRow();
            if (selectedRow < 0) {
                return;
            }
            int result = JOptionPane.showConfirmDialog(requestsTable,
                    "Remove this request from the Planner queue?", "Remove request",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                plannerController.removeRequest(requestsTable.convertRowIndexToModel(selectedRow));
            }
        });
        popup.add(remove);
        requestsTable.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { showPopup(event); }
            @Override public void mouseReleased(MouseEvent event) { showPopup(event); }
            private void showPopup(MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }
                int row = requestsTable.rowAtPoint(event.getPoint());
                if (row >= 0) {
                    if (!requestsTable.isRowSelected(row)) {
                        requestsTable.setRowSelectionInterval(row, row);
                    }
                    popup.show(event.getComponent(), event.getX(), event.getY());
                }
            }
        });
    }

    public void hidePreviewPanel() {
        previewView.hide();
    }

    public void clearPreviewContent() {
        previewView.clear();
    }

    public void clearRequestSelection() {
        requestsTable.clearSelection();
        updateSelectionState();
    }

    public PlannerChatView createNewConversationTab(String tabName) {
        ChatTab chat = createChatTab();
        conversationTabs.addTab(tabName, chat.panel());
        conversationTabs.setSelectedIndex(conversationTabs.getTabCount() - 1);
        return chat.chatView();
    }

    public String getSelectedMode() {
        return selectedMode;
    }

    public int getCurrentTabIndex() {
        return conversationTabs == null ? 0 : conversationTabs.getSelectedIndex();
    }

    public void setCurrentTabIndex(int index) {
        if (conversationTabs != null && index >= 0 && index < conversationTabs.getTabCount()) {
            conversationTabs.setSelectedIndex(index);
        }
    }

    private static final class RequestTable extends JTable {
        private RequestTable(PlannerRequestTableModel model) {
            super(model);
        }

        @Override
        protected void processMouseEvent(MouseEvent event) {
            if (event.getID() == MouseEvent.MOUSE_PRESSED
                    && javax.swing.SwingUtilities.isLeftMouseButton(event)) {
                int row = rowAtPoint(event.getPoint());
                if (row >= 0 && isRowSelected(row)) {
                    clearSelection();
                    event.consume();
                    return;
                }
            }
            super.processMouseEvent(event);
        }
    }

    private static final class SelectionRadioRenderer extends JRadioButton
            implements javax.swing.table.TableCellRenderer {
        private SelectionRadioRenderer() {
            setHorizontalAlignment(CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            setSelected(table.isRowSelected(row));
            setBackground(table.isRowSelected(row)
                    ? CourierTheme.selectionBackground() : CourierTheme.surface());
            return this;
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private HeaderRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            setHorizontalAlignment(LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setBackground(CourierTheme.elevatedSurface());
            setForeground(CourierTheme.muted());
            setFont(CourierTheme.bodyFont(9).deriveFont(java.awt.Font.BOLD));
            return this;
        }
    }

    private static final class UrlRenderer extends DefaultTableCellRenderer {
        private UrlRenderer() {
            setHorizontalAlignment(LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            String fullUrl = value == null ? "" : value.toString();
            setText(ellipsize(fullUrl, table.getColumnModel().getColumn(column).getWidth() - 16,
                    table.getFontMetrics(table.getFont())));
            setToolTipText(fullUrl);
            return this;
        }

        private static String ellipsize(String value, int availableWidth, java.awt.FontMetrics metrics) {
            if (metrics.stringWidth(value) <= availableWidth) {
                return value;
            }
            String suffix = "…";
            int end = value.length();
            while (end > 0 && metrics.stringWidth(value.substring(0, end) + suffix) > availableWidth) {
                end--;
            }
            return value.substring(0, end) + suffix;
        }
    }

    private static final class LeftRenderer extends DefaultTableCellRenderer {
        private LeftRenderer() {
            setHorizontalAlignment(LEFT);
        }
    }

    private static final class MethodRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setForeground(new java.awt.Color(93, 183, 255));
            setFont(CourierTheme.monoFont(10).deriveFont(java.awt.Font.BOLD));
            return this;
        }
    }

    private record ChatTab(JPanel panel, PlannerChatView chatView) {
    }
}
