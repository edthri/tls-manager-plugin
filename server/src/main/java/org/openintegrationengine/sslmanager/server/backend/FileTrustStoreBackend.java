package org.openintegrationengine.sslmanager.server.backend;

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
}
