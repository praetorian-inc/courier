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
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.*;

class PlannerChatViewTest {
    @Test
    void collapsesLargeMarkdownMessagesAndNeverScrollsHorizontally() throws Exception {
        PlannerChatView chat = new PlannerChatView();
        String content = "## Large response\n\n" + "content ".repeat(300);

        SwingUtilities.invokeAndWait(() ->
                chat.append("Guard", content, ChatMessage.MessageType.CHARIOT, "12:00:00"));

        JScrollPane scroll = chat.component();
        assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                scroll.getHorizontalScrollBarPolicy());
        JButton expand = findButton(scroll, "Expand");
        JEditorPane editor = findEditor(scroll);
        assertNotNull(expand);
        assertNotNull(editor);
        int collapsedLength = editor.getText().length();
        SwingUtilities.invokeAndWait(expand::doClick);
        assertEquals("Collapse", expand.getText());
        assertTrue(editor.getText().length() > collapsedLength);
        JScrollPane messageScroll = findEditorScroll(scroll);
        assertNotNull(messageScroll);
        assertEquals(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                messageScroll.getVerticalScrollBarPolicy());
    }

    @Test
    void showsAndResolvesGuardPendingSpinner() throws Exception {
        PlannerChatView chat = new PlannerChatView();

        SwingUtilities.invokeAndWait(chat::showGuardPending);
        assertNotNull(findLabel(chat.component(), "Guard is working"));
        SwingUtilities.invokeAndWait(chat::resolveGuardPending);
        assertNull(findLabel(chat.component(), "Guard is working"));
    }

    private static JLabel findLabel(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel label && label.getText().contains(text)) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel label = findLabel(child, text);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    private static JScrollPane findEditorScroll(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JScrollPane scroll
                    && scroll.getViewport().getView() instanceof JEditorPane) {
                return scroll;
            }
            if (component instanceof Container child) {
                JScrollPane scroll = findEditorScroll(child);
                if (scroll != null) {
                    return scroll;
                }
            }
        }
        return null;
    }

    private static JEditorPane findEditor(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JEditorPane editor) {
                return editor;
            }
            if (component instanceof Container child) {
                JEditorPane editor = findEditor(child);
                if (editor != null) {
                    return editor;
                }
            }
        }
        return null;
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
}
