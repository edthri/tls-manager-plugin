// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: 2025 NovaMap Health Limited <https://novamap.health>

package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.Connector;
import org.openintegrationengine.tlsmanager.client.dialog.ItemPickerDialog;
import org.openintegrationengine.tlsmanager.shared.models.ClientAuthMode;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;
import org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;

public class ListenerConnectorPropertiesPanel extends AbstractTLSConnectorPropertiesPanel {

    private JLabel clientAuthLabel;
    private MirthRadioButton clientAuthRadioNone;
    private MirthRadioButton clientAuthRadioRequested;
    private MirthRadioButton clientAuthRadioRequired;

    private JLabel trustedClientCertsLabel;
    private JButton trustedClientCertsButton;
    private JLabel trustedClientCertsText;

    private JLabel serverCertificateLabel;
    private JButton serverCertificateButton;
    private JLabel serverCertificateText;

    private TLSConnectorProperties properties;

    public ListenerConnectorPropertiesPanel() {
        this.properties = new TLSConnectorProperties();

        initComponents();
        initLayout();
        fetchData();
    }

    @Override
    public TLSConnectorProperties getProperties() {
        return properties.clone();
    }

    @Override
    public void setProperties(ConnectorProperties connectorProperties, ConnectorPluginProperties connectorPluginProperties, Connector.Mode mode, String s) {
        if (connectorPluginProperties instanceof TLSConnectorProperties tlsConnectorProperties) {
            this.properties = tlsConnectorProperties;
            fetchData();
            redrawState();
            handleManagerEnabledButton(tlsConnectorProperties.isTlsManagerEnabled());
        }
    }

    @Override
    public TLSConnectorProperties getDefaults() {
        return new TLSConnectorProperties();
    }

    @Override
    public boolean checkProperties(ConnectorProperties connectorProperties, ConnectorPluginProperties connectorPluginProperties, Connector.Mode mode, String s, boolean b) {
        return true;
    }

    @Override
    public void resetInvalidProperties() {

    }

    @Override
    public Component[][] getLayoutComponents() {
        return new Component[0][];
    }

    @Override
    public void setLayoutComponentsEnabled(boolean b) {}

