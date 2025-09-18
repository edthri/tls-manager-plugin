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
import com.mirth.connect.server.api.MirthServlet;
import lombok.extern.slf4j.Slf4j;
import org.openintegrationengine.tlsmanager.server.CertificateService;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;
import org.openintegrationengine.tlsmanager.shared.servlet.TLSServletInterface;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@MirthApiProvider(type = ApiProviderType.SERVER_CLASS)
public class TLSServlet extends MirthServlet implements TLSServletInterface {

    public TLSServlet(@Context HttpServletRequest request, @Context SecurityContext sc) {
        super(request, sc, TLSPluginConstants.PLUGIN_POINTNAME);
    }

    @Override
    public List<String> getImportedCertificates() {
        var additionalTruststore = CertificateService.getInstance().getTruststore();

        Enumeration<String> aliasEnumeration;
        try {
            aliasEnumeration = additionalTruststore.aliases();
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }

        var aliasList = new ArrayList<String>();
        while (aliasEnumeration.hasMoreElements()) {
            var alias = aliasEnumeration.nextElement();

            aliasList.add(alias);
        }

        return aliasList;
    }

    @Override
    public byte[] getKeystore() {
        var keystore = CertificateService.getInstance().getTruststore();

        try (var baos = new ByteArrayOutputStream()) {
            keystore.store(baos, "changeit".toCharArray());
            return baos.toByteArray();
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setKeystore(byte[] keystore, String password) {
        log.info("Stubbed setKeystore method call");
    }
}

