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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileTrustStoreBackend implements TrustStoreBackend {

    private final Path keystorePath;

    public FileTrustStoreBackend(String keystorePath) {
        this.keystorePath = Paths.get(keystorePath);
    }

    @Override
    public boolean persist(byte[] keystore) {
        try {
            Files.write(keystorePath, keystore, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] load() {
        try {
            return Files.readAllBytes(keystorePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public char[] loadPassword() {
        return "changeit".toCharArray();
    }
}
