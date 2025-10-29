package org.openintegrationengine.tlsmanager.server.connectorconfig;

import com.mirth.connect.connectors.tcp.DefaultTcpConfiguration;
import com.mirth.connect.connectors.tcp.StateAwareSocket;
import com.mirth.connect.connectors.tcp.TcpDispatcher;
import com.mirth.connect.donkey.server.channel.Connector;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.openintegrationengine.tlsmanager.server.SocketFactoryService;
import org.openintegrationengine.tlsmanager.server.TLSServicePlugin;
import org.openintegrationengine.tlsmanager.server.io.StateAwareTLSSocket;
import org.openintegrationengine.tlsmanager.shared.properties.TLSSenderProperties;

import java.net.Socket;

public class TLSTcpConfiguration extends DefaultTcpConfiguration {

    private final SocketFactoryService socketFactoryService;

    private TLSSenderProperties tlsSenderProperties;

    private SSLConnectionSocketFactory socketFactory;

    public TLSTcpConfiguration() {
        this(TLSServicePlugin.getPluginInstance().getSocketFactoryService());
    }

    public TLSTcpConfiguration(SocketFactoryService socketFactoryService) {
        this.socketFactoryService = socketFactoryService;
    }

    @Override
    public void configureConnectorDeploy(Connector connector) throws Exception {
        var tcpDispatcher = (TcpDispatcher) connector;

        this.tlsSenderProperties = tcpDispatcher.getConnectorProperties().getPluginProperties()
            .stream()
            .filter(TLSSenderProperties.class::isInstance)
            .findFirst()
            .map(TLSSenderProperties.class::cast)
            .orElse(null);

        if (tlsSenderProperties != null && tlsSenderProperties.isTlsManagerEnabled()) {
            socketFactory = socketFactoryService.getConnectorSocketFactory(tcpDispatcher, tlsSenderProperties);
        }
    }

    @Override
    public Socket createSocket() {
        if (tlsSenderProperties == null || !tlsSenderProperties.isTlsManagerEnabled()) {
            return new StateAwareSocket();
        } else {
            if (socketFactory == null) {
                throw new IllegalStateException("TLS for TCP connections is enabled, but socket factory is null. Possibly because no trust anchors were found.");
            }
            return new StateAwareTLSSocket(socketFactory);
        }
    }
}
