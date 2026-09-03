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

import burp.controller.ConfigurationController;
import burp.controller.LogController;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

public final class OptionsView {
    private final ConfigurationController configurationController;
    private Consumer<Boolean> scopeListener = ignored -> { };

    public OptionsView(LogController logger) {
        configurationController = new ConfigurationController(logger);
    }

    public ConfigurationController getConfigurationController() {
        return configurationController;
    }

    public void setScopeListener(Consumer<Boolean> listener) {
        scopeListener = listener == null ? ignored -> { } : listener;
    }

    public JPanel createOptionsPanel() {
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(8, 8, 10, 8));
        JPanel toggles = new JPanel();
        toggles.setOpaque(false);
        toggles.setLayout(new BoxLayout(toggles, BoxLayout.Y_AXIS));

        CourierToggle aiTraining = option("ML-based training", true);
        aiTraining.addActionListener(event ->
                configurationController.handleEnableAITrainingChange(aiTraining.isSelected()));
        toggles.add(optionRow(aiTraining, "Include synchronized data in training workflows"));

        CourierToggle respectScope = option("Burp target scope", false);
        respectScope.addActionListener(event -> {
            configurationController.handleRespectScopeChange(respectScope.isSelected());
            scopeListener.accept(respectScope.isSelected());
        });
        toggles.add(optionRow(respectScope, "Capture only requests in Burp's configured target scope"));

        CourierToggle showProxyData = option("Proxy data in log", false);
        showProxyData.addActionListener(event ->
                configurationController.handleShowProxyDataChange(showProxyData.isSelected()));
        toggles.add(optionRow(showProxyData, "Show additional Proxy capture information"));
        toggles.add(Box.createVerticalGlue());
        body.add(toggles, BorderLayout.CENTER);

        JTextField excludedExtensions = new JTextField(
                "svg,png,ico,jpg,jpeg,gif,webp,webm,pdf,mp3,mp4,txt,csv,map,js,css,woff,woff2,ttf,eot,otf");
        CourierTheme.styleInput(excludedExtensions);
        excludedExtensions.setFont(CourierTheme.monoFont(10));
        excludedExtensions.setToolTipText("Comma-separated file extensions excluded from capture");
        excludedExtensions.addActionListener(event ->
                configurationController.handleExcludedExtensionsChange(excludedExtensions.getText()));
        excludedExtensions.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                configurationController.handleExcludedExtensionsChange(excludedExtensions.getText());
            }
        });
        configurationController.handleExcludedExtensionsChange(excludedExtensions.getText());
        body.add(field("Excluded extensions", excludedExtensions), BorderLayout.SOUTH);

        return CourierTheme.card("Capture policy", body,
                CourierTheme.statusPill("Live", CourierTheme.SUCCESS));
    }

    private static CourierToggle option(String name, boolean selected) {
        CourierToggle toggle = new CourierToggle(selected);
        toggle.setName(name);
        return toggle;
    }

    private static JPanel optionRow(CourierToggle toggle, String description) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(9, 5, 9, 5));
        row.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 58));
        JLabel name = new JLabel(toggle.getName());
        name.setFont(CourierTheme.bodyFont(11).deriveFont(java.awt.Font.BOLD));
        JLabel descriptionLabel = new JLabel(description);
        descriptionLabel.setForeground(CourierTheme.muted());
        descriptionLabel.setFont(CourierTheme.bodyFont(10));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(name);
        copy.add(Box.createVerticalStrut(2));
        copy.add(descriptionLabel);
        row.add(toggle, BorderLayout.WEST);
        row.add(copy, BorderLayout.CENTER);
        return row;
    }

    private static JPanel field(String label, java.awt.Component component) {
        JPanel field = new JPanel(new BorderLayout(0, 5));
        field.setOpaque(false);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(CourierTheme.muted());
        fieldLabel.setFont(CourierTheme.bodyFont(10));
        field.add(fieldLabel, BorderLayout.NORTH);
        field.add(component, BorderLayout.CENTER);
        return field;
    }
}
