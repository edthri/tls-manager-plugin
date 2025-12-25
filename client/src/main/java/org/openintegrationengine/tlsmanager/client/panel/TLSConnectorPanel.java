package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.ConnectorTypeDecoration;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.components.MirthEditableComboBox;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.client.ui.panels.connectors.ResponseHandler;
import com.mirth.connect.connectors.http.HttpDispatcherProperties;
import com.mirth.connect.connectors.http.HttpSender;
import com.mirth.connect.connectors.tcp.TcpDispatcherProperties;
import com.mirth.connect.connectors.tcp.TcpListener;
import com.mirth.connect.connectors.tcp.TcpSender;
import com.mirth.connect.connectors.ws.DefinitionServiceMap;
import com.mirth.connect.connectors.ws.WebServiceDispatcherProperties;
import com.mirth.connect.connectors.ws.WebServiceSender;
import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.Connector;
import org.openintegrationengine.tlsmanager.client.dialog.ItemPickerDialog;
import org.openintegrationengine.tlsmanager.client.misc.SwingMagic;
import org.openintegrationengine.tlsmanager.shared.models.ClientAuthMode;
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

public class TLSConnectorPanel extends AbstractTLSConnectorPropertiesPanel {

    /*
    Client mode UI components
     */
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

    /*
    Server mode UI components
     */
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

    /*
    Other crap
     */
    private TLSConnectorProperties properties;
    private boolean isServerMode;
    private final ResponseHandler responseHandler;

    private enum Transport {
        HTTP, TCP, WS
    }

