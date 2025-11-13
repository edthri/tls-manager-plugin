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
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ExtensionController;
import com.mirth.connect.server.util.TemplateValueReplacer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.server.connectorconfig.TLSHttpConfiguration;
import org.openintegrationengine.tlsmanager.server.connectorconfig.TLSTcpConfiguration;
import org.openintegrationengine.tlsmanager.server.connectorconfig.TLSWebServiceConfiguration;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.plugins.ServicePlugin;
import com.mirth.connect.server.controllers.ControllerFactory;
import org.openintegrationengine.tlsmanager.shared.SerializationController;
import org.openintegrationengine.tlsmanager.shared.models.TLSPluginConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@MirthServerClass
@Slf4j
public class TLSServicePlugin implements ServicePlugin {

    public static final String PLUGIN_POINT_NAME = "TLS Manager Service Plugin";

    @Getter
    private CertificateService certificateService;

    @Getter
    private SocketFactoryService socketFactoryService;

    @Getter
    private WebServiceService webServiceService;

    @Override
    public void init(Properties properties) {
        var configurationController = ControllerFactory.getFactory().createConfigurationController();

        this.certificateService = new CertificateService();
        this.socketFactoryService = new SocketFactoryService(
            configurationController,
            certificateService
        );

        this.webServiceService = new WebServiceService(
            socketFactoryService,
            new TemplateValueReplacer()
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

        configurationController.saveProperty(
            "WS",
            "wsConfigurationClass",
            TLSWebServiceConfiguration.class.getCanonicalName()
        );

        SerializationController.registerSerializableClasses();

        installWar(configurationController);
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
        return PLUGIN_POINT_NAME;
    }

    @Override
    public void start() {
        var pluginConfiguration = TLSPluginConfiguration.fromEnv();
        this.certificateService.init(pluginConfiguration);
    }

    @Override
    public void stop() { }

    public static TLSServicePlugin getPluginInstance() {
        var servicePlugin = ControllerFactory.getFactory()
            .createExtensionController()
            .getServicePlugins()
            .get(PLUGIN_POINT_NAME);

        if (servicePlugin instanceof TLSServicePlugin tlsServicePlugin) {
            return tlsServicePlugin;
        } else {
            // well we shouldn't really get here
            var ex = new RuntimeException(
                "Plugin pointname '%s' does not point to an instance of %s class".formatted(
                    PLUGIN_POINT_NAME,
                    TLSServicePlugin.class.getCanonicalName()
                )
            );

            log.error("Error fetching plugin instance", ex);
            throw ex;
        }
    }

    private void installWar(ConfigurationController configurationController) {
        var webappsPath = Path.of(configurationController.getBaseDir(), "webapps");
        var warPath = Path.of(webappsPath.toString(), "tls-manager.war");

        if (!webappsPath.toFile().exists()) {
            log.debug("Webapps directory does not exist. Creating...");
            if (!webappsPath.toFile().mkdirs()) {
                throw new IllegalStateException("Failed to create webapps directory at " + webappsPath);
            }
        }

        var warFile = warPath.toFile();

        if (warFile.exists()) {
            log.debug("TLS Manager WAR already exists at {}. Deleting...", warPath);
            if (!warFile.delete()) {
                throw new IllegalStateException("Failed to delete TLS Manager WAR at " + warPath);
            }
        }

        var pluginDirectoryPath = Path.of(ExtensionController.getExtensionsPath(), "tls-manager", "tls-manager.war");

        log.debug("Copying TLS Manager WAR from {} to {}", pluginDirectoryPath, warPath);
        try {
            Files.copy(pluginDirectoryPath, warPath);
            log.debug("TLS Manager WAR copied successfully");
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to copy TLS Manager WAR from %s to %s".formatted(pluginDirectoryPath, warPath),
                e
            );
        }
    }
}
