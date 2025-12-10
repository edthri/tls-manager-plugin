package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTLSConnectorProperties extends ConnectorPluginProperties {
    protected boolean isTlsManagerEnabled;

    protected AbstractTLSConnectorProperties() {
        isTlsManagerEnabled = false;
    }

    protected AbstractTLSConnectorProperties(AbstractTLSConnectorProperties props) {
        isTlsManagerEnabled = props.isTlsManagerEnabled();
    }
}
