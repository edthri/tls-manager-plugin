package org.openintegrationengine.tlsmanager.server;

import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.util.MirthSSLUtil;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.properties.HttpConnectorProperties;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509CertSelector;
import java.util.EnumSet;
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

    public SSLConnectionSocketFactory getChannelSocketFactory(DestinationConnector connector, HttpConnectorProperties properties) {
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

            var pkixBuilderParameters = new PKIXBuilderParameters(truststore, new X509CertSelector());

            pkixBuilderParameters.setRevocationEnabled(
                properties.getCrlMode() != RevocationMode.DISABLED
                || properties.getOscpMode() != RevocationMode.DISABLED
            );

            var crlStore = CertStore.getInstance(
                "Collection",
                new CollectionCertStoreParameters()
            );
            pkixBuilderParameters.addCertStore(crlStore);

            if (properties.getCrlMode() != RevocationMode.DISABLED) {
                // Prefer CRLs and avoid falling back to OCSP (adjust to your policy)
                // TODO investigate OCSP
                var revocationChecker = (PKIXRevocationChecker) CertPathBuilder.getInstance("PKIX").getRevocationChecker();

                var revocationOptions = EnumSet.of(
                    PKIXRevocationChecker.Option.PREFER_CRLS,
                    PKIXRevocationChecker.Option.NO_FALLBACK
                );

                if (properties.getCrlMode() == RevocationMode.SOFT_FAIL) {
                    // This options sets from the default of HARD_FAIL to SOFT_FAIL
                    revocationOptions.add(PKIXRevocationChecker.Option.SOFT_FAIL);
                }

                revocationChecker.setOptions(revocationOptions);
                pkixBuilderParameters.addCertPathChecker(revocationChecker);
            }

            var trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init(new CertPathTrustManagerParameters(pkixBuilderParameters));

            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

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
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}
