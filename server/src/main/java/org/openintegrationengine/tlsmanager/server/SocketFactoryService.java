package org.openintegrationengine.tlsmanager.server;

import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.util.MirthSSLUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.openintegrationengine.tlsmanager.server.revocation.DualCheckerTrustManager;
import org.openintegrationengine.tlsmanager.shared.models.WeirdIntermediaryContextContainer;
import org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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
        var contextContainer = generateSSLContext(connector, properties);
        return getConnectorSocketFactory(contextContainer);
    }

    public SSLConnectionSocketFactory getConnectorSocketFactory(WeirdIntermediaryContextContainer contextContainer) {
        // Return null to trigger building the connection with OIE's internal logic
        if (contextContainer == null) return null;

        return new SSLConnectionSocketFactory(
            contextContainer.sslContext(),
            contextContainer.protocols(),
            contextContainer.ciphers(),
            contextContainer.hostnameVerifier()
        );
    }

    public WeirdIntermediaryContextContainer generateSSLContext(DestinationConnector connector, TLSConnectorProperties properties) {
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

            KeyManager[] keyManagers = null;
            var clientAlias = properties.getClientCertificateAlias();
            if (clientAlias != null && !clientAlias.isBlank()) {
                var keystore = certificateService.getKeyStore(clientAlias, connector);

                var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keystore, new char[0]);
                keyManagers = keyManagerFactory.getKeyManagers();
            }

            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, new TrustManager[] { dualcheckerTrustManager }, null);

            var protocolArray = properties.isUseServerDefaultProtocols()
                ? MirthSSLUtil.getEnabledHttpsProtocols(configurationController.getHttpsServerProtocols())
                : MirthSSLUtil.getEnabledHttpsProtocols(properties.getUsedProtocols().toArray(new String[0]));

            var cipherArray = properties.isUseServerDefaultCiphers()
                ? MirthSSLUtil.getEnabledHttpsCipherSuites(configurationController.getHttpsCipherSuites())
                : MirthSSLUtil.getEnabledHttpsCipherSuites(properties.getUsedCiphers().toArray(new String[0]));

            var hostnameVerificationStrategy = properties.isHostnameVerificationEnabled()
                ? SSLConnectionSocketFactory.getDefaultHostnameVerifier()
                : NoopHostnameVerifier.INSTANCE;

            return new WeirdIntermediaryContextContainer(
                sslContext,
                protocolArray,
                cipherArray,
                hostnameVerificationStrategy
            );
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            log.error("Error generating SSLContext", e);
            throw new RuntimeException(e);
        }
    }
}
