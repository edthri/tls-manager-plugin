/*
 * SPDX-License-Identifier: Apache-2.0 OR MPL-2.0
 * Copyright (c) 2021 Kaur Palang
 */

package org.openintegrationengine.tlsmanager.server.backend;

public interface TrustStoreBackend {
    boolean persist(byte[] keystore);

    void init();

    byte[] load();

    char[] loadPassword();
}
