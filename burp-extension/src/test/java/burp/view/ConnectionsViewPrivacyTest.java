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

import burp.controller.ConnectionController;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConnectionsViewPrivacyTest {
    private static final String PRODUCTION_ENDPOINT =
            "https://d0qcl2e18h.execute-api.us-east-2.amazonaws.com/chariot";

    @Test
    void disclosureNamesGuardSubscriptionsSensitiveDataAndTrainingDefault() {
        assertTrue(ConnectionsView.DATA_UPLOAD_DISCLOSURE
                .contains("Guard platform for active client subscriptions"));
        assertTrue(ConnectionsView.DATA_UPLOAD_DISCLOSURE.contains("credentials, tokens"));
        assertTrue(ConnectionsView.DATA_UPLOAD_DISCLOSURE
                .contains("ML-based training is enabled by default"));
    }

    @Test
    void disclosureUsesFixedWidthHtmlComponent() {
        JScrollPane disclosure = ConnectionsView.createDataUploadDisclosure();

        assertEquals(500, disclosure.getPreferredSize().width);
        assertEquals(240, disclosure.getPreferredSize().height);
        JEditorPane html = assertInstanceOf(
                JEditorPane.class, disclosure.getViewport().getView());
        assertEquals("text/html", html.getContentType());
        assertFalse(html.isEditable());
    }

    @Test
    void shipsTheDefaultGuardApiEndpointWithoutDisplayingItsUrl() {
        ConnectionsView view = new ConnectionsView();
        Container panel = view.createConnectionsPanel(mock(ConnectionController.class));

        assertEquals(PRODUCTION_ENDPOINT, view.readCredentials().endpoint());
        assertNull(findTextField(panel, PRODUCTION_ENDPOINT));
    }

    @Test
    void customSelectionRevealsAndUsesEndpointField() {
        ConnectionsView view = new ConnectionsView();
        Container panel = view.createConnectionsPanel(mock(ConnectionController.class));
        JComboBox<?> environment = findEnvironmentSelector(panel);
        JTextField customEndpoint = findCustomEndpointField(panel);
        assertNotNull(environment);
        assertNotNull(customEndpoint);
        assertFalse(customEndpoint.isVisible());

        environment.setSelectedItem("Custom...");
        customEndpoint.setText("https://custom.example.test/guard");

        assertTrue(customEndpoint.isVisible());
        assertEquals("https://custom.example.test/guard", view.readCredentials().endpoint());
    }

    private static JComboBox<?> findEnvironmentSelector(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JComboBox<?> comboBox && comboBox.getItemCount() == 2
                    && "Guard Production".equals(comboBox.getItemAt(0))) {
                return comboBox;
            }
            if (component instanceof Container child) {
                JComboBox<?> result = findEnvironmentSelector(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static JTextField findCustomEndpointField(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JTextField field
                    && field.getToolTipText() != null
                    && field.getToolTipText().startsWith("Custom Guard API endpoint")) {
                return field;
            }
            if (component instanceof Container child) {
                JTextField result = findCustomEndpointField(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static JTextField findTextField(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JTextField field && text.equals(field.getText())) {
                return field;
            }
            if (component instanceof Container child) {
                JTextField result = findTextField(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
