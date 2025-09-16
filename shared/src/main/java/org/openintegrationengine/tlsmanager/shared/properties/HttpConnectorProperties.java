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

package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class HttpConnectorProperties extends ConnectorPluginProperties {

    private boolean isTlsManagerEnabled;
    private boolean isServerCertificateValidationEnabled;

    // Public certificates
    private boolean trustSystemTruststore;
    private List<String> trustedServerCertificates;

    // Protocols
    private boolean isUseServerDefaultProtocols;
    private List<String> usedProtocols;

    // Ciphers
    private boolean isUseServerDefaultCiphers;
    private List<String> usedCiphers;

    private boolean isHostnameVerificationEnabled;
    private String clientCertificateAlias;

    public HttpConnectorProperties() {
        isTlsManagerEnabled = false;
        isServerCertificateValidationEnabled = false;

        trustSystemTruststore = true;
        trustedServerCertificates = Collections.emptyList();

        isUseServerDefaultProtocols = true;
        usedProtocols = Collections.emptyList();

        isUseServerDefaultCiphers = true;
        usedCiphers = Collections.emptyList();

        isHostnameVerificationEnabled = true;
        clientCertificateAlias = "";
    }

    public HttpConnectorProperties(HttpConnectorProperties props) {
        isTlsManagerEnabled = props.isTlsManagerEnabled();
        isServerCertificateValidationEnabled = props.isServerCertificateValidationEnabled();

        trustSystemTruststore = props.isTrustSystemTruststore();
        trustedServerCertificates = props.getTrustedServerCertificates();

        isUseServerDefaultProtocols = props.isUseServerDefaultProtocols();
        usedProtocols = props.getUsedProtocols();

        isUseServerDefaultCiphers = props.isUseServerDefaultCiphers();
        usedCiphers = props.getUsedCiphers();

        isHostnameVerificationEnabled = props.isHostnameVerificationEnabled();
        clientCertificateAlias = props.getClientCertificateAlias();
    }

    @Override
    public String getName() {
        return TLSPluginConstants.PLUGIN_POINTNAME;
    }

    @Override
    public HttpConnectorProperties clone() {
        return new HttpConnectorProperties(this);
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        return Map.of();
    }
}
