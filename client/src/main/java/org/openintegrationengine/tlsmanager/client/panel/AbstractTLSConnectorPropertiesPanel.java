package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.ui.AbstractConnectorPropertiesPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.components.MirthTextField;
import net.miginfocom.swing.MigLayout;
import org.openintegrationengine.tlsmanager.client.misc.DisplayTextEnumModeComboBoxRenderer;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Color;
import java.time.Instant;

public abstract class AbstractTLSConnectorPropertiesPanel extends AbstractConnectorPropertiesPanel {

    protected final ImageIcon wrenchIcon;
    protected final Frame parentFrame;

    protected JLabel managerEnabledLabel;
    protected MirthRadioButton managerEnabledRadioYes;
    protected MirthRadioButton managerEnabledRadioNo;

    protected JLabel subjectDnValidationLabel;
    protected MirthComboBox<SubjectDnValidationMode> subjectDnValidationModeComboBox;
    protected MirthTextField subjectDnValidationFilterTextField;

    protected JLabel crlModeLabel;
    protected MirthComboBox<RevocationMode> crlModeComboBox;

    protected JLabel ocspModeLabel;
    protected MirthComboBox<RevocationMode> ocspModeComboBox;

    protected JLabel protocolsLabel;
    protected JButton protocolsButton;
    protected JLabel protocolsText;

    protected JLabel ciphersLabel;
    protected JButton ciphersButton;
    protected JLabel ciphersText;

    AbstractTLSConnectorPropertiesPanel() {
        this.wrenchIcon = new ImageIcon(Frame.class.getResource("images/wrench.png"));
        this.parentFrame = PlatformUI.MIRTH_FRAME;
    }

    protected void initComponents() {
        setBackground(UIConstants.BACKGROUND_COLOR);

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
        // TODO addKeyListener

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
        // TODO addActionListener

        protocolsText = new JLabel();

        ciphersLabel = new JLabel("Enabled Ciphers:");
        ciphersButton = new JButton(wrenchIcon);
        // TODO addActionListener

        ciphersText = new JLabel();
    }

    protected void initLayout() {
        setLayout(new MigLayout("insets 0, novisualpadding, hidemode 3", "[]12[]", ""));

        add(managerEnabledLabel, "newline, right");
        add(managerEnabledRadioYes, "split");
        add(managerEnabledRadioNo);

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

    protected abstract void handleManagerEnabledButton(boolean managerEnabled);

    protected abstract void handleCrlModeChange();
    protected abstract void handleOcspModeChange();
    protected abstract void handleSubjectDnValidationModeChange();

    protected static void log(String message) {
        System.out.printf("%s - %s.%n", Instant.now(), message);
    }
}
