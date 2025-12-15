package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
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
public class TLSConnectorProperties extends AbstractTLSConnectorProperties {

    // Server mode properties
    private String serverCertificateAlias;
    private ClientAuthMode clientAuthMode;

    // Client mode properties
    private boolean isServerCertificateValidationEnabled;
    private boolean isHostnameVerificationEnabled;
    private String clientCertificateAlias;

    public TLSConnectorProperties() {
        super();

        // Server mode properties
        serverCertificateAlias = null;
        clientAuthMode = ClientAuthMode.NONE;

        // Client mode properties
        isServerCertificateValidationEnabled = false;
        isHostnameVerificationEnabled = true;
        clientCertificateAlias = null;
    }

    public TLSConnectorProperties(TLSConnectorProperties props) {
        super(props);

        // Server mode properties
        serverCertificateAlias = props.getServerCertificateAlias();
        clientAuthMode = Objects.requireNonNullElse(
            props.getClientAuthMode(),
            ClientAuthMode.NONE
        );

        // Client mode properties
        isServerCertificateValidationEnabled = props.isServerCertificateValidationEnabled();
        isHostnameVerificationEnabled = props.isHostnameVerificationEnabled();
        clientCertificateAlias = props.getClientCertificateAlias();
    }

    @Override
    public String getName() {
        return TLSPluginConstants.TLS_LISTENER_PROPERTIES_PLUGIN_POINT_NAME;
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
