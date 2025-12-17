package org.openintegrationengine.tlsmanager.client;

import com.kaurpalang.mirth.annotationsplugin.annotation.MirthClientClass;
import com.mirth.connect.client.core.TaskConstants;
import com.mirth.connect.client.ui.BareBonesBrowserLaunch;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.components.MirthTreeTable;
import com.mirth.connect.plugins.TaskPlugin;
import org.jdesktop.swingx.JXTaskPane;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import javax.swing.*;

@MirthClientClass
public class TLSTaskPlugin extends TaskPlugin {

    // https://github.com/phosphor-icons/core/blob/main/raw/duotone/gear-duotone.svg
    private static final String SETTINGS_ICON_PATH = "images/tls_plugin_settings.png";

    private final Frame parent;
    private final JXTaskPane tlsTaskPane;

    private static final String OPEN_MANAGER_CALLBACK_NAME = "openManagerInBrowser";

    public TLSTaskPlugin(String name) {
        super(name);

        this.parent = PlatformUI.MIRTH_FRAME;

        tlsTaskPane = new JXTaskPane();
        tlsTaskPane.setTitle("TLS Manager Tasks");
        tlsTaskPane.setName(TaskConstants.CHANNEL_KEY);
        tlsTaskPane.setFocusable(false);

        parent.addTask(
            OPEN_MANAGER_CALLBACK_NAME,
            "Open TLS Manager",
            "Open the TLS Manager UI in browser",
            "",
            new ImageIcon(this.getClass().getClassLoader().getResource(SETTINGS_ICON_PATH)),
            tlsTaskPane,
            null,
            this
        );

        parent.setNonFocusable(tlsTaskPane);
        parent.taskPaneContainer.add(tlsTaskPane, parent.taskPaneContainer.getComponentCount() - 1);
        tlsTaskPane.setVisible(true);
    }

    @Override
    public void onRowSelected(MirthTreeTable mirthTreeTable) {

    }

    @Override
    public void onRowDeselected() {

    }

    @Override
    public JXTaskPane getTaskPane() {
        return tlsTaskPane;
    }

    @Override
    public String getPluginPointName() {
        return TLSPluginConstants.TLS_TASK_PLUGIN_POINT_NAME;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void reset() {

    }

    /**
     * No touch! I know IntelliJ marks this function as unused, but OIE does this stupid callback reflection crap where callback function names
     * are defined as strings. This function is used to... open the TLS Manager in browser... I know, right!
     */
    public void openManagerInBrowser() {
        BareBonesBrowserLaunch.openURL(PlatformUI.SERVER_URL + "/tls-manager");
    }
}
