// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: 2025 NovaMap Health Limited <https://novamap.health>

package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.ConnectorTypeDecoration;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.components.MirthEditableComboBox;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.panels.connectors.ResponseHandler;
import com.mirth.connect.connectors.http.HttpDispatcherProperties;
import com.mirth.connect.connectors.http.HttpSender;
import com.mirth.connect.connectors.tcp.TcpDispatcherProperties;
import com.mirth.connect.connectors.tcp.TcpSender;
import com.mirth.connect.connectors.ws.DefinitionServiceMap;
import com.mirth.connect.connectors.ws.WebServiceDispatcherProperties;
import com.mirth.connect.connectors.ws.WebServiceSender;
import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.Connector;
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.client.dialog.ItemPickerDialog;
import org.openintegrationengine.tlsmanager.client.misc.SwingMagic;
import org.openintegrationengine.tlsmanager.shared.models.ConnectionTestResult;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;
import org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties;
import org.openintegrationengine.tlsmanager.shared.servlet.TLSServletInterface;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@Slf4j
public class SenderConnectorPropertiesPanel extends AbstractTLSConnectorPropertiesPanel {

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

    private TLSConnectorProperties properties;
    private enum Transport { HTTP, TCP, WS };

    private final ResponseHandler responseHandler;

    public SenderConnectorPropertiesPanel() {
        this.properties = new TLSConnectorProperties();

        this.responseHandler = new ResponseHandler() {
            @Override
            public void handle(Object response) {
                var result = (ConnectionTestResult) response;

                if (result == null) {
                    parentFrame.alertError(parentFrame, "Failed to invoke service.");
                } else {
                    new ConnectionTestResultPanel(PlatformUI.MIRTH_FRAME, result);
                }
            }
        };

        initComponents();
        initLayout();
        fetchData();
    }

    private List<JButton> getButtonsByText(String text) {
        var settingsComponents = connectorPanel
            .getConnectorSettingsPanel()
            .getComponents();

        return Arrays
            .stream(settingsComponents)
            .filter(component -> component instanceof JButton)
            .map(component -> (JButton) component)
            .filter(button -> button.getText().equals(text))
            .toList();
    }

    private void doActionListenerOverrides() {
        var settingsPanel = connectorPanel.getConnectorSettingsPanel();

        Transport transport;
        if (settingsPanel instanceof HttpSender) {
            transport = Transport.HTTP;
        } else if (settingsPanel instanceof TcpSender) {
            transport = Transport.TCP;
        } else if (settingsPanel instanceof WebServiceSender) {
            transport = Transport.WS;
        } else {
            return;
        }

        if (transport == Transport.HTTP || transport == Transport.TCP) {
            var testConnectionButtons = getButtonsByText("Test Connection");
            if (!testConnectionButtons.isEmpty()) {
                var button = testConnectionButtons.get(0);

                var actionListeners = button.getActionListeners().clone();

                var previousActionListener = actionListeners[0]; // Hope it only has a single listener

                // Replace the ActionListener
                button.removeActionListener(previousActionListener);
                button.addActionListener(e -> testTlsConnection(previousActionListener, e, transport));
            } else {
                var message = "No test connection button found in settings panel %s".formatted(settingsPanel);
                log(message);
            }
        }

        if (transport == Transport.WS) {
            var testConnectionButtons = getButtonsByText("Test Connection");
            if (!testConnectionButtons.isEmpty()) {
                // This works on the faint hope the buttons are ordered, and the order of said buttons is not messed with during processing...
                var testWsdlConnectionButton = testConnectionButtons.get(0);
                var testLocationConnectionButton = testConnectionButtons.get(1);

                var wsdlActionListeners = testWsdlConnectionButton.getActionListeners().clone();
                var locationActionListeners = testLocationConnectionButton.getActionListeners().clone();

                var previousWsdlActionListener = wsdlActionListeners[0];
                var previousLocationActionListener = locationActionListeners[0];

                testWsdlConnectionButton.removeActionListener(previousWsdlActionListener);
                testWsdlConnectionButton.addActionListener(e -> testWsTlsConnection(previousWsdlActionListener, e, true));

                testLocationConnectionButton.removeActionListener(previousLocationActionListener);
                testLocationConnectionButton.addActionListener(e -> testWsTlsConnection(previousLocationActionListener, e, false));
            } else {
                var message = "No Get Operations button found in settings panel %s".formatted(settingsPanel);
                log(message);
            }

            var getOperationsButtons = getButtonsByText("Get Operations");
            if (!getOperationsButtons.isEmpty()) {
                var button = getOperationsButtons.get(0);

                var actionListeners = button.getActionListeners().clone();

                var previousActionListener = actionListeners[0]; // Hope it only has a single listener

                // Replace the ActionListener
                button.removeActionListener(previousActionListener);
                button.addActionListener(e -> getOperations(previousActionListener, e));
            } else {
                var message = "No Get Operations button found in settings panel %s".formatted(settingsPanel);
                log(message);
            }
        }
    }

