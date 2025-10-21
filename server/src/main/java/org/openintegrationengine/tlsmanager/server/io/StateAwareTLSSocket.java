package org.openintegrationengine.tlsmanager.server.io;

import com.mirth.connect.connectors.tcp.StateAwareSocket;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class StateAwareTLSSocket extends StateAwareSocket {

    private final SSLConnectionSocketFactory socketFactory;

    private Socket sslSocket;

    private boolean isClosing;

    public StateAwareTLSSocket(SSLConnectionSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
        this.isClosing = false;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        this.connect(endpoint, 0);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        // Step 1: Perform the plain TCP connection first
        super.connect(endpoint, timeout);

        // Step 2: Layer TLS on top using createLayeredSocket
        if (endpoint instanceof InetSocketAddress inet) {
            String host = inet.getHostString();
            int port = inet.getPort();

            // createLayeredSocket() will internally call SSLSocketFactory.createSocket()
            this.sslSocket = socketFactory.createLayeredSocket(this, host, port, null);
        } else {
            throw new IOException("Expected InetSocketAddress for TLS connection");
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (sslSocket != null) {
            return sslSocket.getInputStream();
        }
        return super.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (sslSocket != null) {
            return sslSocket.getOutputStream();
        }
        return super.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        if (isClosing) {
            // Prevent re-entry when sslSocket tries to close the underlying socket
            super.close();
            return;
        }

        isClosing = true;
        try {
            if (sslSocket != null) {
                sslSocket.close();
            } else {
                super.close();
            }
        } finally {
            isClosing = false;
        }
    }

    @Override
    public boolean remoteSideHasClosed() throws IOException {
        return super.remoteSideHasClosed();
    }
}
