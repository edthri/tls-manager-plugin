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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.server.backend.DatabaseTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.FileTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.SystemTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.TrustStoreBackend;
import org.openintegrationengine.tlsmanager.shared.PersistenceMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public final class CertificateService {

    @Getter
    private KeyStore systemTrustStore;

    @Getter
    private KeyStore truststore;

    @Getter
    private KeyStore keystore;

    private TrustStoreBackend systemTrustStoreBackend;
    private TrustStoreBackend extraTrustStoreBackend;

    public CertificateService() {
    }

    void init() {
        systemTrustStoreBackend = new SystemTrustStoreBackend();

        var persistenceMode = getPersistenceMode();

        if (persistenceMode == PersistenceMode.DATABASE) {
            extraTrustStoreBackend = new DatabaseTrustStoreBackend();
        } else if (persistenceMode == PersistenceMode.FILESYSTEM) {
            extraTrustStoreBackend = new FileTrustStoreBackend("/certs/truststore.p12");
        } else {
            // Should not get here
            throw new RuntimeException("Unsupported persistence mode: " + persistenceMode);
        }

        byte[] cacerts = systemTrustStoreBackend.load();
        byte[] extraTrustStore = extraTrustStoreBackend.load();

        try {
            systemTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        loadKeyStore(systemTrustStore, cacerts, systemTrustStoreBackend.loadPassword());
        loadKeyStore(truststore, extraTrustStore, "changeit".toCharArray());
    }

    KeyStore getTrustStoreFromProperties(boolean isTrustSystem, Set<String> aliasSet) {
        try {
            KeyStore finalTrustStore;

            if (isTrustSystem) {
                finalTrustStore = clone(systemTrustStore);
            } else {
                finalTrustStore = KeyStore.getInstance("PKCS12");
                finalTrustStore.load(null, "supabase".toCharArray());
            }

            var presentInSystem = new HashSet<String>();
            var unknownAliases = new HashSet<String>();
            for (String alias : aliasSet) {
                try {
                    if (systemTrustStore.containsAlias(alias)) {
                        presentInSystem.add(alias);
                        continue;
                    }

                    if (!truststore.containsAlias(alias)) {
                        unknownAliases.add(alias);
                        continue;
                    }

                    var publicCertificate = truststore.getCertificate(alias);
                    finalTrustStore.setCertificateEntry(alias, publicCertificate);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }

            // TODO Connector data
            if (!presentInSystem.isEmpty()) {
                log.warn("Generating effective TrustStore for connector {}. Found and ignored aliases present in system truststore: {}", "connectorId", presentInSystem);
            }

            if (!unknownAliases.isEmpty()) {
                log.warn("Generating effective TrustStore for connector {}. Found aliases not present in additional truststore: {}", "connectorId", unknownAliases);
            }

            return finalTrustStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadKeyStore(KeyStore keystore, byte[] bytes, char[] password) {
        try {
            try (var bais = new ByteArrayInputStream(bytes)) {
                keystore.load(bais, password);
            }
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getLoadedAliases() {
        try {
            return new HashSet<>(Collections.list(truststore.aliases()));
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private PersistenceMode getPersistenceMode() {
        var persistenceModeFromEnv = System.getenv("OIE_TLS_PLUGIN_PERSISTENCE_BACKEND");

        if (persistenceModeFromEnv == null) {
            throw new RuntimeException("OIE_TLS_PLUGIN_PERSISTENCE_BACKEND is not set");
        }

        var persistenceMode = PersistenceMode.valueOf(persistenceModeFromEnv.toUpperCase());

        log.info("Using persistence mode {}", persistenceMode);

        return persistenceMode;
    }

    /**
     * Perform a byte-level clone of a KeyStore object
     *
     * @param keystore The KeyStore object to be cloned
     * @return Byte-level clone of the provided KeyStore
     */
    private KeyStore clone(KeyStore keystore) {
        try (var outStream = new ByteArrayOutputStream()) {
            var finalTrustStore = KeyStore.getInstance("PKCS12");

            keystore.store(outStream, "sup3rS3cr1t!".toCharArray());

            try (var inStream = new ByteArrayInputStream(outStream.toByteArray())) {
                finalTrustStore.load(inStream, "sup3rS3cr1t!".toCharArray());
            }

            return finalTrustStore;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
