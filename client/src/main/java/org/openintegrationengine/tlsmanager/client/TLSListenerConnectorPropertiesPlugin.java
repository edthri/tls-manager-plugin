// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: 2025 NovaMap Health Limited <https://novamap.health>

package org.openintegrationengine.tlsmanager.client;

import com.kaurpalang.mirth.annotationsplugin.annotation.MirthClientClass;
import com.mirth.connect.client.ui.AbstractConnectorPropertiesPanel;
import com.mirth.connect.plugins.ConnectorPropertiesPlugin;
import org.openintegrationengine.tlsmanager.client.panel.ListenerConnectorPropertiesPanel;
import org.openintegrationengine.tlsmanager.shared.SerializationController;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import java.util.Set;

//@MirthClientClass
public class TLSListenerConnectorPropertiesPlugin extends ConnectorPropertiesPlugin {

    public TLSListenerConnectorPropertiesPlugin(String name) {
        super(name);
        SerializationController.registerSerializableClasses();
    }

    @Override
    public String getSettingsTitle() {
        return "TLS Settings";
    }

    @Override
    public AbstractConnectorPropertiesPanel getConnectorPropertiesPanel() {
        return new ListenerConnectorPropertiesPanel();
    }

    @Override
    public boolean isSupported(String transportName) {
        return Set
            .of("HTTP Listener", "TCP Listener", "Web Service Listener")
            .contains(transportName);
    }

    @Override
    public boolean isConnectorPropertiesPluginSupported(String pluginPointName) {
        return false;
    }

    @Override
    public String getPluginPointName() {
        return TLSPluginConstants.TLS_LISTENER_CONNECTOR_PROPERTIES_PLUGIN_POINT_NAME;
    }
}
