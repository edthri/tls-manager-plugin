package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.Connector;
import com.mirth.connect.util.MirthSSLUtil;
import net.miginfocom.swing.MigLayout;
import org.openintegrationengine.tlsmanager.client.dialog.ItemPickerDialog;
import org.openintegrationengine.tlsmanager.client.misc.DisplayTextEnumModeComboBoxRenderer;
import org.openintegrationengine.tlsmanager.shared.models.ClientAuthMode;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;
import org.openintegrationengine.tlsmanager.shared.properties.TLSListenerProperties;
import org.openintegrationengine.tlsmanager.shared.servlet.TLSServletInterface;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class ListenerConnectorPropertiesPanel extends AbstractTLSConnectorPropertiesPanel {

    private JLabel managerEnabledLabel;
    private MirthRadioButton managerEnabledRadioYes;
    private MirthRadioButton managerEnabledRadioNo;

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

    private JLabel subjectDnValidationLabel;
    private MirthComboBox<SubjectDnValidationMode> subjectDnValidationModeComboBox;
    private MirthTextField subjectDnValidationFilterTextField;

    private JLabel crlModeLabel;
    private MirthComboBox<RevocationMode> crlModeComboBox;

    private JLabel ocspModeLabel;
    private MirthComboBox<RevocationMode> ocspModeComboBox;

    private JLabel protocolsLabel;
    private JButton protocolsButton;
    private JLabel protocolsText;

    private JLabel ciphersLabel;
    private JButton ciphersButton;
    private JLabel ciphersText;

    private TLSListenerProperties properties;

    private Set<String> publicCertificates;
    private Set<String> serverCertificates;
    private Set<String> supportedProtocols;
    private Set<String> supportedCiphers;

    public ListenerConnectorPropertiesPanel() {
        this.properties = new TLSListenerProperties();

        this.publicCertificates = new HashSet<>();
        this.serverCertificates = new HashSet<>();
        this.supportedProtocols = new HashSet<>();
        this.supportedCiphers = new HashSet<>();

        initComponents();
        initLayout();
        fetchData();
    }

    @Override
    public TLSListenerProperties getProperties() {
        return properties.clone();
    }

    @Override
    public void setProperties(ConnectorProperties connectorProperties, ConnectorPluginProperties connectorPluginProperties, Connector.Mode mode, String s) {
        if (connectorPluginProperties instanceof TLSListenerProperties tlsListenerProperties) {
            this.properties = tlsListenerProperties;
            fetchData();
            redrawState();
            handleManagerEnabledButton(tlsListenerProperties.isTlsManagerEnabled());
        }
    }

    @Override
    public ConnectorPluginProperties getDefaults() {
        return new TLSListenerProperties();
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
                serverCertificates,
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
        protocolsText = new JLabel("Server default: TLSv4.6");

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
        ciphersText = new JLabel("Server default: 22 enabled");

        super.initComponents();
    }

    protected void initLayout() {
        setLayout(new MigLayout("insets 0, novisualpadding, hidemode 3", "[]12[]", ""));

        add(managerEnabledLabel, "newline, right");
        add(managerEnabledRadioYes, "split");
        add(managerEnabledRadioNo);

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

        add(subjectDnValidationLabel, "newline, right");
        add(subjectDnValidationModeComboBox, "split");
        add(subjectDnValidationFilterTextField, "w 168!");

        add(crlModeLabel, "newline, right");
        add(crlModeComboBox);

        add(ocspModeLabel, "newline, right");
        add(ocspModeComboBox);

        add(protocolsLabel, "newline, right");
        add(protocolsButton, "h 22!, w 22!, split");
        add(protocolsText);

        add(ciphersLabel, "newline, right");
        add(ciphersButton, "h 22!, w 22!, split");
        add(ciphersText);
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

    private void redrawState() {
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

    private void fetchData() {
        final var workerId = PlatformUI.MIRTH_FRAME.startWorking("Fetching data...");

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
                serverCertificates = clientCertAliasSet;
                publicCertificates = publicCertAliasSet;
                supportedProtocols = Set.of(
                    cryptoMap.get(MirthSSLUtil.KEY_ENABLED_SERVER_PROTOCOLS)
                );

                supportedCiphers = Set.of(
                    cryptoMap.get(MirthSSLUtil.KEY_ENABLED_CIPHER_SUITES)
                );

                PlatformUI.MIRTH_FRAME.stopWorking(workerId);
            }
        };

        worker.execute();
    }
}
