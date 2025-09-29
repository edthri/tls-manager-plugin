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

package org.openintegrationengine.tlsmanager.server;

import com.kaurpalang.mirth.annotationsplugin.annotation.MirthServerClass;
import lombok.Getter;
import org.openintegrationengine.tlsmanager.server.connectorconfig.TLSHttpConfiguration;
import org.openintegrationengine.tlsmanager.server.connectorconfig.TLSTcpConfiguration;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.plugins.ServicePlugin;
import com.mirth.connect.server.controllers.ControllerFactory;
import org.openintegrationengine.tlsmanager.shared.SerializationController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@MirthServerClass
public class TLSServicePlugin implements ServicePlugin {

    @Getter
    private CertificateService certificateService;

    @Getter
    private SocketFactoryService socketFactoryService;

    @Override
    public void init(Properties properties) {
        var configurationController = ControllerFactory.getFactory().createConfigurationController();

        this.certificateService = new CertificateService();
        this.socketFactoryService = new SocketFactoryService(
            configurationController,
            certificateService
        );

        configurationController.saveProperty(
            "HTTP",
            "httpConfigurationClass",
            TLSHttpConfiguration.class.getCanonicalName()
        );

        configurationController.saveProperty(
            "TCP",
            "tcpConfigurationClass",
            TLSTcpConfiguration.class.getCanonicalName()
        );

        SerializationController.registerSerializableClasses();
    }

    @Override
    public void update(Properties properties) {
        // We don't need to do anything here.
    }

    @Override
    public Properties getDefaultProperties() {
        var defaultProperties = new Properties();
        defaultProperties.setProperty(TLSPluginConstants.PROPERTY_TRUST_BACKEND, "database");

        return defaultProperties;
    }

    @Override
    public ExtensionPermission[] getExtensionPermissions() {
        return new ExtensionPermission[]{};
    }

    @Override
    public Map<String, Object> getObjectsForSwaggerExamples() {
        return new HashMap<>();
    }

    @Override
    public String getPluginPointName() {
        return TLSPluginConstants.PLUGIN_POINTNAME;
    }

    @Override
    public void start() {
        this.certificateService.init();
    }

    @Override
    public void stop() { }

    public static TLSServicePlugin getPluginInstance() {
        var servicePlugin = ControllerFactory.getFactory()
            .createExtensionController()
            .getServicePlugins()
            .get(TLSPluginConstants.PLUGIN_POINTNAME);

        if (servicePlugin instanceof TLSServicePlugin tlsServicePlugin) {
            return tlsServicePlugin;
        } else {
            // well we shouldn't really get here
            throw new RuntimeException(
                "Plugin pointname '%s' does not point to an instance of %s class".formatted(
                    TLSPluginConstants.PLUGIN_POINTNAME,
                    TLSServicePlugin.class.getCanonicalName()
                )
            );
        }
    }
}
