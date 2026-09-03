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
import burp.controller.PlannerController;
import burp.model.HttpRequestResponsePair;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlannerViewTest {
    @Test
    void presentsSingleRequestSelectionAsRadioChoice() throws Exception {
        LogController logger = mock(LogController.class);
        PlannerView view = new PlannerView(logger);
        PlannerController controller = new PlannerController(view, logger);
        view.setPlannerController(controller);
        Container panel = view.createPlannerPanel();
        HttpRequestResponsePair first = new HttpRequestResponsePair();
        HttpRequestResponsePair second = new HttpRequestResponsePair();
        HttpRequestResponsePair third = new HttpRequestResponsePair();
        view.getTableModel().addRequest(first);
        view.getTableModel().addRequest(second);
        view.getTableModel().addRequest(third);
        JTable table = findTable(panel);
        assertNotNull(table);

        table.setRowSelectionInterval(0, 0);
        table.addRowSelectionInterval(2, 2);

        assertEquals(ListSelectionModel.SINGLE_SELECTION,
                table.getSelectionModel().getSelectionMode());
        assertEquals(String.class, table.getColumnClass(0));
        Component selectionIndicator = table.getCellRenderer(2, 0)
                .getTableCellRendererComponent(table, "", true, false, 2, 0);
        JRadioButton radio = assertInstanceOf(JRadioButton.class, selectionIndicator);
        assertTrue(radio.isSelected());
        java.awt.Rectangle selectedRowCell = table.getCellRect(2, 3, true);
        java.awt.event.MouseEvent clickSelectedRow = new java.awt.event.MouseEvent(
                table, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
                selectedRowCell.x + 1, selectedRowCell.y + 1, 1, false,
                java.awt.event.MouseEvent.BUTTON1);
        javax.swing.SwingUtilities.invokeAndWait(() -> table.dispatchEvent(clickSelectedRow));
        assertTrue(view.getSelectedRequests().isEmpty());

        table.setRowSelectionInterval(2, 2);
        for (int column = 0; column < table.getColumnCount(); column++) {
            assertFalse(table.getColumnModel().getColumn(column).getResizable());
        }
        assertTrue(table.getColumnModel().getColumn(3).getWidth()
                > table.getColumnModel().getColumn(2).getWidth());
        assertTrue(table.getColumnModel().getColumn(6).getWidth()
                < table.getColumnModel().getColumn(3).getWidth());
        assertEquals(java.util.List.of(third), view.getSelectedRequests());
        JButton clearSelection = findButton(panel, "Clear selection");
        assertNotNull(clearSelection);
        clearSelection.doClick();
        assertTrue(view.getSelectedRequests().isEmpty());
        JButton agent = findButton(panel, "Agent");
        assertNotNull(agent);
        agent.doClick();
        assertEquals("agent", view.getSelectedMode());
        assertTrue(view.getRequestPreviewArea().getScrollableTracksViewportWidth());
        JButton raw = findButton(panel, "Raw");
        assertNotNull(raw);
        raw.doClick();
        assertFalse(view.getRequestPreviewArea().getScrollableTracksViewportWidth());
        controller.close();
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton button = findButton(child, text);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static JTable findTable(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JTable table) {
                return table;
            }
            if (component instanceof Container child) {
                JTable table = findTable(child);
                if (table != null) {
                    return table;
                }
            }
        }
        return null;
    }
}
