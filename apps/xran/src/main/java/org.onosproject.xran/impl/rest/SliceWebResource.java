/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.xran.impl.rest;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.xran.XranStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Slice web resource.
 */
@Path("slice")
public class SliceWebResource extends AbstractWebResource {

    private static final Logger log =
            LoggerFactory.getLogger(SliceWebResource.class);

    public SliceWebResource() {
    }

    /**
     * List the slice with the given slice ID.
     *
     * @param sliceid ID of the slice
     * @return Response
     */
    @GET
    @Path("{sliceid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 404, message = "HTTP_NOT_FOUND")
    })
    public Response getSlice(@PathParam("sliceid") long sliceid) {
        return get(XranStore.class).getSlice(sliceid).map(slice -> {
            try {
                JsonNode jsonNode = mapper().valueToTree(slice);

                return ResponseHelper.getResponse(
                        mapper(),
                        HttpURLConnection.HTTP_OK,
                        jsonNode
                );
            } catch (Exception e) {
                String fullStackTrace = ExceptionUtils.getFullStackTrace(e);
                log.error(fullStackTrace);

                return ResponseHelper.getResponse(
                        mapper(),
                        HttpURLConnection.HTTP_INTERNAL_ERROR,
                        "Exception",
                        fullStackTrace
                );
            }
        }).orElse(
                ResponseHelper.getResponse(
                        mapper(),
                        HttpURLConnection.HTTP_NOT_FOUND,
                        "Not Found",
                        "Slice " + sliceid + " not found"
                )
        );
    }

    /**
     * Create slice with the corresponding attributes.
     *
     * @param stream Attributes to create slice
     * @return Response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 501, message = "HTTP_NOT_IMPLEMENTED")
    })
    public Response postSlice(InputStream stream) {
        try {
//            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
//            get(XranStore.class).createSlice(jsonTree);

            // FIXME: change when implemented
            return ResponseHelper.getResponse(
                    mapper(),
                    HttpURLConnection.HTTP_NOT_IMPLEMENTED,
                    "Not Implemented",
                    "POST Slice not implemented"
            );
        } catch (Exception e) {
            String fullStackTrace = ExceptionUtils.getFullStackTrace(e);
            log.error(fullStackTrace);
            e.printStackTrace();

            return ResponseHelper.getResponse(
                    mapper(),
                    HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "Exception",
                    fullStackTrace
            );
        }
    }

}
