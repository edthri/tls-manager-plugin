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
import com.mirth.connect.client.core.Operation;
import com.mirth.connect.client.core.api.BaseServletInterface;
import com.mirth.connect.client.core.api.MirthOperation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openintegrationengine.tlsmanager.shared.TLSPluginConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Path("/tlsmanager")
@Tag(name = TLSPluginConstants.PLUGIN_POINTNAME)
@Consumes({ APPLICATION_XML, APPLICATION_JSON })
@Produces({ APPLICATION_XML, APPLICATION_JSON })
@MirthApiProvider(type = ApiProviderType.SERVLET_INTERFACE)
public interface TLSServletInterface extends BaseServletInterface {

    @GET
    @Path("/importedcertificates")
    @Produces({ APPLICATION_XML, APPLICATION_JSON })
    @ApiResponse(responseCode = "200", description = "Found the information",
        content = {
            @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = List.class)),
            @Content(mediaType = APPLICATION_XML, schema = @Schema(implementation = List.class))
        })
    @MirthOperation(
        name = "getImportedCertificates",
        display = "Get list of imported certificates",
        type = Operation.ExecuteType.ASYNC
    )
    Set<String> getImportedCertificates();

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
    @Path("/keystore")
    @Consumes({ APPLICATION_OCTET_STREAM })
    @ApiResponse(
        responseCode = "200",
        description = "Write the additional keystore from the given byte array",
        content = {
            @Content(mediaType = TEXT_PLAIN, schema = @Schema(implementation = String.class)),
        })
    @MirthOperation(
        name = "setTlsKeystore",
        display = "Write the additional keystore from the given byte array",
        type = Operation.ExecuteType.ASYNC
    )
    void setKeystore(
        @RequestBody byte[] fileBytes,
        @HeaderParam("X-Keystore-Password") String password
    );
}
