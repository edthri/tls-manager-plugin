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

import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.util.TemplateValueReplacer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.server.backend.DatabaseTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.FileTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.SystemTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.TrustStoreBackend;
import org.openintegrationengine.tlsmanager.shared.PersistenceMode;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

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

import static org.openintegrationengine.tlsmanager.shared.TLSPluginConstants.PKCS12;

@Slf4j
public final class CertificateService {

    @Getter
    private KeyStore systemTrustStore;

    @Getter
    private KeyStore externalTrustStore;

    @Getter
    private KeyStore keystore;

    private TrustStoreBackend systemTrustStoreBackend;
    private TrustStoreBackend extraTrustStoreBackend;

    private TemplateValueReplacer templateValueReplacer;

    public CertificateService() {
        this(
            new TemplateValueReplacer()
        );
    }

    public CertificateService(
        TemplateValueReplacer templateValueReplacer
    ) {
        this.templateValueReplacer = templateValueReplacer;
    }

    void init() {
        systemTrustStoreBackend = new SystemTrustStoreBackend();

        var persistenceMode = getPersistenceMode();

        if (persistenceMode == PersistenceMode.DATABASE) {
            extraTrustStoreBackend = new DatabaseTrustStoreBackend();
        } else if (persistenceMode == PersistenceMode.FILESYSTEM) {
            var truststorePath = System.getenv(TLSPluginConstants.ENV_PERSISTENCE_FS_STOREPATH);
            extraTrustStoreBackend = new FileTrustStoreBackend(truststorePath);
        } else {
            // Should not get here
            throw new RuntimeException("Unsupported persistence mode: " + persistenceMode);
        }

        extraTrustStoreBackend.init();

        byte[] cacertsBytes = systemTrustStoreBackend.load();
        byte[] extraTrustStoreBytes = extraTrustStoreBackend.load();

        try {
            systemTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            externalTrustStore = KeyStore.getInstance(PKCS12);
        } catch (KeyStoreException e) {
            log.error("Error initializing CetificateService", e);
            throw new RuntimeException(e);
        }

        loadKeyStore(systemTrustStore, cacertsBytes, systemTrustStoreBackend.loadPassword());
        loadKeyStore(externalTrustStore, extraTrustStoreBytes, extraTrustStoreBackend.loadPassword());
    }

    KeyStore getTrustStoreFromProperties(boolean isTrustSystem, Set<String> aliasSet, DestinationConnector connector) {
        try {
            KeyStore finalTrustStore;

            if (isTrustSystem) {
                finalTrustStore = clone(systemTrustStore);
            } else {
                finalTrustStore = KeyStore.getInstance(PKCS12);
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

                    if (!externalTrustStore.containsAlias(alias)) {
                        unknownAliases.add(alias);
                        continue;
                    }

                    var publicCertificate = externalTrustStore.getCertificate(alias);
                    finalTrustStore.setCertificateEntry(alias, publicCertificate);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }

            // TODO Connector data
            if (!presentInSystem.isEmpty()) {
                log.warn(
                    "Generating effective TrustStore for connector ({}) in channel ({}). Found and ignored aliases present in system truststore: {}",
                    connector.getDestinationName(),
                    connector.getChannel().getName(),
                    presentInSystem
                );
            }

            if (!unknownAliases.isEmpty()) {
                log.warn(
                    "Generating effective TrustStore for connector ({}) in channel ({}). Found aliases not present in additional truststore: {}",
                    connector.getDestinationName(),
                    connector.getChannel().getName(),
                    presentInSystem
                );
            }

            return finalTrustStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadKeyStore(KeyStore keystore, byte[] bytes, char[] password) {
        try (var bais = new ByteArrayInputStream(bytes)) {
            keystore.load(bais, password);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            log.error("Error loading keystore into memory", e);
            throw new RuntimeException(e);
        }
    }

    public void storeExtraTrustStore(byte[] keystoreBytes, char[] password) {
        try (var bais = new ByteArrayInputStream(keystoreBytes)) {
            externalTrustStore.load(bais, password);
            extraTrustStoreBackend.persist(keystoreBytes);
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            log.error("Error overwriting truststore", e);
            throw new RuntimeException(e);
        }
    }

    public Set<String> getLoadedAliases() {
        try {
            return new HashSet<>(Collections.list(externalTrustStore.aliases()));
        } catch (KeyStoreException e) {
            log.error("Error reading alias list from loaded truststore", e);
            throw new RuntimeException(e);
        }
    }

    private PersistenceMode getPersistenceMode() {
        var persistenceModeFromEnv = System.getenv(TLSPluginConstants.ENV_PERSISTENCE_BACKEND);

        if (persistenceModeFromEnv == null) {
            throw new IllegalStateException("%s is not set".formatted(TLSPluginConstants.ENV_PERSISTENCE_BACKEND));
        }

        var persistenceMode = PersistenceMode.valueOf(persistenceModeFromEnv.toUpperCase());

        log.info("Using persistence mode {}", persistenceMode);

        return persistenceMode;
    }

    /*
    TODO
    public void testConnection(
        String channelId,
        String channelName,
        HttpConnectorProperties tlsProperties,
        HttpDispatcherProperties dispatcherProperties
    ) {
        try {
            var url = new URL(
                templateValueReplacer.replaceValues(dispatcherProperties.getHost(),
                    channelId,
                    channelName
                )
            );

            int port = url.getPort();
            // If no port was provided, default to port 80 or 443.
            return ConnectorUtil.testConnection(url.getHost(), (port == -1) ? (StringUtils.equalsIgnoreCase(url.getProtocol(), "https") ? 443 : 80) : port, TIMEOUT);
        } catch (Exception e) {
            throw new MirthApiException(e);
        }
    }
     */

    /**
     * Perform a byte-level clone of a KeyStore object
     *
     * @param keystore The KeyStore object to be cloned
     * @return Byte-level clone of the provided KeyStore
     */
    private KeyStore clone(KeyStore keystore) {
        // This doesn't have to be secret. It is just here because a keystore password cannot be null
        final var password = "sup3rS3cr1t!".toCharArray();

        try (var outStream = new ByteArrayOutputStream()) {
            var finalTrustStore = KeyStore.getInstance(PKCS12);

            keystore.store(outStream, password);

            try (var inStream = new ByteArrayInputStream(outStream.toByteArray())) {
                finalTrustStore.load(inStream, password);
            }

            return finalTrustStore;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
