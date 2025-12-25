// This file uses Phosphor Icons (https://github.com/phosphor-icons)
// Copyright (c) 2023 Phosphor Icons
// Licensed under the MIT License

package org.openintegrationengine.tlsmanager.client.panel;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.BareBonesBrowserLaunch;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.plugins.SettingsPanelPlugin;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;

public class TLSManagerPanel extends AbstractSettingsPanel {

    // https://github.com/phosphor-icons/core/blob/main/raw/duotone/gear-duotone.svg
    private static final String SETTINGS_ICON_PATH = "images/tls_plugin_settings.png";

    private JPanel infoPanel;

    public TLSManagerPanel(String tabName, SettingsPanelPlugin plugin) {
        super(tabName);
        setVisibleTasks(0, 1, false);
        addTask(
            "openManagerInBrowser",
            "Open TLS Manager",
            "Launch the Web TLS Manager inside your system browser",
            "",
            new ImageIcon(this.getClass().getClassLoader().getResource(SETTINGS_ICON_PATH)));
        initComponents();
        initLayout();
    }

    private void initComponents() {
        setBackground(UIConstants.BACKGROUND_COLOR);

        infoPanel = new JPanel();
        infoPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        infoPanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)),
                "TLS Manager Plugin", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font(Font.SANS_SERIF, Font.BOLD, 11)
            )
        );
    }

    private void initLayout() {
        setLayout(new MigLayout("hidemode 3, novisualpadding, insets 12", "[grow]"));

        infoPanel.setLayout(new MigLayout("hidemode 3, novisualpadding, insets 0", "12[right][left]"));

        add(infoPanel, "growx, sx, wrap");
    }

    @Override
    public void doRefresh() {

    }

    @Override
    public boolean doSave() {
        return false;
    }

    /**
     * No touch! I know IntelliJ marks this function as unused, but OIE does this stupid callback reflection crap where callback function names
     * are defined as strings. This function is used to... open the TLS Manager in a browser... I know, right!
     */
    public void openManagerInBrowser() {
        BareBonesBrowserLaunch.openURL(PlatformUI.SERVER_URL + "/tls-manager");
    }
}
