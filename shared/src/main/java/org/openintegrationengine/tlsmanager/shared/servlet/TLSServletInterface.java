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

package org.openintegrationengine.tlsmanager.shared.servlet;

import com.kaurpalang.mirth.annotationsplugin.annotation.MirthApiProvider;
import com.kaurpalang.mirth.annotationsplugin.type.ApiProviderType;
import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.core.Operation;
import com.mirth.connect.client.core.api.BaseServletInterface;
import com.mirth.connect.client.core.api.MirthOperation;
import com.mirth.connect.client.core.api.Param;
import com.mirth.connect.connectors.http.HttpConnectorServletInterface;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

@Path("/tlsmanager")
@Tag(name = TLSPluginConstants.PLUGIN_POINTNAME)
@Consumes({ APPLICATION_XML, APPLICATION_JSON })
@Produces({ APPLICATION_XML, APPLICATION_JSON })
@MirthApiProvider(type = ApiProviderType.SERVLET_INTERFACE)
public interface TLSServletInterface extends BaseServletInterface, HttpConnectorServletInterface {

    @GET
    @Path("/importedcertificates")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    @ApiResponse(responseCode = "200", description = "Found the information",
        content = {
            @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Set.class)),
            @Content(mediaType = APPLICATION_XML, schema = @Schema(implementation = Set.class))
        })
    @MirthOperation(
        name = "getImportedCertificates",
        display = "Get list of imported certificates",
        type = Operation.ExecuteType.ASYNC
    )
    Set<String> getPublicCertificates();

    @GET
    @Path("/keystore")
    @Produces({ APPLICATION_OCTET_STREAM })
    @ApiResponse(
        responseCode = "200",
        description = "Retrieve current additional keystore as byte array",
        content = {
            @Content(mediaType = APPLICATION_OCTET_STREAM, schema = @Schema(type = "string", format = "binary")),
        })
    @MirthOperation(
        name = "getTlsKeystore",
        display = "Retrieve current additional keystore as byte array",
        type = Operation.ExecuteType.ASYNC
    )
    byte[] getKeystore();

    @POST
    @Path("/truststore")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Overwrite the in use truststore"
    )
    @MirthOperation(
        name = "setTlsKeystore",
        display = "Write the additional truststore from the given byte array",
        type = Operation.ExecuteType.ASYNC
    )
    String setTruststore(
        @Param("inputStream")
        @Parameter(
            description = "The truststore file to upload.",
            schema = @Schema(description = "The truststore file to upload.", type = "string", format = "binary")
        )
        @FormDataParam("file") InputStream inputStream,

        @Param("password")
        @Parameter(description = "Truststore password")
        @Schema(description = "Truststore password", type = "string")
        @FormDataParam("password")
        String password
    ) throws ClientException;
}
