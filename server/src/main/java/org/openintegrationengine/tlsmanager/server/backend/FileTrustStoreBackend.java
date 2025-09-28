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

import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Slf4j
public class FileTrustStoreBackend implements TrustStoreBackend {

    private final Path keystorePath;
    private char[] storepass;

    public FileTrustStoreBackend(String keystorePath) {
        this.keystorePath = Paths.get(keystorePath);
    }

    @Override
    public boolean persist(byte[] keystore) {
        final var openOptions = new OpenOption[]{
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE
        };

        try {
            Files.write(keystorePath, keystore, openOptions);
            return true;
        } catch (IOException e) {
            log.error("Error persisting keystore to file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        var envStorepass = System.getenv(TLSPluginConstants.ENV_PERSISTENCE_FS_STOREPASS);
        if (envStorepass == null) {
            throw new IllegalStateException("TrustStore password not set");
        }

        storepass = envStorepass.toCharArray();

        if (Files.exists(keystorePath)) {
            log.debug("Using existing keystore at {}", keystorePath);
            return;
        }

        try {
            var keystore = KeyStore.getInstance(TLSPluginConstants.PKCS12);
            keystore.load(null, storepass);

            try (var baos = new ByteArrayOutputStream()) {
                keystore.store(baos, storepass);
                persist(baos.toByteArray());
            }

        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            log.error("Error initializing keystore", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] load() {
        try {
            return Files.readAllBytes(keystorePath);
        } catch (IOException e) {
            log.error("Error reading keystore at {}", keystorePath, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public char[] loadPassword() {
        return storepass;
    }
}
