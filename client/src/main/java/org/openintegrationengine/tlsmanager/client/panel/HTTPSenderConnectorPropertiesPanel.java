/*
 * Copyright 2025 Kaur Palang
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

package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.ui.AbstractConnectorPropertiesPanel;
import com.mirth.connect.client.ui.ConnectorTypeDecoration;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.Connector;
import net.miginfocom.swing.MigLayout;
import org.openintegrationengine.tlsmanager.client.dialog.ItemPickerDialog;
import org.openintegrationengine.tlsmanager.shared.properties.HttpConnectorProperties;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class HTTPSenderConnectorPropertiesPanel extends AbstractConnectorPropertiesPanel {

    private JLabel managerEnabledLabel;
    private MirthRadioButton managerEnabledRadioYes;
    private MirthRadioButton managerEnabledRadioNo;

    private JLabel serverCertificateValidationLabel;
    private MirthRadioButton serverCertificateValidationRadioYes;
    private MirthRadioButton serverCertificateValidationRadioNo;

    private JLabel trustedServerCertsLabel;
    private JButton trustedServerCertsButton;
    private JLabel trustedServerCertsText;

    private JLabel hostnameValidationLabel;
    private MirthRadioButton hostnameValidationRadioYes;
    private MirthRadioButton hostnameValidationRadioNo;

    private JLabel clientCertLabel;
    private JButton clientCertButton;
    private JLabel clientCertText;

    private JLabel protocolsLabel;
    private JButton protocolsButton;
    private JLabel protocolsText;

    private JLabel ciphersLabel;
    private JButton ciphersButton;
    private JLabel ciphersText;

    private HttpConnectorProperties properties;

    public HTTPSenderConnectorPropertiesPanel() {
        this.properties = new HttpConnectorProperties();

        initComponents();
        initLayout();
    }

    @Override
    public HttpConnectorProperties getProperties() {
        return properties.clone();
    }

    @Override
    public void setProperties(ConnectorProperties connectorProperties, ConnectorPluginProperties connectorPluginProperties, Connector.Mode mode, String s) {
        if (connectorPluginProperties instanceof HttpConnectorProperties httpConnectorProperties) {
            this.properties = httpConnectorProperties;
            redrawState();
            handleManagerEnabledButton(httpConnectorProperties.isTlsManagerEnabled());
        }
    }

    @Override
    public ConnectorPluginProperties getDefaults() {
        return new HttpConnectorProperties();
    }

    @Override
    public boolean checkProperties(ConnectorProperties connectorProperties, ConnectorPluginProperties connectorPluginProperties, Connector.Mode mode, String s, boolean b) {
        return true;
    }

    @Override
    public void resetInvalidProperties() {}

    @Override
    public Component[][] getLayoutComponents() {
        return null;
    }

    @Override
    public void setLayoutComponentsEnabled(boolean isEnabled) {}

    @Override
    public ConnectorTypeDecoration getConnectorTypeDecoration() {
        return new ConnectorTypeDecoration(Connector.Mode.DESTINATION);
    }

    private void initComponents() {
        setBackground(UIConstants.BACKGROUND_COLOR);

        var wrenchIcon = new ImageIcon(Frame.class.getResource("images/wrench.png"));

        managerEnabledLabel = new JLabel("Use TLS Manager:");
        var managerEnabledButtonGroup = new ButtonGroup();

        managerEnabledRadioYes = new MirthRadioButton();
        managerEnabledRadioYes.setText("Yes");
        managerEnabledRadioYes.setBackground(new Color(255, 255, 255));
        managerEnabledRadioYes.addActionListener(e -> handleManagerEnabledButton(true));
        managerEnabledButtonGroup.add(managerEnabledRadioYes);

        managerEnabledRadioNo = new MirthRadioButton();
        managerEnabledRadioNo.setText("No");
        managerEnabledRadioNo.setBackground(new Color(255, 255, 255));
        managerEnabledRadioNo.addActionListener(e -> handleManagerEnabledButton(false));
        managerEnabledButtonGroup.add(managerEnabledRadioNo);

        serverCertificateValidationLabel = new JLabel("Server Certificate Validation:");
        var serverCertificateValidationButtonGroup = new ButtonGroup();

        serverCertificateValidationRadioYes = new MirthRadioButton();
        serverCertificateValidationRadioYes.setBackground(new Color(255, 255, 255));
        serverCertificateValidationRadioYes.setText("Enabled");
        serverCertificateValidationRadioYes.addActionListener(e -> properties.setServerCertificateValidationEnabled(true));
        serverCertificateValidationButtonGroup.add(serverCertificateValidationRadioYes);

        serverCertificateValidationRadioNo = new MirthRadioButton();
        serverCertificateValidationRadioNo.setBackground(new Color(255, 255, 255));
        serverCertificateValidationRadioNo.setText("Disabled");
        serverCertificateValidationRadioNo.addActionListener(e -> properties.setServerCertificateValidationEnabled(false));
        serverCertificateValidationButtonGroup.add(serverCertificateValidationRadioNo);

        trustedServerCertsLabel = new JLabel("Trusted Server Certificates:");
        trustedServerCertsButton = new JButton(wrenchIcon);
        trustedServerCertsButton.addActionListener(e -> {
            BiConsumer<Boolean, List<String>> completionConsumer = (isTrustSystemTrustStoreEnabled, selectedCerts) -> {
                properties.setTrustSystemTruststore(isTrustSystemTrustStoreEnabled);
                properties.setTrustedServerCertificates(selectedCerts);
                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Certificate Picker",
                List.of("GoDaddy", "Some other CA"),
                properties.getTrustedServerCertificates(),
                properties.isTrustSystemTruststore(),
                "[JVM Truststore]",
                completionConsumer
            );
        });

        trustedServerCertsText = new JLabel("Trusting some certs as a placeholder");

        hostnameValidationLabel = new JLabel("Hostname verification:");
        var hostnameValidationButtonGroup = new ButtonGroup();

        hostnameValidationRadioYes = new MirthRadioButton();
        hostnameValidationRadioYes.setBackground(new Color(255, 255, 255));
        hostnameValidationRadioYes.setText("Enabled");
        hostnameValidationRadioYes.addActionListener(e -> properties.setHostnameVerificationEnabled(true));
        hostnameValidationButtonGroup.add(hostnameValidationRadioYes);

        hostnameValidationRadioNo = new MirthRadioButton();
        hostnameValidationRadioNo.setBackground(new Color(255, 255, 255));
        hostnameValidationRadioNo.setText("Disabled");
        hostnameValidationRadioNo.addActionListener(e -> properties.setHostnameVerificationEnabled(false));
        hostnameValidationButtonGroup.add(hostnameValidationRadioNo);

        clientCertLabel = new JLabel("Client Certificate:");
        clientCertButton = new JButton(wrenchIcon);
        clientCertButton.addActionListener(e -> System.out.println("client button"));
        clientCertText = new JLabel("myclientcert");

        protocolsLabel = new JLabel("Enabled Protocols:");
        protocolsButton = new JButton(wrenchIcon);
        protocolsButton.addActionListener(e -> {
            BiConsumer<Boolean, List<String>> completionConsumer = (trustDefaultProtocols, selectedProtocols) -> {
                properties.setUseServerDefaultProtocols(trustDefaultProtocols);
                if (trustDefaultProtocols) {
                    properties.setUsedProtocols(Collections.emptyList());
                } else {
                    properties.setUsedProtocols(selectedProtocols);
                }

                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Protocols Picker",
                List.of(PlatformUI.HTTPS_PROTOCOLS),
                properties.getUsedProtocols(),
                properties.isUseServerDefaultProtocols(),
                "[Server default]",
                completionConsumer
            );
        });
        protocolsText = new JLabel("Server default: TLSv4.6");

        ciphersLabel = new JLabel("Enabled Ciphers:");
        ciphersButton = new JButton(wrenchIcon);
        ciphersButton.addActionListener(e -> {
            BiConsumer<Boolean, List<String>> completionConsumer = (trustDefaultCiphers, selectedCiphers) -> {
                properties.setUseServerDefaultCiphers(trustDefaultCiphers);
                if (trustDefaultCiphers) {
                    properties.setUsedCiphers(Collections.emptyList());
                } else {
                    properties.setUsedCiphers(selectedCiphers);
                }

                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Ciphers Picker",
                List.of(PlatformUI.HTTPS_CIPHER_SUITES),
                properties.getUsedCiphers(),
                properties.isUseServerDefaultCiphers(),
                "[Server default]",
                completionConsumer
            );
        });
        ciphersText = new JLabel("Server default: 22 enabled");
    }

    private void initLayout() {
        setLayout(new MigLayout("insets 0, novisualpadding, hidemode 3", "[]12[]", ""));

        add(managerEnabledLabel, "newline, right");
        add(managerEnabledRadioYes, "split");
        add(managerEnabledRadioNo);

        add(serverCertificateValidationLabel, "newline, right");
        add(serverCertificateValidationRadioYes, "split");
        add(serverCertificateValidationRadioNo);

        add(trustedServerCertsLabel, "newline, right");
        add(trustedServerCertsButton, "h 22!, w 22!, split");
        add(trustedServerCertsText);

        add(hostnameValidationLabel, "newline, right");
        add(hostnameValidationRadioYes, "split");
        add(hostnameValidationRadioNo);

        add(clientCertLabel, "newline, right");
        add(clientCertButton, "h 22!, w 22!, split");
        add(clientCertText);

        add(protocolsLabel, "newline, right");
        add(protocolsButton, "h 22!, w 22!, split");
        add(protocolsText);

        add(ciphersLabel, "newline, right");
        add(ciphersButton, "h 22!, w 22!, split");
        add(ciphersText);
    }

    private void handleManagerEnabledButton(boolean managerEnabled) {
        properties.setTlsManagerEnabled(managerEnabled);

        serverCertificateValidationLabel.setEnabled(managerEnabled);
        serverCertificateValidationRadioYes.setEnabled(managerEnabled);
        serverCertificateValidationRadioNo.setEnabled(managerEnabled);

        trustedServerCertsLabel.setEnabled(managerEnabled);
        trustedServerCertsButton.setEnabled(managerEnabled);
        trustedServerCertsText.setEnabled(managerEnabled);

        hostnameValidationLabel.setEnabled(managerEnabled);
        hostnameValidationRadioYes.setEnabled(managerEnabled);
        hostnameValidationRadioNo.setEnabled(managerEnabled);

        clientCertLabel.setEnabled(managerEnabled);
        clientCertButton.setEnabled(managerEnabled);
        clientCertText.setEnabled(managerEnabled);

        protocolsLabel.setEnabled(managerEnabled);
        protocolsButton.setEnabled(managerEnabled);
        protocolsText.setEnabled(managerEnabled);

        ciphersLabel.setEnabled(managerEnabled);
        ciphersButton.setEnabled(managerEnabled);
        ciphersText.setEnabled(managerEnabled);
    }

    private void redrawState() {
        if (properties.isTlsManagerEnabled()) {
            managerEnabledRadioYes.setSelected(true);
        } else {
            managerEnabledRadioNo.setSelected(true);
        }

        if (properties.isServerCertificateValidationEnabled()) {
            serverCertificateValidationRadioYes.setSelected(true);
        } else {
            serverCertificateValidationRadioNo.setSelected(true);
        }

        var count = properties.getTrustedServerCertificates().size();
        var plural = (count == 1) ? "" : "s";

        var serverCertificatesText = "Trusting %d cert%s%s".formatted(
            count,
            plural,
            properties.isTrustSystemTruststore() ? " and System Truststore" : ""
        );

        trustedServerCertsText.setText(serverCertificatesText);

        if (properties.isHostnameVerificationEnabled()) {
            hostnameValidationRadioYes.setSelected(true);
        } else {
            hostnameValidationRadioNo.setSelected(true);
        }

        clientCertText.setText(properties.getClientCertificateAlias());

        var protocolsString = properties.isUseServerDefaultProtocols()
            ? "Server default: %s".formatted(Arrays.toString(PlatformUI.HTTPS_PROTOCOLS))
            : "%d selected".formatted(properties.getUsedProtocols().size());

        protocolsText.setText(protocolsString);

        var ciphersString = properties.isUseServerDefaultCiphers()
            ? "Server default: %d selected".formatted(PlatformUI.HTTPS_CIPHER_SUITES.length)
            : "%d selected".formatted(properties.getUsedCiphers().size());

        ciphersText.setText(ciphersString);
    }
}
