package org.openintegrationengine.tlsmanager.server.connectorconfig;

import com.mirth.connect.connectors.tcp.DefaultTcpConfiguration;
import com.mirth.connect.connectors.tcp.StateAwareServerSocket;
import com.mirth.connect.connectors.tcp.StateAwareSocket;
import com.mirth.connect.connectors.tcp.TcpDispatcher;
import com.mirth.connect.connectors.tcp.TcpReceiver;
import com.mirth.connect.donkey.server.channel.Connector;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.openintegrationengine.tlsmanager.server.SocketFactoryService;
import org.openintegrationengine.tlsmanager.server.TLSServicePlugin;
import org.openintegrationengine.tlsmanager.server.io.StateAwareTLSServerSocket;
import org.openintegrationengine.tlsmanager.server.io.StateAwareTLSSocket;
import org.openintegrationengine.tlsmanager.shared.properties.TLSListenerProperties;
import org.openintegrationengine.tlsmanager.shared.properties.TLSSenderProperties;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class TLSTcpConfiguration extends DefaultTcpConfiguration {

    private final SocketFactoryService socketFactoryService;

    private TLSSenderProperties tlsSenderProperties;
    private TLSListenerProperties tlsListenerProperties;

    private SSLConnectionSocketFactory socketFactory;

    private Connector connector;

    public TLSTcpConfiguration() {
        this(TLSServicePlugin.getPluginInstance().getSocketFactoryService());
    }

    public TLSTcpConfiguration(SocketFactoryService socketFactoryService) {
        this.socketFactoryService = socketFactoryService;
    }

    @Override
    public void configureConnectorDeploy(Connector connector) throws Exception {
        this.connector = connector;

        if (connector instanceof TcpDispatcher tcpDispatcher) {
            this.tlsSenderProperties = getConnectorProperties(TLSSenderProperties.class, tcpDispatcher);

            if (tlsSenderProperties != null && tlsSenderProperties.isTlsManagerEnabled()) {
                socketFactory = socketFactoryService.getConnectorSocketFactory(tcpDispatcher, tlsSenderProperties);
            }
        } else if (connector instanceof TcpReceiver tcpReceiver) {
            this.tlsListenerProperties = getConnectorProperties(TLSListenerProperties.class, tcpReceiver);
        } else {
            // should not get here
            throw new IllegalStateException("Unexpected connector type: %s".formatted(connector.getClass().getCanonicalName()));
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

    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        log.error("Unexpected call to createServerSocket(int, int)");
        return super.createServerSocket(port, backlog);
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        var createTlsSocket = tlsListenerProperties != null && tlsListenerProperties.isTlsManagerEnabled();

        log.debug(
            "Creating server socket. Properties null - {}; Manager enabled - {}",
            tlsListenerProperties == null,
            createTlsSocket
        );

        if (createTlsSocket) {
            var contextContainer = socketFactoryService.generateTLSContext(connector, tlsListenerProperties);
            return new StateAwareTLSServerSocket(port, backlog, bindAddr, contextContainer);
        } else {
            return new StateAwareServerSocket(port, backlog, bindAddr);
        }
    }

    private <T> T getConnectorProperties(Class<T> propertiesClass, Connector connector) {
        return connector.getConnectorProperties().getPluginProperties()
            .stream()
            .filter(propertiesClass::isInstance)
            .findFirst()
            .map(propertiesClass::cast)
            .orElse(null);
    }
}
