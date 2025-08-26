package org.openintegrationengine.sslmanager.server;

import lombok.Getter;
import org.openintegrationengine.sslmanager.server.backend.FileTrustStoreBackend;
import org.openintegrationengine.sslmanager.server.backend.SystemTrustStoreBackend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;

public final class CertificateService {

    @Getter
    private static CertificateService instance = new CertificateService();

    @Getter
    private KeyStore systemTrustStore;

    @Getter
    private KeyStore additionalTrustStore;

    @Getter
    private KeyStore mergedTruststore;

    void init() {
        byte[] cacerts = new SystemTrustStoreBackend().load();
        byte[] additional = new FileTrustStoreBackend(
            "/certs/truststore.p12"
        ).load();

        try {
            systemTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            additionalTrustStore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        loadKeyStore(systemTrustStore, cacerts, SystemTrustStoreBackend.resolvePassword());
        loadKeyStore(additionalTrustStore, additional, "changeit".toCharArray());

        try {
            mergedTruststore = mergeKeystores(systemTrustStore, additionalTrustStore);
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
