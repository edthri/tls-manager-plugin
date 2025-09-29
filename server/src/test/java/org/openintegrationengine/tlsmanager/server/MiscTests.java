package org.openintegrationengine.tlsmanager.server;

import com.mirth.connect.connectors.http.HttpDispatcher;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.util.MirthSSLUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openintegrationengine.tlsmanager.server.backend.FileTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.backend.SystemTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.util.ConnectionUtils;
import org.openintegrationengine.tlsmanager.server.util.MockConfigurationController;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.properties.HttpConnectorProperties;

import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MiscTests {

    //@Test
    public void asi() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        var configurationController = mock(MockConfigurationController.class);
        var certificateService = mock(CertificateService.class);

        var connector = new HttpDispatcher();

        var trustStoreBackend = new FileTrustStoreBackend("/home/kaurpalang/IdeaProjects/plugin-ssl-manager/docker/certs/truststore.p12");
        var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (var bais = new ByteArrayInputStream(trustStoreBackend.load())) {
            trustStore.load(bais, trustStoreBackend.loadPassword());
        }

        when(
            certificateService.getTrustStoreFromProperties(anyBoolean(), anySet(), isA(DestinationConnector.class))
        ).thenReturn(
            trustStore
        );

        when(
            configurationController.getHttpsServerProtocols()
        ).thenReturn(
            protocols()
        );

        when(
            configurationController.getHttpsCipherSuites()
        ).thenReturn(
            cipherSuites()
        );

        var socketFactoryService = new SocketFactoryService(
            configurationController,
            certificateService
        );

        var connectorProperties = new HttpConnectorProperties();
        connectorProperties.setCrlMode(RevocationMode.DISABLED);
        connectorProperties.setOscpMode(RevocationMode.DISABLED);

        try (var mirthSSlUtil = mockStatic(MirthSSLUtil.class)) {
            mirthSSlUtil
                .when(MirthSSLUtil::getSupportedHttpsProtocols)
                .thenReturn(protocols());

            mirthSSlUtil
                .when(MirthSSLUtil::getSupportedHttpsCipherSuites)
                .thenReturn(cipherSuites());

            var socketFactory = socketFactoryService.getConnectorSocketFactory(connector, connectorProperties);

            var exception = assertThrows(SSLHandshakeException.class, () -> {
                var connectionResult = ConnectionUtils.thing(
                    socketFactory,
                    "valid.crl.caddy",
                    9443,
                    2_000,
                    null,
                    0
                );
            });
        }
    }

    //@Test
    public void test_SSLHandShakeException() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        var configurationController = mock(MockConfigurationController.class);
        var certificateService = mock(CertificateService.class);
        var connector = new HttpDispatcher();

        var trustStoreBackend = new SystemTrustStoreBackend();
        var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (var bais = new ByteArrayInputStream(trustStoreBackend.load())) {
            trustStore.load(bais, trustStoreBackend.loadPassword());
        }

        when(
            certificateService.getTrustStoreFromProperties(anyBoolean(), anySet(), isA(DestinationConnector.class))
        ).thenReturn(
            trustStore
        );

        when(
            configurationController.getHttpsServerProtocols()
        ).thenReturn(
            protocols()
        );

        when(
            configurationController.getHttpsCipherSuites()
        ).thenReturn(
            cipherSuites()
        );

        var socketFactoryService = new SocketFactoryService(
            configurationController,
            certificateService
        );

        var connectorProperties = new HttpConnectorProperties();

        try (var mirthSSlUtil = mockStatic(MirthSSLUtil.class)) {
            mirthSSlUtil
                .when(MirthSSLUtil::getSupportedHttpsProtocols)
                .thenReturn(protocols());

            mirthSSlUtil
                .when(MirthSSLUtil::getSupportedHttpsCipherSuites)
                .thenReturn(cipherSuites());

            var socketFactory = socketFactoryService.getConnectorSocketFactory(connector, connectorProperties);

            var exception = assertThrows(SSLHandshakeException.class, () -> {
                var connectionResult = ConnectionUtils.thing(
                    socketFactory,
                    "valid.crl.caddy",
                    9443,
                    2_000,
                    null,
                    0
                );
            });
        }
    }

    private static String[] protocols() {
        return new String[] {
            "TLSv1.3", "TLSv1.2", "SSLv2Hello"
        };
    }

    private static String[] cipherSuites() {
        return new String[] {
            "TLS_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
        };
    }
}
