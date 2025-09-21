package org.openintegrationengine.tlsmanager.server;

import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.util.MirthSSLUtil;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.openintegrationengine.tlsmanager.shared.properties.HttpConnectorProperties;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class SocketFactoryService {

    private final ConfigurationController configurationController;
    private final CertificateService certificateService;
    private final ConcurrentHashMap<String, SSLConnectionSocketFactory> socketFactories;

    public SocketFactoryService(
        ConfigurationController configurationController,
        CertificateService certificateService
    ) {
        this.socketFactories = new ConcurrentHashMap<>();
        this.certificateService = certificateService;
        this.configurationController = configurationController;
    }

    public SSLConnectionSocketFactory getChannelSocketFactory(String connectorId, HttpConnectorProperties properties) {
        var socketFactory = buildSocketFactory(properties);
        socketFactories.put(connectorId, socketFactory);
        return socketFactory;
    }

    private SSLConnectionSocketFactory buildSocketFactory(HttpConnectorProperties properties) {
        try {
            var truststore = properties.isTrustSystemTruststore()
                ? certificateService.getSystemTrustStore()
                : certificateService.getMergedTruststore();

            var sslContext = SSLContexts
                .custom()
                .loadTrustMaterial(truststore, null)
                .build();

            var protocolArray = properties.isUseServerDefaultProtocols()
                ? MirthSSLUtil.getEnabledHttpsProtocols(configurationController.getHttpsServerProtocols())
                : MirthSSLUtil.getEnabledHttpsProtocols(properties.getUsedProtocols().toArray(new String[0]));

            var cipherArray = properties.isUseServerDefaultCiphers()
                ? MirthSSLUtil.getEnabledHttpsCipherSuites(configurationController.getHttpsCipherSuites())
                : MirthSSLUtil.getEnabledHttpsCipherSuites(properties.getUsedCiphers().toArray(new String[0]));

            var hostnameVerificationStrategy = properties.isHostnameVerificationEnabled()
                ? SSLConnectionSocketFactory.getDefaultHostnameVerifier()
                : NoopHostnameVerifier.INSTANCE;

            return new SSLConnectionSocketFactory(
                sslContext,
                protocolArray,
                cipherArray,
                hostnameVerificationStrategy
            );
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
