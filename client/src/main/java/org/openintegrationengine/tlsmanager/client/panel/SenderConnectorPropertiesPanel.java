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

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.ConnectorTypeDecoration;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthEditableComboBox;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.components.MirthTextField;
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
import com.mirth.connect.util.MirthSSLUtil;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import org.openintegrationengine.tlsmanager.client.dialog.ItemPickerDialog;
import org.openintegrationengine.tlsmanager.client.misc.DisplayTextEnumModeComboBoxRenderer;
import org.openintegrationengine.tlsmanager.client.misc.SwingMagic;
import org.openintegrationengine.tlsmanager.shared.models.ConnectionTestResult;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;
import org.openintegrationengine.tlsmanager.shared.properties.TLSSenderProperties;
import org.openintegrationengine.tlsmanager.shared.servlet.TLSServletInterface;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Slf4j
public class SenderConnectorPropertiesPanel extends AbstractTLSConnectorPropertiesPanel {

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

    private JLabel subjectDnValidationLabel;
    private MirthComboBox<SubjectDnValidationMode> subjectDnValidationModeComboBox;
    private MirthTextField subjectDnValidationFilterTextField;

    private JLabel crlModeLabel;
    private MirthComboBox<RevocationMode> crlModeComboBox;

    private JLabel ocspModeLabel;
    private MirthComboBox<RevocationMode> ocspModeComboBox;

    private JLabel clientCertLabel;
    private JButton clientCertButton;
    private JLabel clientCertText;

    private JLabel protocolsLabel;
    private JButton protocolsButton;
    private JLabel protocolsText;

    private JLabel ciphersLabel;
    private JButton ciphersButton;
    private JLabel ciphersText;

    private TLSSenderProperties properties;
    private Set<String> publicCertificates;
    private Set<String> clientCertificates;
    private Set<String> supportedProtocols;
    private Set<String> supportedCiphers;

    private final Frame parentFrame;
    private enum Transport { HTTP, TCP, WS };

    private final ResponseHandler responseHandler;

