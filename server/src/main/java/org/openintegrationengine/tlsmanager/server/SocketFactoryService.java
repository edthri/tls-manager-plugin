package org.openintegrationengine.tlsmanager.server;

import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.util.MirthSSLUtil;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.openintegrationengine.tlsmanager.server.revocation.DualCheckerTrustManager;
import org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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

    public SSLConnectionSocketFactory getConnectorSocketFactory(DestinationConnector connector, TLSConnectorProperties properties) {
        try {
            var truststore = certificateService.getTrustStoreFromProperties(
                properties.isTrustSystemTruststore(),
                properties.getTrustedServerCertificates(),
                connector
            );

            // Exit early if truststore is empty
            if (!truststore.aliases().hasMoreElements()) {
                return null;
            }

            var dualcheckerTrustManager = new DualCheckerTrustManager(
                truststore,
                properties.getOcspMode(),
                properties.getCrlMode(),
                null
            );


            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { dualcheckerTrustManager }, null);

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
