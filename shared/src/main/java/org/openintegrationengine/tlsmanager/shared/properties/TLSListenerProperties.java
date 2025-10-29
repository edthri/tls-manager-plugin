package org.openintegrationengine.tlsmanager.shared.properties;

import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class TLSListenerProperties extends ConnectorPluginProperties {

    private boolean isTlsManagerEnabled;

    public TLSListenerProperties() {
        isTlsManagerEnabled = false;
    }

    public TLSListenerProperties(TLSListenerProperties properties) {
        isTlsManagerEnabled = properties.isTlsManagerEnabled();
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
