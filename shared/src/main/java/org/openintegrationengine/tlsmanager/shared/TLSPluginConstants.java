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

package org.openintegrationengine.tlsmanager.shared;

public final class TLSPluginConstants {
    public static final String PLUGIN_POINTNAME = "TLS Manager";

    public static final String SETTINGS_TABNAME_MAIN = "TLS Settings";

    public static final String PROPERTY_TRUST_BACKEND = "trust.backend";

    public static final String ENV_PERSISTENCE_BACKEND = "OIE_TLS_PLUGIN_PERSISTENCE_BACKEND";
    public static final String ENV_PERSISTENCE_FS_STOREPASS = "OIE_TLS_PLUGIN_FS_STOREPASS";
    public static final String ENV_PERSISTENCE_FS_STOREPATH = "OIE_TLS_PLUGIN_FS_STOREPATH";

    public static final String PKCS12 = "PKCS12";

    private TLSPluginConstants() {}
}
