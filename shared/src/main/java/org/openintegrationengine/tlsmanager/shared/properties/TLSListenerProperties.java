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
public class TLSListenerProperties extends ConnectorPluginProperties {

    private boolean isTlsManagerEnabled;

    private SubjectDnValidationMode subjectDnValidationMode;
    private String subjectDnValidationFilter;

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

        subjectDnValidationMode = SubjectDnValidationMode.NONE;
        subjectDnValidationFilter = null;

        crlMode = RevocationMode.HARD_FAIL;
        ocspMode = RevocationMode.HARD_FAIL;

        isUseServerDefaultProtocols = true;
        usedProtocols = Collections.emptySet();

        isUseServerDefaultCiphers = true;
        usedCiphers = Collections.emptySet();
    }

    public TLSListenerProperties(TLSListenerProperties props) {
        isTlsManagerEnabled = props.isTlsManagerEnabled();

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
        return TLSPluginConstants.PLUGIN_POINTNAME;
    }

    @Override
    public ConnectorPluginProperties clone() {
        return new TLSListenerProperties(this);
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        return Map.of();
    }
}
