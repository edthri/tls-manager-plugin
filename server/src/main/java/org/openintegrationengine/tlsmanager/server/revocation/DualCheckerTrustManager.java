package org.openintegrationengine.tlsmanager.server.revocation;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CRL;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public final class DualCheckerTrustManager extends X509ExtendedTrustManager {

    private final KeyStore trustStore;
    private final RevocationMode ocspMode, crlMode;
    private final Collection<? extends CRL> preloadedCrls; // optional (in addition to CRLDP)

    public DualCheckerTrustManager(
        KeyStore trustStore,
        RevocationMode ocspMode,
        RevocationMode crlMode,
        Collection<? extends CRL> preloadedCrls
    ) {
        this.trustStore = trustStore;
        this.ocspMode = ocspMode;
        this.crlMode = crlMode;
        this.preloadedCrls = preloadedCrls == null ? List.of() : preloadedCrls;
    }

    // --- JSSE delegation ---
    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {  }
    @Override public void checkClientTrusted(X509Certificate[] chain, String authType, Socket s) throws CertificateException { }
    @Override public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine e) throws CertificateException { }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        validate(chain, null);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket s) throws CertificateException {
        validate(chain, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine e) {
        throw new UnsupportedOperationException("SSLEngine not supported");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }


    // --- Core: run two separate PKIX passes, each with its own PKIXRevocationChecker ---
    private void validate(X509Certificate[] chain, Socket socket) throws CertificateException {
        try {
            Set<TrustAnchor> anchors = anchorsFrom(trustStore);
            if (anchors.isEmpty()) {
                throw new CertificateException("No trust anchors found in truststore");
            }

            var certificateFactory = CertificateFactory.getInstance("X.509");
            var certPath = certificateFactory.generateCertPath(List.of(chain));

            // Baseline chain sanity (revocation OFF) to get clean path errors early.
            var base = new PKIXParameters(anchors);
            base.setRevocationEnabled(false);

            CertPathValidator.getInstance("PKIX").validate(certPath, base);

            // OCSP-only pass (if requested)
            if (ocspMode != RevocationMode.DISABLED) {

                boolean hasStapledOcsp = false;

                if (socket instanceof SSLSocket sslSocket) {
                    SSLSession session = sslSocket.getHandshakeSession();

                    if (session instanceof ExtendedSSLSession extendedSession) {
                        var statusResponses = extendedSession.getStatusResponses();

                        if (statusResponses != null && !statusResponses.isEmpty()) {
                            log.info("Received {} stapled OCSP response(s)", statusResponses.size());

                            for (int i = 0; i < Math.min(statusResponses.size(), chain.length); i++) {
                                byte[] response = statusResponses.get(i);
                                if (response != null && response.length > 0) {
                                    validateStapledOcspResponse(response, chain[i]);
                                    hasStapledOcsp = true;
                                }
                            }
                        }
                    }
                }


                if (!hasStapledOcsp) {
                    pkixOcspOnly(certPath, anchors, ocspMode == RevocationMode.SOFT_FAIL);
                }
            }

            // CRL-only pass (if requested)
            if (crlMode != RevocationMode.DISABLED) {
                // Preloaded CRLs + CRLDP-fetched CRLs (HTTP)
                List<CRL> crls = new ArrayList<>(preloadedCrls);
                crls.addAll(fetchCrlsFromCrlDP(chain));
                pkixCrlOnly(certPath, anchors, crls, crlMode == RevocationMode.SOFT_FAIL);
            }
            // If both are HARD_FAIL, reaching here means both passes succeeded.

        } catch (GeneralSecurityException e) {
            if (e instanceof CertificateException exception) {
                throw exception;
            }

            throw new CertificateException("Validation error: " + e.getMessage(), e);
        }
    }

    // ---- Pass A: OCSP-only ----
    private void pkixOcspOnly(CertPath path, Set<TrustAnchor> anchors, boolean softFail) throws GeneralSecurityException {
        var params = new PKIXParameters(anchors);
        params.setRevocationEnabled(true);

        var certPathValidator = CertPathValidator.getInstance("PKIX");
        var revocationChecker = (PKIXRevocationChecker) certPathValidator.getRevocationChecker();

        var opts = EnumSet.of(
            PKIXRevocationChecker.Option.NO_FALLBACK // don't fall back to CRLs
        );

        if (softFail) opts.add(PKIXRevocationChecker.Option.SOFT_FAIL);

        revocationChecker.setOptions(opts);
        params.addCertPathChecker(revocationChecker);

        certPathValidator.validate(path, params);
    }

    // ---- Pass B: CRL-only ----
    private void pkixCrlOnly(CertPath path, Set<TrustAnchor> anchors, Collection<? extends CRL> crls, boolean softFail) throws GeneralSecurityException {
        var params = new PKIXParameters(anchors);
        params.setRevocationEnabled(true);

        if (crls != null && !crls.isEmpty()) {
            CertStore cs = CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls));
            params.addCertStore(cs);
        }

        var certPathValidator = CertPathValidator.getInstance("PKIX");
        var revocationChecker = (PKIXRevocationChecker) certPathValidator.getRevocationChecker();

        var opts = EnumSet.of(
            PKIXRevocationChecker.Option.PREFER_CRLS, // CRL-first
            PKIXRevocationChecker.Option.NO_FALLBACK  // do NOT fall back to OCSP
        );

        if (softFail) {
            opts.add(PKIXRevocationChecker.Option.SOFT_FAIL);
        }

        revocationChecker.setOptions(opts);
        params.addCertPathChecker(revocationChecker);

        certPathValidator.validate(path, params);
    }

    // ---- Helpers: anchors, AIA->OCSP URL, CRLDP fetch (HTTP) ----
    private static Set<TrustAnchor> anchorsFrom(KeyStore ks) throws KeyStoreException {
        Set<TrustAnchor> out = new HashSet<>();
        for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
            var alias = e.nextElement();
            var certificate = ks.getCertificate(alias);
            if (certificate instanceof X509Certificate x509Certificate) {
                out.add(new TrustAnchor(x509Certificate, null));
            } else {
                log.debug("Ignoring non-X.509 certificate {} in truststore", certificate);
            }
        }
        return out;
    }

    private static Collection<? extends CRL> fetchCrlsFromCrlDP(X509Certificate[] chain) {
        List<CRL> out = new ArrayList<>();
        try {
            var certificateFactory = CertificateFactory.getInstance("X.509");
            for (X509Certificate cert : chain) {
                byte[] ext = cert.getExtensionValue(Extension.cRLDistributionPoints.getId());
                if (ext == null) continue;

                byte[] inner = ((DEROctetString) ASN1Primitive.fromByteArray(ext)).getOctets();

                var crlDistPoint = CRLDistPoint.getInstance(ASN1Primitive.fromByteArray(inner));
                for (DistributionPoint p : crlDistPoint.getDistributionPoints()) {
                    var name = p.getDistributionPoint();
                    if (name == null || name.getType() != DistributionPointName.FULL_NAME) continue;

                    for (GeneralName gn : GeneralNames.getInstance(name.getName()).getNames()) {
                        if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                            String uri = gn.getName().toString();
                            if (!uri.startsWith("http://")) continue; // keep simple; avoid HTTPS recursion

                            try (InputStream in = URI.create(uri).toURL().openStream()) {
                                byte[] bytes = in.readAllBytes();
                                byte[] der = maybeDecodePem(bytes, "X509 CRL");
                                out.add(certificateFactory.generateCRL(new ByteArrayInputStream(der)));
                            } catch (Exception ignoreOne) {}
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return out;
    }

    private static byte[] maybeDecodePem(byte[] content, String type) {
        String s = new String(content);
        if (!s.contains("-----BEGIN " + type)) return content;
        String base64 = s.replaceAll("-----BEGIN [^-]+-----", "")
            .replaceAll("-----END [^-]+-----", "")
            .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private void validateStapledOcspResponse(byte[] responseBytes, X509Certificate cert)
        throws CertificateException {
        try {
            var ocspResponse = new OCSPResp(responseBytes);

            if (ocspResponse.getStatus() == OCSPResp.SUCCESSFUL) {
                var basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();

                for (var singleResp : basicResponse.getResponses()) {
                    var status = singleResp.getCertStatus();

                    if (status == CertificateStatus.GOOD) {
                        log.debug("Stapled OCSP: Certificate is GOOD");
                    } else if (status instanceof RevokedStatus) {
                        throw new CertificateException("Certificate is REVOKED (from stapled OCSP)");
                    } else {
                        log.warn("Stapled OCSP: Certificate status is UNKNOWN");
                    }
                }
            } else {
                log.warn("Stapled OCSP response status: {}", ocspResponse.getStatus());
            }
        } catch (Exception e) {
            log.error("Failed to validate stapled OCSP response", e);
            if (ocspMode == RevocationMode.HARD_FAIL) {
                throw new CertificateException("Invalid stapled OCSP response", e);
            }
        }
    }
}
