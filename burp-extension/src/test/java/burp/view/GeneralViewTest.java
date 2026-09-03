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

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.*;

class GeneralViewTest {
    @Test
    void topBarShowsPraetorianBylineAndAboutButton() {
        GeneralView.Shell shell = GeneralView.createShell("test-build");

        JLabel byline = findLabel(shell.root(), "by Praetorian");
        JLabel subtitle = findLabel(shell.root(), "Guard bridge for Burp Suite");
        JButton about = findButton(shell.root(), "About");

        assertNotNull(byline);
        assertNotNull(subtitle);
        assertEquals(subtitle.getFont().getSize2D(), byline.getFont().getSize2D());
        assertSame(byline.getParent().getParent(), subtitle.getParent());
        assertNotNull(about);
        assertTrue(about.getActionListeners().length > 0);
        shell.close();
    }

    @Test
    void aboutContentUsesLogoAndProjectFacts() {
        JPanel about = GeneralView.createAboutContent("test-build", () -> { });
        String content = collectText(about);
        JLabel logo = findIconLabel(about, 72);

        assertNotNull(logo);
        assertEquals(72, logo.getIcon().getIconHeight());
        assertTrue(content.contains("Courier"));
        assertTrue(content.contains("by Praetorian"));
        assertTrue(content.contains("Java 17 extension for Burp Suite"));
        assertTrue(content.contains("Apache 2.0"));
        assertTrue(content.contains("Copyright Praetorian Security Inc."));
        assertTrue(content.contains("Build test-build"));
        JButton repository = findButton(about, "GitHub repository ↗");
        assertNotNull(repository);
        assertEquals(GeneralView.REPOSITORY_URL, repository.getToolTipText());
        assertTrue(repository.getActionListeners().length > 0);
        assertNotNull(findButton(about, "Close"));
    }

    private static JLabel findLabel(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel result = findLabel(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static JLabel findIconLabel(Container parent, int width) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label && label.getIcon() != null
                    && label.getIcon().getIconWidth() == width) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel result = findIconLabel(child, width);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static String collectText(Container parent) {
        StringBuilder text = new StringBuilder();
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label) {
                text.append(label.getText()).append('\n');
            } else if (component instanceof JTextArea textArea) {
                text.append(textArea.getText()).append('\n');
            }
            if (component instanceof Container child) {
                text.append(collectText(child));
            }
        }
        return text.toString();
    }

    private static JButton findButton(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton result = findButton(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
