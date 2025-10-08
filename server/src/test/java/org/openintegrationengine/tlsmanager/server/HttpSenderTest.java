package org.openintegrationengine.tlsmanager.server;

import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.util.ConnectionTestResponse;
import com.mirth.connect.util.MirthSSLUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openintegrationengine.tlsmanager.server.backend.SystemTrustStoreBackend;
import org.openintegrationengine.tlsmanager.server.util.ConnectionUtils;
import org.openintegrationengine.tlsmanager.server.util.MockConfigurationController;
import org.openintegrationengine.tlsmanager.server.util.MockDestinationConnector;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;
import org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties;

import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.openintegrationengine.tlsmanager.server.util.Statics.cipherSuites;
import static org.openintegrationengine.tlsmanager.server.util.Statics.protocols;

@ExtendWith(MockitoExtension.class)
public class HttpSenderTest {

    private ConfigurationController configurationController;
    private CertificateService certificateService;

    private DestinationConnector connector;

    private MockedStatic<MirthSSLUtil> mirthSSLUtil;

    private static KeyStore systemTruststore;

    @BeforeAll
    static void beforeAll() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        var trustStoreBackend = new SystemTrustStoreBackend();

        systemTruststore = KeyStore.getInstance(TLSPluginConstants.PKCS12);
        try (var bais = new ByteArrayInputStream(trustStoreBackend.load())) {
            systemTruststore.load(bais, trustStoreBackend.loadPassword());
        }
    }

    @BeforeEach
    void beforeEach() {
        // Nasty
        mirthSSLUtil = mockStatic(MirthSSLUtil.class);
        mirthSSLUtil
            .when(MirthSSLUtil::getSupportedHttpsProtocols)
            .thenReturn(protocols());
        mirthSSLUtil
            .when(MirthSSLUtil::getSupportedHttpsCipherSuites)
            .thenReturn(cipherSuites());
        mirthSSLUtil
            .when(() -> MirthSSLUtil.getEnabledHttpsProtocols(any()))
            .thenReturn(protocols());
        mirthSSLUtil
            .when(() -> MirthSSLUtil.getEnabledHttpsCipherSuites(any()))
            .thenReturn(cipherSuites());

        configurationController = mock(MockConfigurationController.class);
        connector = new MockDestinationConnector();

        certificateService = mock(CertificateService.class);
    }

    @AfterEach
    public void tearDown() {
        mirthSSLUtil.close();
    }

    @Test
    void test_OSP_T13_untrustedConfiguredCertificate() {
        var tlsProperties = new TLSConnectorProperties(
            true,
            false,
            RevocationMode.HARD_FAIL,
            RevocationMode.HARD_FAIL,
            false,
            Set.of("server2"),
            true,
            Collections.emptySet(),
            true,
            Collections.emptySet(),
            false,
            null
        );

        when(
            certificateService.getTrustStoreFromProperties(anyBoolean(), anySet(), isA(MockDestinationConnector.class))
        ).thenReturn(
            systemTruststore
        );

        var socketFactoryService = new SocketFactoryService(configurationController, certificateService);
        var socketFactory = socketFactoryService.getConnectorSocketFactory(connector, tlsProperties);

        var exception = assertThrows(SSLHandshakeException.class, () -> ConnectionUtils.testConnection(
            socketFactory,
            "https://valid.crl.caddy:9443",
            1_000,
            null,
            0
        ));

        assertEquals(
            "Validation error: Path does not chain with any of the trust anchors",
            exception.getMessage()
        );
    }

    @Test
    void test_OSP_T14_systemTruststore() throws Exception {
        var tlsProperties = new TLSConnectorProperties(
            true,
            false,
            RevocationMode.HARD_FAIL,
            RevocationMode.HARD_FAIL,
            true,
            Collections.emptySet(),
            true,
            Collections.emptySet(),
            true,
            Collections.emptySet(),
            false,
            null
        );

        when(
            certificateService.getTrustStoreFromProperties(anyBoolean(), anySet(), isA(MockDestinationConnector.class))
        ).thenReturn(
            systemTruststore
        );

        var socketFactoryService = new SocketFactoryService(configurationController, certificateService);
        var socketFactory = socketFactoryService.getConnectorSocketFactory(connector, tlsProperties);

        var result = ConnectionUtils.testConnection(
            socketFactory,
            "https://bbc.co.uk",
            1_000,
            null,
            0
        );

        assertEquals(
            ConnectionTestResponse.Type.SUCCESS,
            result.getType()
        );
    }
}
