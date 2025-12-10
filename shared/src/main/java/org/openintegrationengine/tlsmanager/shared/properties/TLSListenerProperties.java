package org.openintegrationengine.tlsmanager.shared.properties;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.models.ClientAuthMode;

import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@ToString(callSuper = true)
public class TLSListenerProperties extends AbstractTLSConnectorProperties {

    private String serverCertificateAlias;

    private ClientAuthMode clientAuthMode;

    public TLSListenerProperties() {
        super();

        serverCertificateAlias = null;

        clientAuthMode = ClientAuthMode.NONE;
    }

    public TLSListenerProperties(TLSListenerProperties props) {
        super(props);

        var defaults = new TLSListenerProperties();

        serverCertificateAlias = props.getServerCertificateAlias();

        clientAuthMode = Objects.requireNonNullElse(
            props.getClientAuthMode(),
            defaults.getClientAuthMode()
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
