package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.models.ClientAuthMode;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.models.SubjectDnValidationMode;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@ToString(callSuper = true)
public class TLSListenerProperties extends ConnectorPluginProperties {

    private boolean isTlsManagerEnabled;

    private String serverCertificateAlias;

    private SubjectDnValidationMode subjectDnValidationMode;
    private String subjectDnValidationFilter;

    private ClientAuthMode clientAuthMode;

    // Certificate revocation modes
    private RevocationMode crlMode;
    private RevocationMode ocspMode;

    // Protocols
    private boolean isUseServerDefaultProtocols;
    private Set<String> usedProtocols;

    // Ciphers
    private boolean isUseServerDefaultCiphers;
    private Set<String> usedCiphers;

    public TLSListenerProperties() {
        isTlsManagerEnabled = false;

        serverCertificateAlias = null;

        subjectDnValidationMode = SubjectDnValidationMode.NONE;
        subjectDnValidationFilter = null;

        clientAuthMode = ClientAuthMode.NONE;

        crlMode = RevocationMode.HARD_FAIL;
        ocspMode = RevocationMode.HARD_FAIL;

        isUseServerDefaultProtocols = true;
        usedProtocols = Collections.emptySet();

        isUseServerDefaultCiphers = true;
        usedCiphers = Collections.emptySet();
    }

    public TLSListenerProperties(TLSListenerProperties props) {
        isTlsManagerEnabled = props.isTlsManagerEnabled();

        serverCertificateAlias = props.getServerCertificateAlias();

        clientAuthMode = props.getClientAuthMode();

        subjectDnValidationMode = props.getSubjectDnValidationMode();
        subjectDnValidationFilter = props.getSubjectDnValidationFilter();

        crlMode = props.getCrlMode();
        ocspMode = props.getOcspMode();

        isUseServerDefaultProtocols = props.isUseServerDefaultProtocols();
        usedProtocols = props.getUsedProtocols();

        isUseServerDefaultCiphers = props.isUseServerDefaultCiphers();
        usedCiphers = props.getUsedCiphers();
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
