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

package org.openintegrationengine.tlsmanager.server.backend;

import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import java.util.Base64;

public class DatabaseTrustStoreBackend implements TrustStoreBackend {

    private ConfigurationController configurationController;

    public DatabaseTrustStoreBackend() {
        this.configurationController = ControllerFactory.getFactory().createConfigurationController();
    }

    @Override
    public boolean persist(byte[] keystore) {
        var encoder = Base64.getEncoder();
        var b64Keystore = encoder.encodeToString(keystore);
        configurationController.saveProperty(TLSPluginConstants.PLUGIN_POINTNAME, "additionalKeystore", b64Keystore);
        return false;
    }

    @Override
    public byte[] load() {
        var decoder = Base64.getDecoder();
        var keystoreBytes = configurationController.getProperty(TLSPluginConstants.PLUGIN_POINTNAME, "additionalKeystore");
        return decoder.decode(keystoreBytes);
    }

    @Override
    public char[] loadPassword() {
        return new char[0];
    }
}
