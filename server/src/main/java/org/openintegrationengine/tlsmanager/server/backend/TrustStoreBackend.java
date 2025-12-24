// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: 2025 NovaMap Health Limited <https://novamap.health>

package org.openintegrationengine.tlsmanager.server.backend;

public interface TrustStoreBackend {
    boolean persist(byte[] keystore);

    void init();

    byte[] load();

    char[] loadPassword();
}
