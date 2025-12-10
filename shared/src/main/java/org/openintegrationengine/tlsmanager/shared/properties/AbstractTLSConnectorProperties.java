package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import lombok.Getter;
import lombok.Setter;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;

import java.util.Objects;

@Getter
@Setter
public abstract class AbstractTLSConnectorProperties extends ConnectorPluginProperties {
    protected boolean isTlsManagerEnabled;

    // Certificate revocation modes
    protected RevocationMode crlMode;
    protected RevocationMode ocspMode;

    protected AbstractTLSConnectorProperties defaults;

    protected AbstractTLSConnectorProperties() {
        isTlsManagerEnabled = false;

        crlMode = RevocationMode.HARD_FAIL;
        ocspMode = RevocationMode.HARD_FAIL;
    }

    protected AbstractTLSConnectorProperties(AbstractTLSConnectorProperties props) {
        isTlsManagerEnabled = props.isTlsManagerEnabled();

        crlMode = Objects.requireNonNullElse(props.getCrlMode(), RevocationMode.HARD_FAIL);
        ocspMode = Objects.requireNonNullElse(props.getOcspMode(), RevocationMode.HARD_FAIL);
    }
}
