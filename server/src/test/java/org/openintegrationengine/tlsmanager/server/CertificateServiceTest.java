package org.openintegrationengine.tlsmanager.server;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openintegrationengine.tlsmanager.shared.PersistenceMode;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class CertificateServiceTest {

    private CertificateService certificateService;

    //@BeforeEach
    public void setUp() {
        certificateService = new CertificateService(null);
    }

    //@Test
    public void testSetTrustStore() {
        try (var system = mockStatic(System.class)) {
            system
                .when(() -> System.getenv(TLSPluginConstants.ENV_PERSISTENCE_BACKEND))
                .thenReturn(PersistenceMode.DATABASE.toString());

            certificateService.init();

            System.out.println(System.getenv(TLSPluginConstants.ENV_PERSISTENCE_BACKEND));
        }

        //certificateService.storeExtraTrustStore();
    }
}
