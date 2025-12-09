package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.ui.AbstractConnectorPropertiesPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Color;
import java.time.Instant;

public abstract class AbstractTLSConnectorPropertiesPanel extends AbstractConnectorPropertiesPanel {

    protected final ImageIcon wrenchIcon;

    protected JLabel managerEnabledLabel;
    protected MirthRadioButton managerEnabledRadioYes;
    protected MirthRadioButton managerEnabledRadioNo;

    AbstractTLSConnectorPropertiesPanel() {
        this.wrenchIcon = new ImageIcon(Frame.class.getResource("images/wrench.png"));
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
    }

    protected void initLayout() {
        setLayout(new MigLayout("insets 0, novisualpadding, hidemode 3", "[]12[]", ""));

        add(managerEnabledLabel, "newline, right");
        add(managerEnabledRadioYes, "split");
        add(managerEnabledRadioNo);
    }

    protected static void log(String message) {
        System.out.printf("%s - %s.%n", Instant.now(), message);
    }

    protected abstract void handleManagerEnabledButton(boolean managerEnabled);
}
