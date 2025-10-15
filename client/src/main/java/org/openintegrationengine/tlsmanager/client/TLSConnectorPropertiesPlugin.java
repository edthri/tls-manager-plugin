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

package org.openintegrationengine.tlsmanager.client;

import com.kaurpalang.mirth.annotationsplugin.annotation.MirthClientClass;
import com.mirth.connect.client.ui.AbstractConnectorPropertiesPanel;
import com.mirth.connect.plugins.ConnectorPropertiesPlugin;
import org.openintegrationengine.tlsmanager.client.panel.HTTPSenderConnectorPropertiesPanel;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.SerializationController;

import java.util.List;
import java.util.Set;

@MirthClientClass
public class TLSConnectorPropertiesPlugin extends ConnectorPropertiesPlugin {

    private final List<String> supportedConnectors;

    public TLSConnectorPropertiesPlugin(String name) {
        super(name);

        SerializationController.registerSerializableClasses();

        supportedConnectors = List.of(
            "HTTP Auth Connector Plugin Properties"
        );
    }

    @Override
    public String getSettingsTitle() {
        return "TLS Settings";
    }

    @Override
    public AbstractConnectorPropertiesPanel getConnectorPropertiesPanel() {
        return new HTTPSenderConnectorPropertiesPanel();
    }

    @Override
    public boolean isSupported(String transportName) {
        return Set
            .of("HTTP Sender", "TCP Sender", "Web Service Sender")
            .contains(transportName);
    }

    @Override
    public boolean isConnectorPropertiesPluginSupported(String pluginPointName) {
        return supportedConnectors.contains(pluginPointName);
    }

    @Override
    public String getPluginPointName() {
        return TLSPluginConstants.PLUGIN_POINTNAME;
    }
}