    protected void initComponents() {
        super.initComponents();

        serverCertificateLabel = new JLabel("Server Certificate:");
        serverCertificateButton = new JButton(wrenchIcon);
        serverCertificateButton.addActionListener(e -> {
            BiConsumer<Boolean, Set<String>> completionConsumer = (unused, selectedCertificate) -> {
                var selectedAlias = selectedCertificate.stream().findFirst().orElse(null);
                properties.setServerCertificateAlias(selectedAlias);

                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            Set<String> currentCerts = properties.getServerCertificateAlias() == null ?  Collections.emptySet() : Set.of(properties.getServerCertificateAlias());

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Server Certificate Picker",
                clientCertificates,
                currentCerts,
                false,
                null,
                completionConsumer
            );
        });
        serverCertificateText = new JLabel();

        clientAuthLabel = new JLabel("Client Authentication Mode");

        var clientAuthModeButtonGroup = new ButtonGroup();
        clientAuthRadioNone = new MirthRadioButton();
        clientAuthRadioNone.setText("None");
        clientAuthRadioNone.setBackground(Color.white);
        clientAuthRadioNone.addActionListener(e -> handleClientAuthModeChange(ClientAuthMode.NONE, true));
        clientAuthModeButtonGroup.add(clientAuthRadioNone);

        clientAuthRadioRequested = new MirthRadioButton();
        clientAuthRadioRequested.setText("Requested");
        clientAuthRadioRequested.setBackground(Color.white);
        clientAuthRadioRequested.addActionListener(e -> handleClientAuthModeChange(ClientAuthMode.REQUESTED, true));
        clientAuthModeButtonGroup.add(clientAuthRadioRequested);

        clientAuthRadioRequired = new MirthRadioButton();
        clientAuthRadioRequired.setText("Required");
        clientAuthRadioRequired.setBackground(Color.white);
        clientAuthRadioRequired.addActionListener(e -> handleClientAuthModeChange(ClientAuthMode.REQUIRED, true));
        clientAuthModeButtonGroup.add(clientAuthRadioRequired);

        trustedClientCertsLabel = new JLabel("Trusted Client Certificates:");
        trustedClientCertsButton = new JButton(wrenchIcon);
        trustedClientCertsButton.addActionListener(e -> {
            BiConsumer<Boolean, Set<String>> completionConsumer = (isTrustSystemTrustStoreEnabled, selectedCertificates) -> {
                properties.setTrustSystemTruststore(isTrustSystemTrustStoreEnabled);
                if (isTrustSystemTrustStoreEnabled) {
                    properties.setTrustedServerCertificates(Collections.emptySet());
                } else {
                    properties.setTrustedServerCertificates(selectedCertificates);
                }

                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Trusted Client Certificates Picker",
                publicCertificates,
                properties.getTrustedServerCertificates(),
                properties.isTrustSystemTruststore(),
                "[Server default]",
                completionConsumer
            );
        });
        trustedClientCertsText = new JLabel();

        subjectDnValidationFilterTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                properties.setSubjectDnValidationFilter(subjectDnValidationFilterTextField.getText());
            }
        });

        protocolsButton.addActionListener(e -> {
            BiConsumer<Boolean, Set<String>> completionConsumer = (trustDefaultProtocols, selectedProtocols) -> {
                properties.setUseServerDefaultProtocols(trustDefaultProtocols);
                if (trustDefaultProtocols) {
                    properties.setUsedProtocols(Collections.emptySet());
                } else {
                    properties.setUsedProtocols(selectedProtocols);
                }

                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Protocols Picker",
                supportedProtocols,
                properties.getUsedProtocols(),
                properties.isUseServerDefaultProtocols(),
                "[Server default]",
                completionConsumer
            );
        });

        ciphersButton.addActionListener(e -> {
            BiConsumer<Boolean, Set<String>> completionConsumer = (trustDefaultCiphers, selectedCiphers) -> {
                properties.setUseServerDefaultCiphers(trustDefaultCiphers);
                if (trustDefaultCiphers) {
                    properties.setUsedCiphers(Collections.emptySet());
                } else {
                    properties.setUsedCiphers(selectedCiphers);
                }

                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Ciphers Picker",
                supportedCiphers,
                properties.getUsedCiphers(),
                properties.isUseServerDefaultCiphers(),
                "[Server default]",
                completionConsumer
            );
        });
    }

    protected void initLayout() {
        super.initLayout();

        add(serverCertificateLabel, "newline, right");
        add(serverCertificateButton, "h 22!, w 22!, split");
        add(serverCertificateText);

        add(clientAuthLabel, "newline, right");
        add(clientAuthRadioNone, "split");
        add(clientAuthRadioRequested, "split");
        add(clientAuthRadioRequired);

        add(trustedClientCertsLabel, "newline, right");
        add(trustedClientCertsButton, "h 22!, w 22!, split");
        add(trustedClientCertsText);
    }

    private void handleClientAuthModeChange(ClientAuthMode authMode, boolean persistChanges) {
        if (persistChanges) {
            properties.setClientAuthMode(authMode);
        }

        var issuerSelectorEnabled = authMode != ClientAuthMode.NONE;
        trustedClientCertsLabel.setEnabled(issuerSelectorEnabled);
        trustedClientCertsButton.setEnabled(issuerSelectorEnabled);
        trustedClientCertsText.setEnabled(issuerSelectorEnabled);
    }

    protected void handleSubjectDnValidationModeChange() {
        if (subjectDnValidationModeComboBox.getSelectedItem() instanceof SubjectDnValidationMode validationMode) {
            properties.setSubjectDnValidationMode(validationMode);
            redrawState();
        }
    }

    protected void handleCrlModeChange() {
        if (crlModeComboBox.getSelectedItem() instanceof RevocationMode revocationMode) {
            properties.setCrlMode(revocationMode);
        }
    }

    protected void handleOcspModeChange() {
        if (ocspModeComboBox.getSelectedItem() instanceof RevocationMode revocationMode) {
            properties.setOcspMode(revocationMode);
        }
    }

    protected void handleManagerEnabledButton(boolean managerEnabled) {
        properties.setTlsManagerEnabled(managerEnabled);

        serverCertificateLabel.setEnabled(managerEnabled);
        serverCertificateButton.setEnabled(managerEnabled);
        serverCertificateText.setEnabled(managerEnabled);

        clientAuthLabel.setEnabled(managerEnabled);
        clientAuthRadioNone.setEnabled(managerEnabled);
        clientAuthRadioRequested.setEnabled(managerEnabled);
        clientAuthRadioRequired.setEnabled(managerEnabled);

        if (managerEnabled) {
            handleClientAuthModeChange(properties.getClientAuthMode(), false);
        } else {
            trustedClientCertsLabel.setEnabled(false);
            trustedClientCertsButton.setEnabled(false);
            trustedClientCertsText.setEnabled(false);
        }

        subjectDnValidationLabel.setEnabled(managerEnabled);
        subjectDnValidationModeComboBox.setEnabled(managerEnabled);
        subjectDnValidationFilterTextField.setEnabled(
            managerEnabled && properties.getSubjectDnValidationMode() != SubjectDnValidationMode.NONE
        );

        crlModeLabel.setEnabled(managerEnabled);
        crlModeComboBox.setEnabled(managerEnabled);

        ocspModeLabel.setEnabled(managerEnabled);
        ocspModeComboBox.setEnabled(managerEnabled);

        protocolsLabel.setEnabled(managerEnabled);
        protocolsButton.setEnabled(managerEnabled);
        protocolsText.setEnabled(managerEnabled);

        ciphersLabel.setEnabled(managerEnabled);
        ciphersButton.setEnabled(managerEnabled);
        ciphersText.setEnabled(managerEnabled);
    }

    protected void redrawState() {
        if (properties.isTlsManagerEnabled()) {
            managerEnabledRadioYes.setSelected(true);
        } else {
            managerEnabledRadioNo.setSelected(true);
        }

        serverCertificateText.setText(properties.getServerCertificateAlias());

        if (properties.getClientAuthMode() == ClientAuthMode.NONE) {
            clientAuthRadioNone.setSelected(true);
        } else if (properties.getClientAuthMode() == ClientAuthMode.REQUESTED) {
            clientAuthRadioRequested.setSelected(true);
        } else if (properties.getClientAuthMode() == ClientAuthMode.REQUIRED) {
            clientAuthRadioRequired.setSelected(true);
        } else {
            clientAuthRadioNone.setSelected(true);
            log("Unable to determine client auth mode: %s. Using NONE");
        }

        handleClientAuthModeChange(properties.getClientAuthMode(), false);

        var thingsToTrust = new ArrayList<String>();
        if (properties.isTrustSystemTruststore()) {
            thingsToTrust.add("System Truststore");
        }

        if (properties.getTrustedServerCertificates() != null && !properties.getTrustedServerCertificates().isEmpty()) {
            var count = properties.getTrustedServerCertificates().size();
            var plural = (count == 1) ? "" : "s";
            thingsToTrust.add("%d certificate%s".formatted(count, plural));
        }

        var serverCertificatesText = "Trusting %s".formatted(
            thingsToTrust.isEmpty()
                ? "no one >:C"
                : String.join(" and ", thingsToTrust)
        );
        trustedClientCertsText.setText(serverCertificatesText);

        subjectDnValidationModeComboBox.setSelectedItem(properties.getSubjectDnValidationMode());
        subjectDnValidationFilterTextField.setEnabled(properties.getSubjectDnValidationMode() != SubjectDnValidationMode.NONE);
        subjectDnValidationFilterTextField.setText(properties.getSubjectDnValidationFilter());

        crlModeComboBox.setSelectedItem(properties.getCrlMode());
        ocspModeComboBox.setSelectedItem(properties.getOcspMode());

        var protocolsString = properties.isUseServerDefaultProtocols()
            ? "Server default: %s".formatted(supportedProtocols)
            : "%d selected".formatted(properties.getUsedProtocols().size());

        protocolsText.setText(protocolsString);

        var ciphersString = properties.isUseServerDefaultCiphers()
            ? "Server default: %d selected".formatted(supportedCiphers.size())
            : "%d selected".formatted(properties.getUsedCiphers().size());

        ciphersText.setText(ciphersString);
    }
}
