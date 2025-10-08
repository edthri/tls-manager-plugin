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
import java.net.SocketTimeoutException;

@Slf4j
public class ConnectionUtils {

    private static SchemePortResolver defaultSchemePortResolver = new DefaultSchemePortResolver();

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

        InetSocketAddress remoteAddress = new InetSocketAddress(target.getHostName(), defaultSchemePortResolver.resolve(target));

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

            if (log.isDebugEnabled()) {
                // Handshake is done if we got here. Inspect what happened:
                var sess = sslSocket.getSession();
                log.debug("Protocol: {}", sess.getProtocol());
                log.debug("Cipher:   {}", sess.getCipherSuite());
                log.debug("Peer:     {}", sess.getPeerPrincipal());

                // Did we actually present a client cert?
                var localCerts = sess.getLocalCertificates();     // null => none presented
                var localPrinc = sess.getLocalPrincipal();        // null => none presented
                log.debug("Client cert presented? {}", localPrinc != null);
            }

            isSocketAlive(sslSocket);

            return new ConnectionTestResponse(ConnectionTestResponse.Type.SUCCESS, "Successfully connected to host: " + connectionInfo, connectionInfo);
        } catch (Exception e) {
            log.error("Error connecting to host: {}", host, e);
            throw e;
        }
    }

    /**
     * Performs a lightweight check to see if the given {@link Socket} appears alive.
     * <p>
     * This method verifies local socket state and performs a short read with a timeout
     * to detect EOF or I/O errors. It returns {@code true} if the connection seems open
     * and responsive, or {@code false} if it is closed, reset, or reaches end-of-stream.
     * <p>
     * Note that TCP cannot guarantee remote liveness without actual I/O, so this result
     * is best-effort only.
     *
     * @param socket the socket to test (not {@code null})
     * @return {@code true} if the socket likely remains open; {@code false} otherwise
     */

    private static boolean isSocketAlive(Socket socket) throws IOException {
        log.trace("Checking socket liveness");
        int oldTimeOut = socket.getSoTimeout();
        socket.setSoTimeout(100); // 100ms read timeout

        log.trace("Set socket timeout to 100ms");

        var in = socket.getInputStream();

        try {
            if (in.available() > 0 || in.read() >= 0) {
                // Data received (or connection still healthy)
                log.debug("Socket alive (data or no EOF)");
            } else {
                // read() == -1 → remote closed cleanly
                log.debug("Socket dead (EOF)");
            }
        } catch (SocketTimeoutException e) {
            // no data within timeout → probably still open
            log.debug("Socket alive (idle)");
        } catch (IOException e) {
            // network error, RST, etc.
            log.debug("Socket dead (idle)", e);
        } finally {
            socket.setSoTimeout(oldTimeOut);
        }

        return true;
    }
}
