package org.openintegrationengine.tlsmanager.server.util;

import com.mirth.connect.util.ConnectionTestResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultSchemePortResolver;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
public class ConnectionUtils {

    private static SchemePortResolver defaultResolver = new DefaultSchemePortResolver();

    public static ConnectionTestResponse thing(
        SSLConnectionSocketFactory socketFactory,
        String host,
        int timeout,
        String localAddr,
        int localPort
    ) throws IOException {
        return thing(
            socketFactory,
            null,
            host,
            timeout,
            localAddr,
            localPort
        );
    }

    public static ConnectionTestResponse thing(
        SSLConnectionSocketFactory socketFactory,
        Socket socket,
        String host,
        int timeout,
        String localAddr,
        int localPort
    ) throws IOException {
        if (
            host == null
            || host.isEmpty()
        ) {
            return new ConnectionTestResponse(ConnectionTestResponse.Type.FAILURE, "Invalid host or port.");
        }

        var target = HttpHost.create(host);

        InetSocketAddress remoteAddress = new InetSocketAddress(target.getHostName(), defaultResolver.resolve(target));

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
                socket,
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
        } catch (Exception e) {
            log.error("Error connecting to host: " + host, e);
            return new ConnectionTestResponse(ConnectionTestResponse.Type.FAILURE, e.getMessage());
        }
    }
}
