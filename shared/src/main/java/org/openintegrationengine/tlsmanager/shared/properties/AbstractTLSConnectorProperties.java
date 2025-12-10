package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import lombok.Getter;
import lombok.Setter;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public abstract class AbstractTLSConnectorProperties extends ConnectorPluginProperties {
    protected boolean isTlsManagerEnabled;

    // Certificate revocation modes
    protected RevocationMode crlMode;
    protected RevocationMode ocspMode;

    // Protocols
    protected boolean isUseServerDefaultProtocols;
    protected Set<String> usedProtocols;

    // Ciphers
    protected boolean isUseServerDefaultCiphers;
    protected Set<String> usedCiphers;

    protected AbstractTLSConnectorProperties defaults;

    protected AbstractTLSConnectorProperties() {
        isTlsManagerEnabled = false;

        crlMode = RevocationMode.HARD_FAIL;
        ocspMode = RevocationMode.HARD_FAIL;

        isUseServerDefaultProtocols = true;
        usedProtocols = Collections.emptySet();

        isUseServerDefaultCiphers = true;
        usedCiphers = Collections.emptySet();
    }

    protected AbstractTLSConnectorProperties(AbstractTLSConnectorProperties props) {
        isTlsManagerEnabled = props.isTlsManagerEnabled();

        crlMode = Objects.requireNonNullElse(props.getCrlMode(), RevocationMode.HARD_FAIL);
        ocspMode = Objects.requireNonNullElse(props.getOcspMode(), RevocationMode.HARD_FAIL);

        isUseServerDefaultProtocols = props.isUseServerDefaultProtocols();
        usedProtocols = Objects.requireNonNullElse(
            props.getUsedProtocols(),
            Collections.emptySet()
        );

        isUseServerDefaultCiphers = props.isUseServerDefaultCiphers();
        usedCiphers = Objects.requireNonNullElse(
            props.getUsedCiphers(),
            Collections.emptySet()
        );
    }
}
