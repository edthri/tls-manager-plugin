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
    public static final String ENV_PERSISTENCE_FS_TRUSTSTOREPATH = "OIE_TLS_PLUGIN_FS_TRUSTSTOREPATH";
    public static final String ENV_PERSISTENCE_FS_TRUSTSTOREPASS = "OIE_TLS_PLUGIN_FS_TRUSTSTOREPASS";
    public static final String ENV_PERSISTENCE_FS_KEYSTOREPASS = "OIE_TLS_PLUGIN_FS_KEYSTOREPASS";
    public static final String ENV_PERSISTENCE_FS_KEYSTOREPATH = "OIE_TLS_PLUGIN_FS_KEYSTOREPATH";

    public static final String PKCS12 = "PKCS12";

    // This ain't no joke
    public static final String TLS_SENDER_CONNECTOR_PROPERTIES_PLUGIN_POINT_NAME = "TLS Sender Connector Properties Plugin";
    public static final String TLS_LISTENER_CONNECTOR_PROPERTIES_PLUGIN_POINT_NAME = "TLS Listener Connector Properties Plugin";

    private TLSPluginConstants() {}
}
