package org.openintegrationengine.tlsmanager.shared.models;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

public record WeirdIntermediaryContextContainer (
    SSLContext sslContext,
    String[] protocols,
    String[] ciphers,
    HostnameVerifier hostnameVerifier
) {}
