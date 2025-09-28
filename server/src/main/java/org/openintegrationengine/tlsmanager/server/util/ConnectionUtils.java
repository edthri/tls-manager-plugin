package org.openintegrationengine.tlsmanager.server.util;

import com.mirth.connect.util.ConnectionTestResponse;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ConnectionUtils {

    public static ConnectionTestResponse thing(
        SSLConnectionSocketFactory socketFactory,
        String host,
        int port,
        int timeout,
        String localAddr,
        int localPort
    ) throws IOException {
        if (
            host == null
            || host.isEmpty()
            || port <= 0
            || port > 65535
        ) {
            return new ConnectionTestResponse(ConnectionTestResponse.Type.FAILURE, "Invalid host or port.");
        }

        // TODO Dynamic scheme
        var target = new HttpHost(host, port, "https");

        InetSocketAddress remoteAddress = new InetSocketAddress(target.getHostName(), target.getPort());

        InetSocketAddress localAddress = null;
        if (localAddr != null) {
            if (
                localAddr.isEmpty()
                || localPort <= 0
                || localPort > 65535
            ) {
                return new ConnectionTestResponse(ConnectionTestResponse.Type.FAILURE, "Invalid local host or port.");
            }

            localAddress = new InetSocketAddress(localAddr, localPort);
        }


        try (
            var sslSocket = (SSLSocket) socketFactory.connectSocket(
                timeout,
                null,
                target,
                remoteAddress,
                localAddress,
                null
            )
        ) {
            var connectionInfo = "%s:%d -> %s:%d".formatted(
                sslSocket.getLocalAddress().getHostAddress(),
                sslSocket.getLocalPort(),
                remoteAddress.getAddress().getHostAddress(),
                remoteAddress.getPort()
            );

            return new ConnectionTestResponse(ConnectionTestResponse.Type.SUCCESS, "Successfully connected to host: " + connectionInfo, connectionInfo);
        }
    }
}
