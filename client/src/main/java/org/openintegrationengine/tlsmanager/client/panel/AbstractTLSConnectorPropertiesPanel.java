package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.ui.AbstractConnectorPropertiesPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.UIConstants;

import javax.swing.ImageIcon;
import java.time.Instant;

public abstract class AbstractTLSConnectorPropertiesPanel extends AbstractConnectorPropertiesPanel {

    protected final ImageIcon wrenchIcon;

    AbstractTLSConnectorPropertiesPanel() {
        this.wrenchIcon = new ImageIcon(Frame.class.getResource("images/wrench.png"));
    }


    protected void initComponents() {
        setBackground(UIConstants.BACKGROUND_COLOR);

    }

    protected void initLayout() {

    }

    protected static void log(String message) {
        System.out.printf("%s - %s.%n", Instant.now(), message);
    }
}