    public TLSConnectorPanel() {
        this.properties = new TLSConnectorProperties();
        this.isServerMode = false;

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

    @Override
    protected void handleManagerEnabledButton(boolean managerEnabled) {
        properties.setTlsManagerEnabled(managerEnabled);

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

        if (isServerMode) {
            handleManagerEnabledButtonClientMode(false);
            handleManagerEnabledButtonServerMode(managerEnabled);
        } else {
            handleManagerEnabledButtonClientMode(managerEnabled);
            handleManagerEnabledButtonServerMode(false);
        }
    }

    private void handleManagerEnabledButtonClientMode(boolean managerEnabled) {
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
    }

    private void handleManagerEnabledButtonServerMode(boolean managerEnabled) {
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
    }

    @Override
    protected void handleCrlModeChange() {
        if (crlModeComboBox.getSelectedItem() instanceof RevocationMode revocationMode) {
            properties.setCrlMode(revocationMode);
        }
    }

    @Override
    protected void handleOcspModeChange() {
        if (ocspModeComboBox.getSelectedItem() instanceof RevocationMode revocationMode) {
            properties.setOcspMode(revocationMode);
        }
    }

    @Override
    protected void handleSubjectDnValidationModeChange() {
        if (subjectDnValidationModeComboBox.getSelectedItem() instanceof SubjectDnValidationMode validationMode) {
            properties.setSubjectDnValidationMode(validationMode);
            redrawState();
        }
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

    @Override
    protected void redrawState() {
        if (properties.isTlsManagerEnabled()) {
            managerEnabledRadioYes.setSelected(true);
        } else {
            managerEnabledRadioNo.setSelected(true);
        }

        subjectDnValidationModeComboBox.setSelectedItem(properties.getSubjectDnValidationMode());
        subjectDnValidationFilterTextField.setEnabled(properties.getSubjectDnValidationMode() != SubjectDnValidationMode.NONE);
        subjectDnValidationFilterTextField.setText(properties.getSubjectDnValidationFilter());

        crlModeComboBox.setSelectedItem(properties.getCrlMode());
        ocspModeComboBox.setSelectedItem(properties.getOcspMode());

        final var protocolsString = properties.isUseServerDefaultProtocols()
            ? "Server default: %s".formatted(supportedProtocols)
            : "%d selected".formatted(properties.getUsedProtocols().size());

        protocolsText.setText(protocolsString);

        final var ciphersString = properties.isUseServerDefaultCiphers()
            ? "Server default: %d selected".formatted(supportedCiphers.size())
            : "%d selected".formatted(properties.getUsedCiphers().size());

        ciphersText.setText(ciphersString);

        redrawClientModeState();
        redrawServerModeState();
    }

    private void redrawClientModeState() {
        if (properties.isServerCertificateValidationEnabled()) {
            serverCertificateValidationRadioYes.setSelected(true);
        } else {
            serverCertificateValidationRadioNo.setSelected(true);
        }

        trustedServerCertsText.setText(renderTrustTginfWhatevs());

        if (properties.isHostnameVerificationEnabled()) {
            hostnameValidationRadioYes.setSelected(true);
        } else {
            hostnameValidationRadioNo.setSelected(true);
        }

        clientCertText.setText(properties.getClientCertificateAlias());
    }

    private void redrawServerModeState() {
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

        trustedClientCertsText.setText(renderTrustTginfWhatevs());
    }

    private String renderTrustTginfWhatevs() {
        var thingsToTrust = new ArrayList<String>();
        if (properties.isTrustSystemTruststore()) {
            thingsToTrust.add("System Truststore");
        }

        if (properties.getTrustedServerCertificates() != null && !properties.getTrustedServerCertificates().isEmpty()) {
            var count = properties.getTrustedServerCertificates().size();
            var plural = (count == 1) ? "" : "s";
            thingsToTrust.add("%d certificate%s".formatted(count, plural));
        }

        return thingsToTrust.isEmpty()
            ? "None selected"
            : "Trusting %s".formatted(String.join(" and ", thingsToTrust)
        );
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
        registerActionListeners();
    }

    @Override
    public Component[][] getLayoutComponents() {
        return new Component[0][];
    }

    @Override
    public void setLayoutComponentsEnabled(boolean b) {}

    @Override
    public ConnectorTypeDecoration getConnectorTypeDecoration() {
        return new ConnectorTypeDecoration(Connector.Mode.DESTINATION);
    }

    @Override
    protected void initComponents() {
        super.initComponents();

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

        initClientModeComponents();
        initServerModeComponents();
    }

    private void initClientModeComponents() {
        serverCertificateValidationLabel = new JLabel("Server Certificate Validation:");
        final var serverCertificateValidationButtonGroup = new ButtonGroup();

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
        final var hostnameValidationButtonGroup = new ButtonGroup();

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
        clientCertText = new JLabel();
    }

    protected void initServerModeComponents() {
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

        final var clientAuthModeButtonGroup = new ButtonGroup();
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
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        initClientModeLayout();
        initServerModeLayout();
    }

    private void initClientModeLayout() {
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

    private void initServerModeLayout() {
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

    private void registerActionListeners() {
        var settingsPanel = connectorPanel.getConnectorSettingsPanel();

        // Register client/server mode listener
        if (settingsPanel instanceof TcpListener tcpListener) {
            tcpListener.modeServerRadio.addActionListener((event) -> handleTcpModeChange(true));
            tcpListener.modeClientRadio.addActionListener((event) -> handleTcpModeChange(false));

            this.isServerMode = tcpListener.modeServerRadio.isSelected();
            handleTcpModeChange(this.isServerMode);
        } else if (settingsPanel instanceof TcpSender tcpSender) {
            tcpSender.modeServerRadio.addActionListener((event) -> handleTcpModeChange(true));
            tcpSender.modeClientRadio.addActionListener((event) -> handleTcpModeChange(false));

            this.isServerMode = tcpSender.modeServerRadio.isSelected();
            handleTcpModeChange(this.isServerMode);
        }

        // Register Connection testing overrides
        if (settingsPanel instanceof HttpSender) {
            registerTestConnectionActionHandlers(settingsPanel, Transport.HTTP);
        } else if (settingsPanel instanceof TcpSender) {
            registerTestConnectionActionHandlers(settingsPanel, Transport.TCP);
        } else if (settingsPanel instanceof WebServiceSender) {

        }

    }

    private void handleTcpModeChange(boolean isServerMode) {
        this.isServerMode = isServerMode;
        if (isServerMode) {
            handleManagerEnabledButtonClientMode(false);
            handleManagerEnabledButtonServerMode(true);
        } else {
            handleManagerEnabledButtonClientMode(true);
            handleManagerEnabledButtonServerMode(false);
        }
    }

    private void registerTestConnectionActionHandlers(ConnectorSettingsPanel settingsPanel, Transport transport) {
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

    private void registerWsTestConnectionActionHandlers(ConnectorSettingsPanel settingsPanel) {
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
}