    public SenderConnectorPropertiesPanel() {
        this.parentFrame = PlatformUI.MIRTH_FRAME;

        this.properties = new TLSSenderProperties();
        this.publicCertificates = new HashSet<>();
        this.clientCertificates = new HashSet<>();

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
    public TLSSenderProperties getProperties() {
        return properties.clone();
    }

    @Override
    public void setProperties(ConnectorProperties connectorProperties, ConnectorPluginProperties connectorPluginProperties, Connector.Mode mode, String s) {
        if (connectorPluginProperties instanceof TLSSenderProperties tlsSenderProperties) {
            this.properties = tlsSenderProperties;
            fetchData();
            redrawState();
            handleManagerEnabledButton(tlsSenderProperties.isTlsManagerEnabled());
        }
    }

    @Override
    public ConnectorPluginProperties getDefaults() {
        return new TLSSenderProperties();
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
        managerEnabledLabel = new JLabel("Use TLS Manager:");
        var managerEnabledButtonGroup = new ButtonGroup();

        managerEnabledRadioYes = new MirthRadioButton();
        managerEnabledRadioYes.setText("Yes");
        managerEnabledRadioYes.setBackground(Color.white);
        managerEnabledRadioYes.addActionListener(e -> handleManagerEnabledButton(true));
        managerEnabledButtonGroup.add(managerEnabledRadioYes);

        managerEnabledRadioNo = new MirthRadioButton();
        managerEnabledRadioNo.setText("No");
        managerEnabledRadioNo.setBackground(Color.white);
        managerEnabledRadioNo.addActionListener(e -> handleManagerEnabledButton(false));
        managerEnabledButtonGroup.add(managerEnabledRadioNo);

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


        var comboBoxRenderer = new DisplayTextEnumModeComboBoxRenderer();

        var subjectDnValidationModeModel = new SubjectDnValidationMode[]{
            SubjectDnValidationMode.NONE,
            SubjectDnValidationMode.PARTIAL,
            SubjectDnValidationMode.EXACT,
        };

        subjectDnValidationLabel = new JLabel("Subject DN Validation Mode:");
        subjectDnValidationModeComboBox = new MirthComboBox<>();
        subjectDnValidationModeComboBox.setRenderer(comboBoxRenderer);
        subjectDnValidationModeComboBox.setModel(new DefaultComboBoxModel<>(subjectDnValidationModeModel));
        subjectDnValidationModeComboBox.addActionListener(evt -> handleSubjectDnValidationModeChange());

        subjectDnValidationFilterTextField = new MirthTextField();
        subjectDnValidationFilterTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                properties.setSubjectDnValidationFilter(subjectDnValidationFilterTextField.getText());
            }
        });

        var revocationModeModel = new RevocationMode[]{
            RevocationMode.DISABLED,
            RevocationMode.SOFT_FAIL,
            RevocationMode.HARD_FAIL
        };

        crlModeLabel = new JLabel("CRL Mode:");
        crlModeComboBox = new MirthComboBox<>();
        crlModeComboBox.setRenderer(comboBoxRenderer);
        crlModeComboBox.setModel(new DefaultComboBoxModel<>(revocationModeModel));
        crlModeComboBox.addActionListener(evt -> handleCrlModeChange());

        ocspModeLabel = new JLabel("OCSP Mode:");
        ocspModeComboBox = new MirthComboBox<>();
        ocspModeComboBox.setRenderer(comboBoxRenderer);
        ocspModeComboBox.setModel(new DefaultComboBoxModel<>(revocationModeModel));
        ocspModeComboBox.addActionListener(evt -> handleOcspModeChange());

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

        protocolsLabel = new JLabel("Enabled Protocols:");
        protocolsButton = new JButton(wrenchIcon);
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
        protocolsText = new JLabel();

        ciphersLabel = new JLabel("Enabled Ciphers:");
        ciphersButton = new JButton(wrenchIcon);
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
        ciphersText = new JLabel();

        super.initComponents();
    }

    protected void initLayout() {
        setLayout(new MigLayout("insets 0, novisualpadding, hidemode 3", "[]12[]", ""));

        add(managerEnabledLabel, "newline, right");
        add(managerEnabledRadioYes, "split");
        add(managerEnabledRadioNo);

        add(serverCertificateValidationLabel, "newline, right");
        add(serverCertificateValidationRadioYes, "split");
        add(serverCertificateValidationRadioNo);

        add(subjectDnValidationLabel, "newline, right");
        add(subjectDnValidationModeComboBox, "split");
        add(subjectDnValidationFilterTextField, "w 168!");

        add(crlModeLabel, "newline, right");
        add(crlModeComboBox);

        add(ocspModeLabel, "newline, right");
        add(ocspModeComboBox);

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

    private void handleSubjectDnValidationModeChange() {
        if (subjectDnValidationModeComboBox.getSelectedItem() instanceof SubjectDnValidationMode validationMode) {
            properties.setSubjectDnValidationMode(validationMode);
            redrawState();
        }
    }

    private void handleCrlModeChange() {
        if (crlModeComboBox.getSelectedItem() instanceof RevocationMode revocationMode) {
            properties.setCrlMode(revocationMode);
        }
    }

    private void handleOcspModeChange() {
        if (ocspModeComboBox.getSelectedItem() instanceof RevocationMode revocationMode) {
            properties.setOcspMode(revocationMode);
        }
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

    private void fetchData() {
        final var workingId = PlatformUI.MIRTH_FRAME.startWorking("Fetching certificates...");

        var worker = new SwingWorker<Void, Void>() {
            private Set<String> publicCertAliasSet;
            private Set<String> clientCertAliasSet;
            private Map<String, String[]> cryptoMap;

            public Void doInBackground() {
                try {
                    publicCertAliasSet = PlatformUI.MIRTH_FRAME.mirthClient.getServlet(TLSServletInterface.class).getPublicCertificates();
                    clientCertAliasSet = PlatformUI.MIRTH_FRAME.mirthClient.getServlet(TLSServletInterface.class).getClientCertificates();
                    cryptoMap = PlatformUI.MIRTH_FRAME.mirthClient.getProtocolsAndCipherSuites();
                } catch (Exception e) {
                    PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e, "Fetching imported certificates failed");
                }

                return null;
            }

            public void done() {
                publicCertificates = publicCertAliasSet;
                clientCertificates = clientCertAliasSet;

                supportedProtocols = Set.of(
                    cryptoMap.get(MirthSSLUtil.KEY_ENABLED_SERVER_PROTOCOLS)
                );

                supportedCiphers = Set.of(
                    cryptoMap.get(MirthSSLUtil.KEY_ENABLED_CIPHER_SUITES)
                );

                PlatformUI.MIRTH_FRAME.stopWorking(workingId);
            }
        };

        worker.execute();
    }
}
