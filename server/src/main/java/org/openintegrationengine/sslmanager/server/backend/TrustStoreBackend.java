package org.openintegrationengine.sslmanager.server.backend;

public interface TrustStoreBackend {
    boolean persist(byte[] keystore);

    byte[] load();
}
