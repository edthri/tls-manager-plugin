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
import org.openintegrationengine.tlsmanager.client.dialog.ItemPickerState;
import org.openintegrationengine.tlsmanager.shared.models.DefaultableList;
import org.openintegrationengine.tlsmanager.shared.properties.HttpConnectorProperties;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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

    private DefaultableList certPickerResult;

    public HTTPSenderConnectorPropertiesPanel() {
        initComponents();
        initLayout();
    }

    @Override
    public HttpConnectorProperties getProperties() {
        var props = new HttpConnectorProperties();

        props.setTlsManagerEnabled(managerEnabledRadioYes.isSelected());
        props.setServerCertificateValidationEnabled(serverCertificateValidationRadioYes.isSelected());
        props.setHostnameVerificationEnabled(hostnameValidationRadioYes.isSelected());

        return props;
    }

    @Override
    public void setProperties(ConnectorProperties connectorProperties, ConnectorPluginProperties connectorPluginProperties, Connector.Mode mode, String s) {
        if (connectorPluginProperties instanceof HttpConnectorProperties httpConnectorProperties) {
            certPickerResult = httpConnectorProperties.getServerCertificateConfiguration();

            if (httpConnectorProperties.isTlsManagerEnabled()) {
                managerEnabledRadioYes.setSelected(true);
            } else {
                managerEnabledRadioNo.setSelected(true);
            }

            if (httpConnectorProperties.isServerCertificateValidationEnabled()) {
                serverCertificateValidationRadioYes.setSelected(true);
            } else {
                serverCertificateValidationRadioNo.setSelected(true);
            }

            if (httpConnectorProperties.isHostnameVerificationEnabled()) {
                hostnameValidationRadioYes.setSelected(true);
            } else {
                hostnameValidationRadioNo.setSelected(true);
            }

            clientCertText.setText(httpConnectorProperties.getClientCertificateAlias());

            handleManagerEnabledButton(httpConnectorProperties.isTlsManagerEnabled());
            /*
            final String workingId = PlatformUI.MIRTH_FRAME.startWorking("Fetching TLS settings...");
            var worker = new SwingWorker<Void, Void>() {
                public Void doInBackground() {
                    var tlsServlet = PlatformUI.MIRTH_FRAME.mirthClient.getServlet(TLSServletInterface.class);

                    var importedCertificates = tlsServlet.getImportedCertificates();

                    itemPickerState = new ItemPickerState(
                        List.of("GoDaddy", "Some other CA"),
                        List.of("java"),
                        "[JVM Truststore]",
                        false
                    );

                    return null;
                }

                @Override
                public void done() {
                    PlatformUI.MIRTH_FRAME.setSaveEnabled(false);
                    PlatformUI.MIRTH_FRAME.stopWorking(workingId);
                }
            };

            worker.execute();
             */
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
        serverCertificateValidationButtonGroup.add(serverCertificateValidationRadioYes);

        serverCertificateValidationRadioNo = new MirthRadioButton();
        serverCertificateValidationRadioNo.setBackground(new Color(255, 255, 255));
        serverCertificateValidationRadioNo.setText("Disabled");
        serverCertificateValidationButtonGroup.add(serverCertificateValidationRadioNo);

        trustedServerCertsLabel = new JLabel("Trusted Server Certificates:");
        trustedServerCertsButton = new JButton(wrenchIcon);
        trustedServerCertsButton.addActionListener(e -> {

            var itemPickerState = new ItemPickerState(
                certPickerResult,
                List.of("GoDaddy", "Some other CA"),
                "[JVM Truststore]"
            );

            var itemPicker = new ItemPickerDialog(PlatformUI.MIRTH_FRAME, itemPickerState);

            if (itemPicker.isSaved()) {
                certPickerResult = itemPicker.getResults();
                PlatformUI.MIRTH_FRAME.setSaveEnabled(true);
            }
        });

        trustedServerCertsText = new JLabel("Trusting some certs as a placeholder");

        hostnameValidationLabel = new JLabel("Hostname verification:");
        var hostnameValidationButtonGroup = new ButtonGroup();

        hostnameValidationRadioYes = new MirthRadioButton();
        hostnameValidationRadioYes.setBackground(new Color(255, 255, 255));
        hostnameValidationRadioYes.setText("Enabled");
        hostnameValidationButtonGroup.add(hostnameValidationRadioYes);

        hostnameValidationRadioNo = new MirthRadioButton();
        hostnameValidationRadioNo.setBackground(new Color(255, 255, 255));
        hostnameValidationRadioNo.setText("Disabled");
        hostnameValidationButtonGroup.add(hostnameValidationRadioNo);

        clientCertLabel = new JLabel("Client Certificate:");
        clientCertButton = new JButton(wrenchIcon);
        clientCertButton.addActionListener(e -> System.out.println("client button"));
        clientCertText = new JLabel("myclientcert");

        protocolsLabel = new JLabel("Enabled Protocols:");
        protocolsButton = new JButton(wrenchIcon);
        protocolsButton.addActionListener(e -> System.out.println("protocols button"));
        protocolsText = new JLabel("Server default: TLSv4.6");

        ciphersLabel = new JLabel("Enabled Ciphers:");
        ciphersButton = new JButton(wrenchIcon);
        ciphersButton.addActionListener(e -> System.out.println("ciphers button"));
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

    }
}
