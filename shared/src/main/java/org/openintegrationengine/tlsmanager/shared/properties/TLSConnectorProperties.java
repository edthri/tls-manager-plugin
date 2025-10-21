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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class TLSConnectorProperties extends ConnectorPluginProperties {

    private boolean isTlsManagerEnabled;
    private boolean isServerCertificateValidationEnabled;

    private SubjectDnValidationMode subjectDnValidationMode;
    private String subjectDnValidationFilter;

    // Certificate revocation modes
    private RevocationMode crlMode;
    private RevocationMode ocspMode;

    // Public certificates
    private boolean trustSystemTruststore;
    private Set<String> trustedServerCertificates;

    // Protocols
    private boolean isUseServerDefaultProtocols;
    private Set<String> usedProtocols;

    // Ciphers
    private boolean isUseServerDefaultCiphers;
    private Set<String> usedCiphers;

    private boolean isHostnameVerificationEnabled;
    private String clientCertificateAlias;

    public TLSConnectorProperties() {
        isTlsManagerEnabled = false;
        isServerCertificateValidationEnabled = false;

        subjectDnValidationMode = SubjectDnValidationMode.NONE;
        subjectDnValidationFilter = null;

        crlMode = RevocationMode.HARD_FAIL;
        ocspMode = RevocationMode.HARD_FAIL;

        trustSystemTruststore = true;
        trustedServerCertificates = Collections.emptySet();

        isUseServerDefaultProtocols = true;
        usedProtocols = Collections.emptySet();

        isUseServerDefaultCiphers = true;
        usedCiphers = Collections.emptySet();

        isHostnameVerificationEnabled = true;
        clientCertificateAlias = null;
    }

    public TLSConnectorProperties(TLSConnectorProperties props) {
        isTlsManagerEnabled = props.isTlsManagerEnabled();
        isServerCertificateValidationEnabled = props.isServerCertificateValidationEnabled();

        subjectDnValidationMode = props.getSubjectDnValidationMode();
        subjectDnValidationFilter = props.getSubjectDnValidationFilter();

        crlMode = props.getCrlMode();
        ocspMode = props.getOcspMode();

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
    public TLSConnectorProperties clone() {
        return new TLSConnectorProperties(this);
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        return Map.of();
    }
}
