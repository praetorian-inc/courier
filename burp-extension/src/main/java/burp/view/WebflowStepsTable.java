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

import burp.model.Webflow;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

final class WebflowStepsTable {
    private static final double[] COLUMN_PROPORTIONS = {0.05, 0.12, 0.43, 0.30, 0.10};
    private final JTable table;
    private final StepTableModel model;
    private final JScrollPane scrollPane;
    private int hoverRow = -1;
    private int copiedRow = -1;
    private Timer copyFeedbackTimer;

    WebflowStepsTable(DefaultListModel<Webflow.WebflowStep> steps) {
        model = new StepTableModel(steps);
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.setRowHeight(38);
        CourierTheme.configureTable(table);
        table.getColumnModel().getColumn(0).setCellRenderer(new MutedRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new DescriptionRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new SelectorRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new CallsRenderer());
        installSelectorCopyInteraction();
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setResizable(false);
        }
        applyWidths(1_000);
        scrollPane = CourierTheme.scroll(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                applyWidths(scrollPane.getViewport().getWidth());
            }
        });
    }

    private void installSelectorCopyInteraction() {
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                int previousRow = hoverRow;
                hoverRow = column == 3 ? row : -1;
                table.setCursor(isCopyIconHit(event)
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                if (previousRow != hoverRow) {
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hoverRow = -1;
                table.setCursor(Cursor.getDefaultCursor());
                table.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (!isCopyIconHit(event)) {
                    return;
                }
                int row = table.rowAtPoint(event.getPoint());
                String selector = model.getValueAt(row, 3).toString();
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(selector), null);
                    showCopiedFeedback(row);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void showCopiedFeedback(int row) {
        copiedRow = row;
        table.repaint(table.getCellRect(row, 3, true));
        if (copyFeedbackTimer != null) {
            copyFeedbackTimer.stop();
        }
        copyFeedbackTimer = new Timer(900, event -> {
            int previousRow = copiedRow;
            copiedRow = -1;
            if (previousRow >= 0) {
                table.repaint(table.getCellRect(previousRow, 3, true));
            }
        });
        copyFeedbackTimer.setRepeats(false);
        copyFeedbackTimer.start();
    }

    private boolean isCopyIconHit(MouseEvent event) {
        int row = table.rowAtPoint(event.getPoint());
        int column = table.columnAtPoint(event.getPoint());
        if (row < 0 || column != 3) {
            return false;
        }
        java.awt.Rectangle cell = table.getCellRect(row, column, true);
        return event.getX() >= cell.x + cell.width - 30;
    }

    JScrollPane component() {
        return scrollPane;
    }

    private void applyWidths(int availableWidth) {
        int effectiveWidth = Math.max(600, availableWidth);
        int remaining = effectiveWidth;
        for (int column = 0; column < COLUMN_PROPORTIONS.length; column++) {
            int width = column == COLUMN_PROPORTIONS.length - 1
                    ? remaining : (int) Math.round(effectiveWidth * COLUMN_PROPORTIONS[column]);
            width = Math.max(45, width);
            var tableColumn = table.getColumnModel().getColumn(column);
            tableColumn.setMinWidth(35);
            tableColumn.setMaxWidth(Integer.MAX_VALUE);
            tableColumn.setPreferredWidth(width);
            tableColumn.setWidth(width);
            remaining -= width;
        }
    }

    private static final class StepTableModel extends AbstractTableModel implements ListDataListener {
        private static final String[] COLUMNS = {"#", "Action", "Description", "Selector", "Calls"};
        private final DefaultListModel<Webflow.WebflowStep> steps;

        private StepTableModel(DefaultListModel<Webflow.WebflowStep> steps) {
            this.steps = steps;
            steps.addListDataListener(this);
        }

        @Override public int getRowCount() { return steps.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public boolean isCellEditable(int row, int column) { return false; }

        @Override
        public Object getValueAt(int row, int column) {
            Webflow.WebflowStep step = steps.get(row);
            return switch (column) {
                case 0 -> String.format("%02d", step.getOrder());
                case 1 -> step.getAction() == null ? "" : step.getAction().toUpperCase();
                case 2 -> step.getDescription() == null ? "" : step.getDescription();
                case 3 -> step.getSelector() == null ? "" : step.getSelector();
                case 4 -> step.getCorrelatedRequests() == null ? 0 : step.getCorrelatedRequests().size();
                default -> "";
            };
        }

        @Override public void intervalAdded(ListDataEvent event) { fireTableDataChanged(); }
        @Override public void intervalRemoved(ListDataEvent event) { fireTableDataChanged(); }
        @Override public void contentsChanged(ListDataEvent event) { fireTableDataChanged(); }
    }

    private static class LeftRenderer extends DefaultTableCellRenderer {
        private LeftRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 8, 0, 8));
        }
    }

    private static final class MutedRenderer extends LeftRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setForeground(CourierTheme.muted());
            setFont(CourierTheme.monoFont(9));
            return this;
        }
    }

    private static final class ActionRenderer extends LeftRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setForeground(CourierTheme.ACCENT);
            setFont(CourierTheme.bodyFont(9).deriveFont(java.awt.Font.BOLD));
            return this;
        }
    }

    private static final class DescriptionRenderer extends LeftRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            String fullDescription = value == null ? "" : value.toString();
            setText(shortenDescription(fullDescription));
            setToolTipText(fullDescription);
            setFont(CourierTheme.bodyFont(9));
            return this;
        }

        private static String shortenDescription(String value) {
            return value.length() <= 64 ? value : value.substring(0, 64) + "...";
        }
    }

    private final class SelectorRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final JLabel text = new JLabel();
        private final JLabel copy = new JLabel(CourierIcons.copy());

        private SelectorRenderer() {
            super(new BorderLayout(6, 0));
            setBorder(new EmptyBorder(0, 8, 0, 7));
            text.setForeground(new Color(111, 174, 232));
            text.setFont(CourierTheme.monoFont(8));
            copy.setToolTipText("Copy selector");
            add(text, BorderLayout.CENTER);
            add(copy, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            String selector = value == null ? "" : value.toString();
            text.setText(selector.length() <= 100 ? selector : selector.substring(0, 99) + "…");
            text.setToolTipText(selector);
            boolean copied = row == copiedRow;
            copy.setIcon(copied ? null : CourierIcons.copy());
            copy.setText(copied ? "Copied!" : "");
            copy.setForeground(copied ? CourierTheme.SUCCESS : CourierTheme.muted());
            copy.setVisible((copied || row == hoverRow) && !selector.isEmpty());
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    private static final class CallsRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final JLabel pill = new JLabel();

        private CallsRenderer() {
            super(new BorderLayout());
            setOpaque(true);
            pill.setPreferredSize(new Dimension(96, 24));
            pill.setMinimumSize(new Dimension(96, 24));
            pill.setMaximumSize(new Dimension(96, 24));
            pill.setHorizontalAlignment(SwingConstants.LEFT);
            add(pill, BorderLayout.WEST);
            setBorder(new EmptyBorder(0, 7, 0, 7));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            int calls = value instanceof Number number ? number.intValue() : 0;
            CourierTheme.setStatus(pill, calls + " calls",
                    calls > 0 ? CourierTheme.SUCCESS : CourierTheme.muted());
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    private static final class HeaderRenderer extends LeftRenderer {
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
}
