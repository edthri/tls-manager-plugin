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
import org.openintegrationengine.tlsmanager.server.backend.FileTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.SystemTrustStoreBackend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class CertificateService {

    @Getter
    private KeyStore systemTrustStore;

    @Getter
    private KeyStore truststore;

    @Getter
    private KeyStore keystore;

    @Getter
    private KeyStore mergedTruststore;

    public CertificateService() {
    }

    void init() {
        var systemTruststore = new SystemTrustStoreBackend();
        var additionalTruststore = new FileTrustStoreBackend("/certs/truststore.p12");

        byte[] cacerts = systemTruststore.load();
        byte[] additional = additionalTruststore.load();

        try {
            systemTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        loadKeyStore(systemTrustStore, cacerts, systemTruststore.loadPassword());
        loadKeyStore(truststore, additional, "changeit".toCharArray());

        try {
            mergedTruststore = mergeKeystores(systemTrustStore, truststore);
        } catch (KeyStoreException e) {
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

    private KeyStore mergeKeystores(KeyStore base, KeyStore toMerge) throws KeyStoreException {
        var mergedKeystore = KeyStore.getInstance("PKCS12");
        try {
            mergedKeystore.load(null, null);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            // TODO Fix exception handling
            throw new RuntimeException(e);
        }

        Collections
            .list(base.aliases())
            .forEach(alias -> {
                try {
                    if (base.isCertificateEntry(alias)) {
                        mergedKeystore.setCertificateEntry(alias, base.getCertificate(alias));
                    }
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            });

        Collections
            .list(toMerge.aliases())
            .forEach(alias -> {
                try {
                    if (toMerge.isCertificateEntry(alias)) {
                        mergedKeystore.setCertificateEntry(
                            "merged-%s".formatted(alias),
                            toMerge.getCertificate(alias)
                        );
                    }
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            });

        return mergedKeystore;
    }
}
