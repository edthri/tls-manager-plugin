package org.openintegrationengine.tlsmanager.shared.properties;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.models.ClientAuthMode;
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
public class TLSListenerProperties extends AbstractTLSConnectorProperties {

    private String serverCertificateAlias;

    private SubjectDnValidationMode subjectDnValidationMode;
    private String subjectDnValidationFilter;

    private ClientAuthMode clientAuthMode;

    // Truststore to use for mtls client cert validation
    private boolean trustSystemTruststore;
    private Set<String> trustedServerCertificates;

    // Protocols
    private boolean isUseServerDefaultProtocols;
    private Set<String> usedProtocols;

    // Ciphers
    private boolean isUseServerDefaultCiphers;
    private Set<String> usedCiphers;

    public TLSListenerProperties() {
        super();

        serverCertificateAlias = null;

        subjectDnValidationMode = SubjectDnValidationMode.NONE;
        subjectDnValidationFilter = null;

        clientAuthMode = ClientAuthMode.NONE;

        trustSystemTruststore = true;
        trustedServerCertificates = Collections.emptySet();

        isUseServerDefaultProtocols = true;
        usedProtocols = Collections.emptySet();

        isUseServerDefaultCiphers = true;
        usedCiphers = Collections.emptySet();
    }

    public TLSListenerProperties(TLSListenerProperties props) {
        super(props);

        var defaults = new TLSListenerProperties();

        serverCertificateAlias = props.getServerCertificateAlias();

        subjectDnValidationMode = Objects.requireNonNullElse(
            props.getSubjectDnValidationMode(),
            defaults.getSubjectDnValidationMode()
        );
        subjectDnValidationFilter = props.getSubjectDnValidationFilter();

        clientAuthMode = Objects.requireNonNullElse(
            props.getClientAuthMode(),
            defaults.getClientAuthMode()
        );

        trustSystemTruststore = props.isTrustSystemTruststore();
        trustedServerCertificates = Objects.requireNonNullElse(
            props.getTrustedServerCertificates(),
            defaults.getTrustedServerCertificates()
        );

        isUseServerDefaultProtocols = props.isUseServerDefaultProtocols();
        usedProtocols = Objects.requireNonNullElse(
            props.getUsedProtocols(),
            defaults.getUsedProtocols()
        );

        isUseServerDefaultCiphers = props.isUseServerDefaultCiphers();
        usedCiphers = Objects.requireNonNullElse(
            props.getUsedCiphers(),
            defaults.getUsedCiphers()
        );
    }

    @Override
    public String getName() {
        return TLSPluginConstants.TLS_LISTENER_CONNECTOR_PROPERTIES_PLUGIN_POINT_NAME;
    }

    @Override
    public TLSListenerProperties clone() {
        return new TLSListenerProperties(this);
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        return Map.of();
    }
}
