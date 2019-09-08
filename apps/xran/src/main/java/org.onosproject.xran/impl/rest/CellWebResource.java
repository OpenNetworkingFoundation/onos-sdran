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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.xran.XranService;
import org.onosproject.xran.XranStore;
import org.onosproject.xran.asn1lib.api.CRNTI;
import org.onosproject.xran.impl.controller.XranManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Cell web resource.
 */
@Path("cell")
public class CellWebResource extends AbstractWebResource {

    private static final Logger log =
            LoggerFactory.getLogger(CellWebResource.class);

    private XranStore xranStore;
    private XranService xranService;

    public CellWebResource() {
        xranStore = get(XranStore.class);
        xranService = get(XranService.class);
    }

    /**
     * Lists the cell with {cellid}.
     *
     * @param eciHex EutranCellIdentifier in binary
     * @return Response
     */
    @GET
    @Path("{cellid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 404, message = "HTTP_NOT_FOUND")
    })
    public Response getCell(@PathParam("cellid") String eciHex) {
        return xranStore.getCell(eciHex).map(cell -> {
            try {
                JsonNode jsonNode = mapper().valueToTree(cell);

                return ResponseHelper.getResponse(
                        mapper(),
                        HttpURLConnection.HTTP_OK,
                        jsonNode
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
        }).orElse(ResponseHelper.getResponse(
                mapper(),
                HttpURLConnection.HTTP_NOT_FOUND,
                "Not Found",
                "Cell with " + eciHex + " was not found"
        ));
    }

    /**
     * Modify the RRMConfig parameters of the cell.
     *
     * @param eciHex EutranCellIdentifier in binary
     * @param stream Parameters that you want to modify
     * @return Response
     */
    @PATCH
    @Path("{cellid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 408, message = "HTTP_CLIENT_TIMEOUT"),
            @ApiResponse(code = 400, message = "HTTP_BAD_REQUEST"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 404, message = "HTTP_NOT_FOUND")

    })
    public Response patchCell(@PathParam("cellid") String eciHex, InputStream stream) {
        return xranStore.getCell(eciHex).map(cell -> {
            try {
                ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

                JsonNode rrmConf = jsonTree.path("RRMConf");
                if (!rrmConf.isMissingNode()) {
                    xranStore.modifyCellRrmConf(cell, rrmConf);

                    return xranService.sendModifiedRrm(cell.getRrmConfig())
                            .flatMap(queue -> {
                                try {
                                    return Optional.ofNullable(queue.poll(xranService
                                            .getNorthboundTimeout(), TimeUnit.MILLISECONDS));
                                } catch (InterruptedException e) {
                                    log.error(ExceptionUtils.getFullStackTrace(e));
                                    return Optional.empty();
                                }
                            }).map(p ->
                                    ResponseHelper.getResponse(
                                            mapper(),
                                            HttpURLConnection.HTTP_OK,
                                            "Handoff Response",
                                            p
                                    )
                            ).orElse(
                                    ResponseHelper.getResponse(
                                            mapper(),
                                            HttpURLConnection.HTTP_CLIENT_TIMEOUT,
                                            "Handoff Timeout",
                                            "eNodeB did not send a HOComplete/HOFailure on time"
                                    )
                            );
                }

                return ResponseHelper.getResponse(
                        mapper(),
                        HttpURLConnection.HTTP_BAD_REQUEST,
                        "Bad Request",
                        "The command you specified is not implemented or doesn't exist. We support " +
                                "RRMConf commands."
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
        }).orElse(ResponseHelper.getResponse(
                mapper(),
                HttpURLConnection.HTTP_NOT_FOUND,
                "Not Found",
                "Cell " + eciHex + " was not found"
        ));
    }

    @GET
    @Path("/crntis")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
    @ApiResponse(code = 200, message = "HTTP_OK"),
    @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
    @ApiResponse(code = 404, message = "HTTP_NOT_FOUND")
    })
    public Response getEcgiCRNTI(){
        //log.info("######### set of UEs =>" + xranStore.getSetofUEs().toString());
        //log.info("######### set of cells =>"+ xranStore.getSetofCells().toString());
        //JsonNode jsonNode = mapper().valueToTree(xranStore.getSetofUEs());
        JsonNode jsonNode = mapper().valueToTree(xranStore.getAllCRNTI());
        log.info(jsonNode.toString());
        return ResponseHelper.getResponse(mapper(),HttpURLConnection.HTTP_OK,jsonNode);

    }
    @GET
    @Path("/rrpscindex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 404, message = "HTTP_NOT_FOUND")
    })
    public Response getIndexofRRPSC(){
        //log.info("######### set of UEs =>" + xranStore.getSetofUEs().toString());
        //log.info("######### set of cells =>"+ xranStore.getSetofCells().toString());
        //JsonNode jsonNode = mapper().valueToTree(xranStore.getSetofUEs());
        JsonNode jsonNode = mapper().valueToTree(XranManager.getRRPSC());
        log.info(jsonNode.toString());
        return ResponseHelper.getResponse(mapper(),HttpURLConnection.HTTP_OK,jsonNode);

    }

}
