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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@ToString(callSuper = true)
public class TLSSenderProperties extends AbstractTLSConnectorProperties {

    private boolean isServerCertificateValidationEnabled;

    private SubjectDnValidationMode subjectDnValidationMode;
    private String subjectDnValidationFilter;

    // Public certificates
    private boolean trustSystemTruststore;
    private Set<String> trustedServerCertificates;

    private boolean isHostnameVerificationEnabled;
    private String clientCertificateAlias;

    public TLSSenderProperties() {
        super();

        isServerCertificateValidationEnabled = false;

        subjectDnValidationMode = SubjectDnValidationMode.NONE;
        subjectDnValidationFilter = null;

        trustSystemTruststore = true;
        trustedServerCertificates = Collections.emptySet();

        isHostnameVerificationEnabled = true;
        clientCertificateAlias = null;
    }

    public TLSSenderProperties(TLSSenderProperties props) {
        super(props);

        var defaults = new TLSSenderProperties();

        isServerCertificateValidationEnabled = props.isServerCertificateValidationEnabled();

        subjectDnValidationMode = Objects.requireNonNullElse(
            props.getSubjectDnValidationMode(),
            defaults.getSubjectDnValidationMode()
        );
        subjectDnValidationFilter = props.getSubjectDnValidationFilter();

        trustSystemTruststore = props.isTrustSystemTruststore();
        trustedServerCertificates = Objects.requireNonNullElse(
            props.getTrustedServerCertificates(),
            defaults.getTrustedServerCertificates()
        );

        isHostnameVerificationEnabled = props.isHostnameVerificationEnabled();
        clientCertificateAlias = props.getClientCertificateAlias();
    }

    @Override
    public String getName() {
        return TLSPluginConstants.TLS_SENDER_CONNECTOR_PROPERTIES_PLUGIN_POINT_NAME;
    }

    @Override
    public TLSSenderProperties clone() {
        return new TLSSenderProperties(this);
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        return Map.of();
    }
}
