package org.openintegrationengine.tlsmanager.server.io;

import com.mirth.connect.connectors.tcp.StateAwareSocket;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class StateAwareTLSSocket extends StateAwareSocket {

    private final SSLConnectionSocketFactory socketFactory;

    private Socket delegate;

    private boolean isClosing;

    public StateAwareTLSSocket(SSLConnectionSocketFactory socketFactory) {
        super();
        this.socketFactory = socketFactory;
        this.isClosing = false;
    }

    public StateAwareTLSSocket(SSLSocket delegate) {
        super();
        this.delegate = delegate;
        this.socketFactory = null;
        this.isClosing = false;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        this.connect(endpoint, 0);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        // Perform the plain TCP connection first
        super.connect(endpoint, timeout);

        // Layer TLS on top using createLayeredSocket
        if (endpoint instanceof InetSocketAddress inet) {
            String host = inet.getHostString();
            int port = inet.getPort();

            // createLayeredSocket() will internally call SSLSocketFactory.createSocket()
            this.delegate = socketFactory.createLayeredSocket(this, host, port, null);
        } else {
            throw new IOException("Expected InetSocketAddress for TLS connection");
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (this.bis == null) {
            var inputStream = delegate != null ? delegate.getInputStream() : super.getInputStream();
            this.bis = new BufferedInputStream(inputStream);
        }

        return this.bis;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (delegate != null) {
            return delegate.getOutputStream();
        }
        return super.getOutputStream();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return delegate == null ? super.getRemoteSocketAddress() : delegate.getRemoteSocketAddress();
    }

    @Override
    public InetAddress getInetAddress() {
        return delegate == null ? super.getInetAddress() : delegate.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return delegate == null ? super.getLocalAddress() : delegate.getLocalAddress();
    }

    @Override
    public int getPort() {
        return delegate == null ? super.getPort() : delegate.getPort();
    }

    @Override
    public boolean isInputShutdown() {
        return delegate == null ? super.isInputShutdown() : delegate.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return delegate == null ? super.isOutputShutdown() : delegate.isOutputShutdown();
    }

    @Override
    public void shutdownOutput() throws IOException {
        if (delegate != null) {
            delegate.shutdownOutput();
        } else {
            super.shutdownOutput();
        }
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegate == null ? super.getLocalSocketAddress() : delegate.getLocalSocketAddress();
    }

    @Override
    public int getLocalPort() {
        return delegate == null ? super.getLocalPort() : delegate.getLocalPort();
    }

    @Override
    public void shutdownInput() throws IOException {
        if (delegate != null) {
            delegate.shutdownInput();
        } else {
            super.shutdownInput();
        }
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
            if (delegate != null) {
                delegate.close();
            } else {
                super.close();
            }
        } finally {
            isClosing = false;
        }
    }

    @Override
    public boolean remoteSideHasClosed() throws IOException {
        if (delegate != null) {
            return remoteSideHasClosedInternal();
        }
        return super.remoteSideHasClosed();
    }

    private boolean remoteSideHasClosedInternal() throws IOException {
        if (delegate.isClosed()) {
            return true;
        }

        int oldTimeout;
        try {
            oldTimeout = delegate.getSoTimeout();
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Socket closed")) {
               return true;
            }

            throw e;
        }

        delegate.setSoTimeout(100);
        this.getInputStream().mark(1);

        try {
            return bis.read() == -1;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                bis.reset();
            } catch (IOException ignored) {
            }

            try {
                delegate.setSoTimeout(oldTimeout);
            } catch (SocketException ignored) {
            }
        }
    }
}
