package org.openintegrationengine.sslmanager.server.backend;

import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;
import org.openintegrationengine.sslmanager.shared.SSLPluginConstants;

import java.util.Base64;

public class DatabaseTrustStoreBackend implements TrustStoreBackend {

    private ConfigurationController configurationController;

    public DatabaseTrustStoreBackend() {
        this.configurationController = ControllerFactory.getFactory().createConfigurationController();
    }

    @Override
    public boolean persist(byte[] keystore) {
        Base64.Encoder encoder = Base64.getEncoder();
        var b64Keystore = encoder.encodeToString(keystore);
        configurationController.saveProperty(SSLPluginConstants.PLUGIN_POINTNAME, "additionalKeystore", b64Keystore);
        return false;
    }

    @Override
    public byte[] load() {
        return new byte[0];
    }
}
