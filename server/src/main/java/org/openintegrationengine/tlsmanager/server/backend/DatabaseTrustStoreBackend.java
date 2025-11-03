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
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

@Slf4j
public class DatabaseTrustStoreBackend implements TrustStoreBackend {

    private final ConfigurationController configurationController;

    private final String databaseColumn;

    public DatabaseTrustStoreBackend(String databaseColumn) {
        this.configurationController = ControllerFactory.getFactory().createConfigurationController();
        this.databaseColumn = databaseColumn;
    }

    @Override
    public boolean persist(byte[] keystore) {
        var encoder = Base64.getEncoder();
        var b64Keystore = encoder.encodeToString(keystore);
        configurationController.saveProperty(TLSPluginConstants.PLUGIN_POINTNAME, databaseColumn, b64Keystore);
        return false;
    }

    @Override
    public void init() {
        var keystoreBytes = configurationController.getProperty(TLSPluginConstants.PLUGIN_POINTNAME, databaseColumn);
        if (keystoreBytes != null) {
            log.debug("Using existing keystore from config column {}", databaseColumn);
            return;
        }

        try {
            var keystore = KeyStore.getInstance(TLSPluginConstants.PKCS12);
            keystore.load(null, new char[] {});

            try (var baos = new ByteArrayOutputStream()) {
                keystore.store(baos, new char[] {});
                persist(baos.toByteArray());
            }

        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            log.error("Error initializing keystore", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] load() {
        var decoder = Base64.getDecoder();
        var keystoreBytes = configurationController.getProperty(TLSPluginConstants.PLUGIN_POINTNAME, databaseColumn);
        return decoder.decode(keystoreBytes);
    }

    @Override
    public char[] loadPassword() {
        return new char[0];
    }
}
