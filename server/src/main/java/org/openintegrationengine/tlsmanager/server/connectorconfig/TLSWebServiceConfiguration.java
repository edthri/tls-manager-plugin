package org.openintegrationengine.tlsmanager.server.connectorconfig;

import com.mirth.connect.connectors.ws.DefaultWebServiceConfiguration;
import com.mirth.connect.connectors.ws.SSLSocketFactoryWrapper;
import com.mirth.connect.connectors.ws.WebServiceDispatcher;
import com.mirth.connect.connectors.ws.WebServiceDispatcherProperties;
import com.mirth.connect.donkey.server.channel.Connector;
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.server.SocketFactoryService;
import org.openintegrationengine.tlsmanager.server.TLSServicePlugin;
import org.openintegrationengine.tlsmanager.shared.models.WeirdIntermediaryContextContainer;
import org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties;

import javax.net.ssl.SSLSocketFactory;
import java.util.Map;

@Slf4j
public class TLSWebServiceConfiguration extends DefaultWebServiceConfiguration {

    private final SocketFactoryService socketFactoryService;

    private WeirdIntermediaryContextContainer contextContainer;

    public TLSWebServiceConfiguration() {
        // This looks ugly, I know
        this(TLSServicePlugin.getPluginInstance().getSocketFactoryService());
    }

    public TLSWebServiceConfiguration(SocketFactoryService socketFactoryService) {
        this.socketFactoryService = socketFactoryService;
    }

    @Override
    public void configureConnectorDeploy(Connector connector) throws Exception {
        if (connector instanceof WebServiceDispatcher webServiceDispatcher) {
            configureSocketFactory(webServiceDispatcher);
        }
    }

    @Override
    public void configureDispatcher(WebServiceDispatcher connector, WebServiceDispatcherProperties connectorProperties, Map<String, Object> requestContext) throws Exception {
        SSLSocketFactory socketFactory = new SSLSocketFactoryWrapper(
            contextContainer.sslContext().getSocketFactory(),
            contextContainer.protocols(),
            contextContainer.ciphers()
        );

        // Wat?
        requestContext.put("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory", socketFactory);
        requestContext.put("com.sun.xml.ws.transport.https.client.SSLSocketFactory", socketFactory); // JAX-WS RI
    }

    private void configureSocketFactory(WebServiceDispatcher connector) {
        var tlsConnectorProperties = connector.getConnectorProperties().getPluginProperties()
            .stream()
            .filter(TLSConnectorProperties.class::isInstance)
            .findFirst()
            .map(TLSConnectorProperties.class::cast)
            .orElse(null);

        if (tlsConnectorProperties != null && tlsConnectorProperties.isTlsManagerEnabled()) {
            contextContainer = socketFactoryService.generateSSLContext(connector, tlsConnectorProperties);

            var socketConnectionFactory = socketFactoryService.getConnectorSocketFactory(contextContainer);
            if (socketConnectionFactory != null) {
                connector.getSocketFactoryRegistry().register("https", socketConnectionFactory);
            }
        } else {
            try {
                super.configureConnectorDeploy(connector);
            } catch (Exception e) {
                log.error("Error creating non-TLS socket factory", e);
                throw new RuntimeException(e);
            }
        }
    }
}
