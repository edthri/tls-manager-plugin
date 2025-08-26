/*
 * Copyright 2021 Kaur Palang
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

package org.openintegrationengine.sslmanager.server;

import com.kaurpalang.mirth.annotationsplugin.annotation.MirthServerClass;
import lombok.Getter;
import org.openintegrationengine.sslmanager.server.connectorconfig.TLSHttpConfiguration;
import org.openintegrationengine.sslmanager.shared.SSLPluginConstants;
import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.plugins.ServicePlugin;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@MirthServerClass
public class SSLServicePlugin implements ServicePlugin {

    private ConfigurationController configurationController;

    @Getter
    private CertificateService certificateService;

    @Override
    public void init(Properties properties) {
        this.configurationController = ControllerFactory.getFactory().createConfigurationController();

        this.certificateService = new CertificateService();

        configurationController.saveProperty(
            "HTTP",
            "httpConfigurationClass",
            TLSHttpConfiguration.class.getCanonicalName()
        );
    }

    @Override
    public void update(Properties properties) {
        // We don't need to do anything here.
    }

    @Override
    public Properties getDefaultProperties() {
        var defaultProperties = new Properties();
        defaultProperties.setProperty(SSLPluginConstants.PROPERTY_TRUST_BACKEND, "database");

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
        return SSLPluginConstants.PLUGIN_POINTNAME;
    }

    @Override
    public void start() {
        this.certificateService.init();
    }

    @Override
    public void stop() { }
}
