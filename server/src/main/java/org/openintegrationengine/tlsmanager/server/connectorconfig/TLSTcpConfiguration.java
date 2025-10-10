package org.openintegrationengine.tlsmanager.server.connectorconfig;

import com.mirth.connect.connectors.tcp.DefaultTcpConfiguration;
import com.mirth.connect.connectors.tcp.StateAwareSocket;
import com.mirth.connect.connectors.tcp.TcpDispatcher;
import com.mirth.connect.donkey.server.channel.Connector;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.openintegrationengine.tlsmanager.server.SocketFactoryService;
import org.openintegrationengine.tlsmanager.server.TLSServicePlugin;
import org.openintegrationengine.tlsmanager.server.io.StateAwareTLSSocket;
import org.openintegrationengine.tlsmanager.shared.properties.TLSConnectorProperties;

import java.net.Socket;

public class TLSTcpConfiguration extends DefaultTcpConfiguration {

    private final SocketFactoryService socketFactoryService;

    private TLSConnectorProperties tlsConnectorProperties;

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

        this.tlsConnectorProperties = tcpDispatcher.getConnectorProperties().getPluginProperties()
            .stream()
            .filter(TLSConnectorProperties.class::isInstance)
            .findFirst()
            .map(TLSConnectorProperties.class::cast)
            .orElse(null);

        if (tlsConnectorProperties != null && tlsConnectorProperties.isTlsManagerEnabled()) {
            socketFactory = socketFactoryService.getConnectorSocketFactory(tcpDispatcher, tlsConnectorProperties);
        }
    }

    @Override
    public Socket createSocket() {
        if (tlsConnectorProperties == null || !tlsConnectorProperties.isTlsManagerEnabled()) {
            return new StateAwareSocket();
        } else {
            return new StateAwareTLSSocket(socketFactory);
        }
    }
}
