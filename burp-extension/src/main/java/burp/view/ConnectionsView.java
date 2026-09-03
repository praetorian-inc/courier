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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

public class ConnectionsView {
    private static final String GUARD_PRODUCTION = "Guard Production";
    private static final String CUSTOM_ENDPOINT = "Custom...";
    private static final String DEFAULT_GUARD_ENDPOINT =
            "https://d0qcl2e18h.execute-api.us-east-2.amazonaws.com/chariot";
    static final String DATA_UPLOAD_DISCLOSURE = "<html><body style='width:440px'>"
            + "<b>Courier data upload disclosure</b><br><br>"
            + "When connected, Courier uploads captured HTTP traffic, audit issues, "
            + "Organizer items, and webflows to the Guard platform for active client "
            + "subscriptions. Uploaded data can contain authorization headers, cookies, "
            + "credentials, tokens, request and response bodies, and personal data.<br><br>"
            + "ML-based training is enabled by default and can be disabled under Capture policy."
            + "<br><br>Continue and enable uploads?</body></html>";
    private final JComboBox<String> endpointSelector = new JComboBox<>(
            new String[] {GUARD_PRODUCTION, CUSTOM_ENDPOINT});
    private final JTextField customEndpointField = new JTextField(40);
    private final JPanel endpointControl = new JPanel(new BorderLayout(0, 6));
    private final JTextField chariotApiKeyIdField = new JTextField(40);
    private final JPasswordField chariotApiKeySecretField = new JPasswordField(40);
    private final JTextField projectNameField = new JTextField(40);
    private final JTextField targetApplicationField = new JTextField("https://target.example.test:8443/", 40);
    private final JComboBox<String> accountComboBox = new JComboBox<>();
    private final JButton fetchAccountsButton = new JButton("Refresh accounts");
    private final JButton enableButton = CourierTheme.primaryButton("Connect");
    private final JButton disableButton = new JButton("Disconnect");
    private final JLabel connectionStatus = CourierTheme.statusPill("Disconnected", CourierTheme.muted());
    private Consumer<String> connectionStatusListener = ignored -> { };
    private JPanel connectionPanel;

    public ConnectionsView() {
        customEndpointField.setToolTipText("Custom Guard API endpoint (HTTPS required)");
        endpointControl.setOpaque(false);
        endpointControl.add(endpointSelector, BorderLayout.NORTH);
        endpointControl.add(customEndpointField, BorderLayout.CENTER);
        customEndpointField.setVisible(false);
        endpointSelector.addActionListener(event -> updateEndpointVisibility());
        accountComboBox.setEnabled(false);
        CourierTheme.styleCombo(endpointSelector);
        CourierTheme.styleInput(customEndpointField);
        CourierTheme.styleInput(chariotApiKeyIdField);
        CourierTheme.styleInput(chariotApiKeySecretField);
        CourierTheme.styleInput(projectNameField);
        CourierTheme.styleInput(targetApplicationField);
        customEndpointField.setFont(CourierTheme.monoFont(10));
        chariotApiKeyIdField.setFont(CourierTheme.monoFont(10));
        chariotApiKeySecretField.setFont(CourierTheme.monoFont(10));
        projectNameField.setFont(CourierTheme.monoFont(10));
        targetApplicationField.setFont(CourierTheme.monoFont(10));
        CourierTheme.styleCombo(accountComboBox);
        CourierTheme.styleSecondary(fetchAccountsButton);
        CourierTheme.styleSecondary(disableButton);
    }

    public void setConnectionStatusListener(Consumer<String> listener) {
        connectionStatusListener = listener == null ? ignored -> { } : listener;
    }

