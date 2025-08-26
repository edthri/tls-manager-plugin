package org.openintegrationengine.sslmanager.server.connectorconfig;

import com.mirth.connect.connectors.http.DefaultHttpConfiguration;
import com.mirth.connect.connectors.http.HttpDispatcher;
import com.mirth.connect.connectors.http.HttpDispatcherProperties;
import com.mirth.connect.donkey.model.channel.ConnectorPluginProperties;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.util.MirthSSLUtil;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.openintegrationengine.sslmanager.server.CertificateService;

public class TLSHttpConfiguration extends DefaultHttpConfiguration {

    private final CertificateService certificateService;
    private final ConfigurationController configurationController;

    public TLSHttpConfiguration() {
        this.configurationController = ControllerFactory.getFactory().createConfigurationController();
        this.certificateService = CertificateService.getInstance();
    }

    @Override
    public void configureDispatcher(HttpDispatcher connector, HttpDispatcherProperties connectorProperties) {}

    @Override
    public void configureSocketFactoryRegistry(ConnectorPluginProperties properties, RegistryBuilder<ConnectionSocketFactory> registry) throws Exception {
        String[] enabledProtocols = MirthSSLUtil.getEnabledHttpsProtocols(configurationController.getHttpsClientProtocols());
        String[] enabledCipherSuites = MirthSSLUtil.getEnabledHttpsCipherSuites(configurationController.getHttpsCipherSuites());

        var mergedTruststore = certificateService.getMergedTruststore();

        var sslContext = SSLContexts
            .custom()
            .loadTrustMaterial(mergedTruststore, null)
            .build();

        var sslSocketFactory = new SSLConnectionSocketFactory(
            sslContext,
            enabledProtocols,
            enabledCipherSuites,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );

        registry.register("https", sslSocketFactory);
    }
}