    private void getOperations(ActionListener nonTlsActionListener, ActionEvent event) {
        if (!properties.isTlsManagerEnabled()) {
            // If TLS management is disabled, run the previous non-tls connection test
            // The <code>isWsdlUrlBeingTested</code> hopefully doesn't matter here as the listeners are already defined
            // by the sender panel.
            nonTlsActionListener.actionPerformed(event);
            return;
        }

        var webServiceSender = (WebServiceSender) connectorPanel.getConnectorSettingsPanel();
        if (!parentFrame.alertOkCancel(parentFrame, "This will replace your current service, port, location URI, and operation list. Press OK to continue.")) {
            return;
        }

        var wsProperties = (WebServiceDispatcherProperties) connectorPanel.getProperties();

        // wtf...
        var cacheWsdlHandler = new ResponseHandler() {
            @Override
            public void handle(Object response) {
                try {
                    var retrieveWsdlFromCacheHandler = new ResponseHandler() {
                        @Override
                        public void handle(Object response) {
                            if (response == null) {
                                return;
                            }

                            var definitionServiceMap = (DefinitionServiceMap) response;

                            var currentProperties = (WebServiceDispatcherProperties) webServiceSender.getProperties();
                            currentProperties.setWsdlDefinitionMap(definitionServiceMap);

                            // Trigger private loadServiceMap() function
                            webServiceSender.setProperties(currentProperties);

                            // Trigger population of service and port comboboxes
                            var serviceCombobox = SwingMagic.findComponentFollowingLabel(connectorPanel.getConnectorSettingsPanel(), "Service:");
                            if (serviceCombobox instanceof MirthEditableComboBox serviceEditableCombobox) {
                                serviceEditableCombobox.setSelectedItem(definitionServiceMap.getMap().keySet().iterator().next());
                            }

                            parentFrame.setSaveEnabled(true);
                        }
                    };

                    connectorPanel
                        .getConnectorSettingsPanel()
                        .getServlet(
                            TLSServletInterface.class,
                            "Retrieving cached WSDL definition map...",
                            "There was an error retrieving the cached WSDL definition map.\n\n",
                            retrieveWsdlFromCacheHandler
                        )
                        .getDefinition(
                            connectorPanel.getConnectorSettingsPanel().getChannelId(),
                            connectorPanel.getConnectorSettingsPanel().getChannelName(),
                            wsProperties.getWsdlUrl(),
                            wsProperties.getUsername(),
                            wsProperties.getPassword()
                        );
                } catch (ClientException e) {
                    // Should not happen
                }
            }
        };

        try {
            connectorPanel
                .getConnectorSettingsPanel()
                .getServlet(
                    TLSServletInterface.class,
                    "Getting operations...",
                    "Error caching WSDL. Please check the WSDL URL and authentication settings.\n\n",
                    cacheWsdlHandler
                )
                .cacheWsdlFromUrl(
                    connectorPanel.getConnectorSettingsPanel().getChannelId(),
                    connectorPanel.getConnectorSettingsPanel().getChannelName(),
                    wsProperties
                );
        } catch (ClientException e) {
            // Should not happen
        }
    }

