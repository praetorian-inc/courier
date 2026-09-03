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
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WebflowRecorderViewTest {
    @Test
    void loadingStateDisablesAndRenamesJsonButton() {
        LogController logger = mock(LogController.class);
        WebflowRecorderView view = new WebflowRecorderView(logger);
        WebflowRecorderController controller = new WebflowRecorderController(view, logger);
        view.setWebflowRecorderController(controller);
        JPanel panel = view.createWebflowRecorderPanel();

        view.setJsonLoading(true);

        JButton loadingButton = find(panel, JButton.class, button -> "Loading...".equals(button.getText()));
        JLabel loadingLabel = find(panel, JLabel.class,
                label -> label.getText() != null && label.getText().contains("Loading"));
        JTextArea loadingArea = find(panel, JTextArea.class,
                area -> "Loading...".equals(area.getText()));
        assertNotNull(loadingButton);
        assertFalse(loadingButton.isEnabled());
        assertNotNull(loadingLabel);
        assertNotNull(loadingArea);

        view.setJsonLoading(false);
        assertEquals("JSON", loadingButton.getText());
        assertTrue(loadingButton.isEnabled());
        controller.close();
    }

    @Test
    void jsonViewUsesReadOnlySelectablePrettyPrintedText() {
        LogController logger = mock(LogController.class);
        WebflowRecorderView view = new WebflowRecorderView(logger);
        WebflowRecorderController controller = new WebflowRecorderController(view, logger);
        view.setWebflowRecorderController(controller);
        JPanel panel = view.createWebflowRecorderPanel();
        view.showJsonText("{\n  \"name\": \"example\"\n}");

        JTextArea jsonArea = find(panel, JTextArea.class,
                area -> area.getText().contains("\"name\""));
        assertNotNull(jsonArea);
        assertFalse(jsonArea.isEditable());
        jsonArea.select(4, 10);
        assertNotNull(jsonArea.getSelectedText());
        assertTrue(jsonArea.getText().startsWith("{\n"));
        assertTrue(jsonArea.getText().contains("\n  \"name\""));

        controller.close();
    }

    private static <T extends JComponent> T find(Container root, Class<T> type,
            java.util.function.Predicate<T> predicate) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                T match = type.cast(component);
                if (predicate.test(match)) {
                    return match;
                }
            }
            if (component instanceof Container child) {
                T match = find(child, type, predicate);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