    public JPanel createConnectionsPanel(ConnectionController controller) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(12, 12, 12, 12));
        addField(form, "Guard environment", endpointControl, 0, 0, 2);
        addField(form, "API key ID", chariotApiKeyIdField, 0, 1, 1);
        addField(form, "API key secret", chariotApiKeySecretField, 1, 1, 1);
        addField(form, "Project", projectNameField, 0, 2, 1);
        addField(form, "Target application", targetApplicationField, 1, 2, 1);

        JPanel account = new JPanel(new BorderLayout(7, 0));
        account.setOpaque(false);
        account.add(accountComboBox, BorderLayout.CENTER);
        account.add(fetchAccountsButton, BorderLayout.EAST);
        addField(form, "Tenant", account, 0, 3, 2);

        JButton scopeButton = new JButton("Burp scope help");
        CourierTheme.styleSecondary(scopeButton);
        scopeButton.addActionListener(event -> controller.configureBurpScope());
        enableButton.addActionListener(event -> controller.handleConnectButtonClick());
        disableButton.setEnabled(false);
        disableButton.addActionListener(event -> controller.handleDisableButtonClick());
        fetchAccountsButton.addActionListener(event -> controller.handleFetchAccountsButtonClick());

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(12, 0, 0, 0));
        JLabel hint = new JLabel("Credentials remain editable while disconnected.");
        hint.setForeground(CourierTheme.muted());
        hint.setFont(CourierTheme.bodyFont(10));
        actions.add(hint, BorderLayout.WEST);
        actions.add(CourierTheme.actionBar(scopeButton, disableButton, enableButton), BorderLayout.EAST);
        GridBagConstraints actionConstraints = constraints(0, 4, 2);
        actionConstraints.insets = new Insets(5, 0, 0, 0);
        form.add(actions, actionConstraints);

        connectionPanel = CourierTheme.card("Connection & project", form, connectionStatus);
        return connectionPanel;
    }

    public Credentials readCredentials() {
        Object selectedAccount = accountComboBox.getSelectedItem();
        String endpoint = CUSTOM_ENDPOINT.equals(endpointSelector.getSelectedItem())
                ? customEndpointField.getText().trim() : DEFAULT_GUARD_ENDPOINT;
        return new Credentials(
                endpoint,
                chariotApiKeyIdField.getText().trim(),
                new String(chariotApiKeySecretField.getPassword()),
                projectNameField.getText().trim(),
                targetApplicationField.getText().trim(),
                selectedAccount == null ? "" : selectedAccount.toString());
    }

    public boolean confirmDataUploadDisclosure() {
        return JOptionPane.showConfirmDialog(connectionPanel, createDataUploadDisclosure(),
                "Confirm Guard Data Upload", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static JScrollPane createDataUploadDisclosure() {
        JEditorPane disclosure = new JEditorPane("text/html", DATA_UPLOAD_DISCLOSURE);
        disclosure.setEditable(false);
        disclosure.setOpaque(false);
        disclosure.setBorder(null);
        disclosure.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        disclosure.setFont(CourierTheme.bodyFont(10));
        disclosure.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(disclosure);
        scroll.setPreferredSize(new Dimension(500, 240));
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        return scroll;
    }

    public JPanel getConnectionPanel() {
        return connectionPanel;
    }

    public void showConnectingState() {
        enableButton.setEnabled(false);
        disableButton.setEnabled(true);
        setStatus("Connecting", CourierTheme.WARNING);
    }

    public void showConnectedState() {
        setFormEnabled(false);
        enableButton.setEnabled(false);
        disableButton.setEnabled(true);
        accountComboBox.setEnabled(false);
        fetchAccountsButton.setEnabled(false);
        setStatus("Connected", CourierTheme.SUCCESS);
    }

    public void showDisconnectedState() {
        setFormEnabled(true);
        enableButton.setEnabled(true);
        disableButton.setEnabled(false);
        accountComboBox.setEnabled(false);
        accountComboBox.removeAllItems();
        fetchAccountsButton.setEnabled(true);
        fetchAccountsButton.setText("Refresh accounts");
        setStatus("Disconnected", CourierTheme.muted());
    }

    public void showFetchingAccounts(boolean fetching) {
        fetchAccountsButton.setEnabled(!fetching);
        fetchAccountsButton.setText(fetching ? "Fetching…" : "Refresh accounts");
    }

    public void setAccounts(List<String> accounts) {
        accountComboBox.removeAllItems();
        accounts.forEach(accountComboBox::addItem);
        accountComboBox.setEnabled(!accounts.isEmpty());
    }

    private void setStatus(String state, java.awt.Color color) {
        CourierTheme.setStatus(connectionStatus, state, color);
        connectionStatusListener.accept(state);
    }

    private void setFormEnabled(boolean enabled) {
        chariotApiKeyIdField.setEnabled(enabled);
        chariotApiKeySecretField.setEnabled(enabled);
        endpointSelector.setEnabled(enabled);
        customEndpointField.setEnabled(enabled && CUSTOM_ENDPOINT.equals(endpointSelector.getSelectedItem()));
        projectNameField.setEnabled(enabled);
        targetApplicationField.setEnabled(enabled);
    }

    private void updateEndpointVisibility() {
        boolean custom = CUSTOM_ENDPOINT.equals(endpointSelector.getSelectedItem());
        customEndpointField.setVisible(custom);
        customEndpointField.setEnabled(custom && endpointSelector.isEnabled());
        endpointControl.revalidate();
        endpointControl.repaint();
    }

    private static void addField(JPanel panel, String label, java.awt.Component component,
            int x, int y, int width) {
        JPanel field = new JPanel(new BorderLayout(0, 5));
        field.setOpaque(false);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(CourierTheme.muted());
        fieldLabel.setFont(CourierTheme.bodyFont(10));
        field.add(fieldLabel, BorderLayout.NORTH);
        field.add(component, BorderLayout.CENTER);
        panel.add(field, constraints(x, y, width));
    }

    private static GridBagConstraints constraints(int x, int y, int width) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.weightx = width;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, x == 0 ? 0 : 6, 10, x == 0 && width == 1 ? 6 : 0);
        return constraints;
    }

    public record Credentials(String endpoint, String keyId, String keySecret,
            String projectName, String targetApplication, String account) {
    }
}
