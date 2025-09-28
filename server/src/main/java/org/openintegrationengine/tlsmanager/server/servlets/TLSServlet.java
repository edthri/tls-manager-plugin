/*
 * Copyright 2025 Kaur Palang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintegrationengine.tlsmanager.server.servlets;

import com.kaurpalang.mirth.annotationsplugin.annotation.MirthApiProvider;
import com.kaurpalang.mirth.annotationsplugin.type.ApiProviderType;
import com.mirth.connect.server.api.DontCheckAuthorized;
import com.mirth.connect.server.api.MirthServlet;
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.server.CertificateService;
import org.openintegrationengine.tlsmanager.server.TLSServicePlugin;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.servlet.TLSServletInterface;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Set;

@Slf4j
@MirthApiProvider(type = ApiProviderType.SERVER_CLASS)
public class TLSServlet extends MirthServlet implements TLSServletInterface {

    private CertificateService certificateService;

    public TLSServlet(@Context HttpServletRequest request, @Context SecurityContext sc) {
        this(
            request,
            sc,
            TLSServicePlugin.getPluginInstance().getCertificateService()
        );
    }

    public TLSServlet(
        @Context HttpServletRequest request,
        @Context SecurityContext sc,
        CertificateService certificateService
    ) {
        super(request, sc, TLSPluginConstants.PLUGIN_POINTNAME);

        this.certificateService = certificateService;
    }

    @Override
    public Set<String> getImportedCertificates() {
        return certificateService.getLoadedAliases();
    }

    @Override
    public byte[] getKeystore() {
        var keystore = certificateService.getExtraTrustStore();

        try (var baos = new ByteArrayOutputStream()) {
            keystore.store(baos, "changeit".toCharArray());
            return baos.toByteArray();
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @DontCheckAuthorized
    public String setTruststore(InputStream inputStream, String password) {

        if (!isUserAuthorized(false)) {
            isUserAuthorized(true);
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        byte[] trustStoreBytes;
        try {
            trustStoreBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        certificateService.storeExtraTrustStore(trustStoreBytes, password.toCharArray());
        return "timmis";
    }
}