    private void testTlsConnection(ActionListener nonTlsActionListener, ActionEvent event, Transport transport) {
        if (!properties.isTlsManagerEnabled()) {
            // If TLS management is disabled, run the previous non-tls connection test
            nonTlsActionListener.actionPerformed(event);
            return;
        }

        try {
            var servletInterface = connectorPanel
                .getConnectorSettingsPanel()
                .getServlet(
                    TLSServletInterface.class,
                    "Testing connection...",
                    "Error testing TLS connection",
                    this.responseHandler
                );

            if (transport == Transport.HTTP) {
                servletInterface.testHttpsConnection(
                    connectorPanel.getConnectorSettingsPanel().getChannelId(),
                    connectorPanel.getConnectorSettingsPanel().getChannelName(),
                    (HttpDispatcherProperties) connectorPanel.getProperties()
                );
            } else if (transport == Transport.TCP) {
                servletInterface.testTcpConnection(
                    connectorPanel.getConnectorSettingsPanel().getChannelId(),
                    connectorPanel.getConnectorSettingsPanel().getChannelName(),
                    (TcpDispatcherProperties) connectorPanel.getProperties()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Should not happen?
        }
    }

    private void testWsTlsConnection(ActionListener nonTlsActionListener, ActionEvent event, boolean isWsdlUrlBeingTested) {
        if (!properties.isTlsManagerEnabled()) {
            // If TLS management is disabled, run the previous non-tls connection test
            // The <code>isWsdlUrlBeingTested</code> hopefully doesn't matter here as the listeners are already defined
            // by the sender panel.
            nonTlsActionListener.actionPerformed(event);
            return;
        }

        if (!canTestConnection(isWsdlUrlBeingTested)) {
            return;
        }

        var wsProperties = (WebServiceDispatcherProperties) connectorPanel.getProperties();

        // Blank out the other property so that it isn't tested
        if (isWsdlUrlBeingTested) {
            wsProperties.setLocationURI("");
        } else {
            wsProperties.setWsdlUrl("");
        }

        try {
            connectorPanel
                .getConnectorSettingsPanel()
                .getServlet(
                    TLSServletInterface.class,
                    "Testing connection...",
                    "Error testing Web Service connection: ",
                    this.responseHandler
                ).testWsConnection(
                    connectorPanel.getConnectorSettingsPanel().getChannelId(),
                    connectorPanel.getConnectorSettingsPanel().getChannelName(),
                    wsProperties
                );
        } catch (ClientException e) {
            // Should not happen
        }
    }

    private boolean canTestConnection(boolean isWsdlUrlBeingTested) {
        var wsProperties = (WebServiceDispatcherProperties) connectorPanel.getProperties();

        if (isWsdlUrlBeingTested) {
            if (wsProperties.getWsdlUrl() == null || wsProperties.getWsdlUrl().isBlank()) {
                parentFrame.alertError(parentFrame, "-WSDL URL is blank.");
            }
        } else if (wsProperties.getLocationURI() == null || wsProperties.getLocationURI().isBlank()) {
            parentFrame.alertError(parentFrame, "-Location URI is blank.");
            return false;
        }

        return true;
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
        // This method seems to be called after other panels have been initialized.
        // We need other panels to be initialized 'cause we'll be fiddling with one.
        doActionListenerOverrides();
    }

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

    protected void initComponents() {
        super.initComponents();

        serverCertificateValidationLabel = new JLabel("Server Certificate Validation:");
        var serverCertificateValidationButtonGroup = new ButtonGroup();

        serverCertificateValidationRadioYes = new MirthRadioButton();
        serverCertificateValidationRadioYes.setBackground(Color.white);
        serverCertificateValidationRadioYes.setText("Enabled");
        serverCertificateValidationRadioYes.addActionListener(e -> properties.setServerCertificateValidationEnabled(true));
        serverCertificateValidationButtonGroup.add(serverCertificateValidationRadioYes);

        serverCertificateValidationRadioNo = new MirthRadioButton();
        serverCertificateValidationRadioNo.setBackground(Color.white);
        serverCertificateValidationRadioNo.setText("Disabled");
        serverCertificateValidationRadioNo.addActionListener(e -> properties.setServerCertificateValidationEnabled(false));
        serverCertificateValidationButtonGroup.add(serverCertificateValidationRadioNo);

        trustedServerCertsLabel = new JLabel("Trusted Server Certificates:");
        trustedServerCertsButton = new JButton(wrenchIcon);
        trustedServerCertsButton.addActionListener(e -> {
            BiConsumer<Boolean, Set<String>> completionConsumer = (isTrustSystemTrustStoreEnabled, selectedCerts) -> {
                properties.setTrustSystemTruststore(isTrustSystemTrustStoreEnabled);
                properties.setTrustedServerCertificates(selectedCerts);
                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Certificate Picker",
                publicCertificates,
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
        hostnameValidationRadioYes.setBackground(Color.white);
        hostnameValidationRadioYes.setText("Enabled");
        hostnameValidationRadioYes.addActionListener(e -> properties.setHostnameVerificationEnabled(true));
        hostnameValidationButtonGroup.add(hostnameValidationRadioYes);

        hostnameValidationRadioNo = new MirthRadioButton();
        hostnameValidationRadioNo.setBackground(Color.white);
        hostnameValidationRadioNo.setText("Disabled");
        hostnameValidationRadioNo.addActionListener(e -> properties.setHostnameVerificationEnabled(false));
        hostnameValidationButtonGroup.add(hostnameValidationRadioNo);

        subjectDnValidationFilterTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                properties.setSubjectDnValidationFilter(subjectDnValidationFilterTextField.getText());
            }
        });

        clientCertLabel = new JLabel("Client Certificate:");
        clientCertButton = new JButton(wrenchIcon);
        clientCertButton.addActionListener(e -> {
            BiConsumer<Boolean, Set<String>> completionConsumer = (unused, selectedCertificate) -> {
                var selectedAlias = selectedCertificate.stream().findFirst().orElse(null);
                properties.setClientCertificateAlias(selectedAlias);

                redrawState();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            };

            Set<String> currentCerts = properties.getClientCertificateAlias() == null ?  Collections.emptySet() : Set.of(properties.getClientCertificateAlias());

            new ItemPickerDialog(
                PlatformUI.MIRTH_FRAME,
                "Client Certificate Picker",
                clientCertificates,
                currentCerts,
                false,
                null,
                completionConsumer
            );
        });
        clientCertText = new JLabel("");

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

        serverCertificateValidationLabel.setEnabled(managerEnabled);
        serverCertificateValidationRadioYes.setEnabled(managerEnabled);
        serverCertificateValidationRadioNo.setEnabled(managerEnabled);

        trustedServerCertsLabel.setEnabled(managerEnabled);
        trustedServerCertsButton.setEnabled(managerEnabled);
        trustedServerCertsText.setEnabled(managerEnabled);

        hostnameValidationLabel.setEnabled(managerEnabled);
        hostnameValidationRadioYes.setEnabled(managerEnabled);
        hostnameValidationRadioNo.setEnabled(managerEnabled);

        subjectDnValidationLabel.setEnabled(managerEnabled);
        subjectDnValidationModeComboBox.setEnabled(managerEnabled);
        subjectDnValidationFilterTextField.setEnabled(
            managerEnabled && properties.getSubjectDnValidationMode() != SubjectDnValidationMode.NONE
        );

        crlModeLabel.setEnabled(managerEnabled);
        crlModeComboBox.setEnabled(managerEnabled);

        ocspModeLabel.setEnabled(managerEnabled);
        ocspModeComboBox.setEnabled(managerEnabled);

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

    protected void redrawState() {
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

        subjectDnValidationModeComboBox.setSelectedItem(properties.getSubjectDnValidationMode());
        subjectDnValidationFilterTextField.setEnabled(properties.getSubjectDnValidationMode() != SubjectDnValidationMode.NONE);
        subjectDnValidationFilterTextField.setText(properties.getSubjectDnValidationFilter());

        crlModeComboBox.setSelectedItem(properties.getCrlMode());
        ocspModeComboBox.setSelectedItem(properties.getOcspMode());

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
        trustedServerCertsText.setText(serverCertificatesText);

        if (properties.isHostnameVerificationEnabled()) {
            hostnameValidationRadioYes.setSelected(true);
        } else {
            hostnameValidationRadioNo.setSelected(true);
        }

        clientCertText.setText(properties.getClientCertificateAlias());

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
