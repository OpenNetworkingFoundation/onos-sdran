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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.xran.XranService;
import org.onosproject.xran.XranStore;
import org.onosproject.xran.asn1lib.ber.types.BerInteger;
import org.onosproject.xran.impl.entities.RnibCell;
import org.onosproject.xran.impl.entities.RnibLink;
import org.onosproject.xran.impl.entities.RnibUe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Link web resource.
 */
@Path("links")
public class LinkWebResource extends AbstractWebResource {

    private static final Logger log =
            LoggerFactory.getLogger(LinkWebResource.class);

    private XranStore xranStore;
    private XranService xranService;

    public LinkWebResource() {
        xranStore = get(XranStore.class);
        xranService = get(XranService.class);
    }

    /**
     * List all the links originating or terminating at cell/UE OR list the link connecting between cell and UE.
     *
     * @param eciHex EutranCellIdentifier in binary
     * @param ue     UE ID
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 404, message = "HTTP_NOT_FOUND")
    })
    public Response getLinksBetween(@DefaultValue("") @QueryParam("cell") String eciHex,
                                    @DefaultValue("-1") @QueryParam("ue") long ue) {
        List<RnibLink> list = Lists.newArrayList();
        if (!eciHex.isEmpty() && ue != -1) {
            xranStore.getLink(eciHex, ue).ifPresent(list::add);
        } else if (!eciHex.isEmpty()) {
            list.addAll(xranStore.getLinks(eciHex));
        } else if (ue != -1) {
            list.addAll(xranStore.getLinks(ue));
        } else {
            list.addAll(xranStore.getLinks());
        }
        //log.info("\n" + list.toString() + "\n");
        if (list.size() > 0) {
            try {
                JsonNode jsonNode = mapper().valueToTree(list);

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
        }

        return ResponseHelper.getResponse(
                mapper(),
                HttpURLConnection.HTTP_NOT_FOUND,
                "Not Found",
                "Specified links not found"
        );
    }

    /**
     * Modify the link.
     *
     * @param src    CELL ECI in binary
     * @param dst    UE ID
     * @param stream Parameter on basis of which link is to be modified
     * @return Response
     */
    @PATCH
    @Path("{src},{dst}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 501, message = "HTTP_NOT_IMPLEMENTED"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 404, message = "HTTP_NOT_FOUND"),
            @ApiResponse(code = 400, message = "HTTP_BAD_REQUEST"),
            @ApiResponse(code = 408, message = "HTTP_CLIENT_TIMEOUT")
    })
    public Response patchLinks(@PathParam("src") String src, @PathParam("dst") long dst, InputStream stream) {
        //log.info(xranStore.getLink(src, dst).toString());
        return xranStore.getLink(src, dst).map(link -> {
            try {
                ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream).deepCopy();

                // Modify link based on Type
                JsonNode type = jsonTree.path("type");
                if (!type.isMissingNode()) {
                    RnibLink.Type anEnum = RnibLink.Type.getEnum(type.asText());
                    log.info("\n %%%%%%%% Calling handleTypeChange from patchLinks %%%% \n");
                    return handleTypeChange(link, anEnum);
                }

                // Modify link based on traffic percent
                JsonNode trafficpercent = jsonTree.path("trafficpercent");
                if (!trafficpercent.isMissingNode()) {
                    return handleTrafficChange(link, trafficpercent);
                }

                // Modify link based on RRMConf
                JsonNode rrmConf = jsonTree.path("RRMConf");
                if (!rrmConf.isMissingNode()) {
                    return handleRrmChange(link, rrmConf);
                }

                return ResponseHelper.getResponse(
                        mapper(),
                        HttpURLConnection.HTTP_NOT_IMPLEMENTED,
                        "Not Implemented",
                        "The command you specified is not implemented or doesn't exist. We support " +
                                "type/RRMConf/traficpercent commands."
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
                "Link not found use POST request"
        ));
    }

    /**
     * Create link based on Type of the link.
     *
     * @param src    CELL ECI in binary
     * @param dst    UE ID
     * @param stream LinkType
     * @return Response
     */
    @POST
    @Path("{src},{dst}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "HTTP_OK"),
            @ApiResponse(code = 501, message = "HTTP_NOT_IMPLEMENTED"),
            @ApiResponse(code = 500, message = "HTTP_INTERNAL_ERROR"),
            @ApiResponse(code = 404, message = "HTTP_NOT_FOUND"),
            @ApiResponse(code = 400, message = "HTTP_BAD_REQUEST"),
            @ApiResponse(code = 408, message = "HTTP_CLIENT_TIMEOUT"),
    })
    public Response postLinks(@PathParam("src") String src, @PathParam("dst") long dst, InputStream stream) {
        Optional<RnibCell> cellOptional = xranStore.getCell(src);
        Optional<RnibUe> ueOptional = xranStore.getUe(dst);

        if (!cellOptional.isPresent()) {
            return ResponseHelper.getResponse(
                    mapper(),
                    HttpURLConnection.HTTP_NOT_FOUND,
                    "Not Found",
                    "Cell " + src + " was not found"
            );
        }

        if (!ueOptional.isPresent()) {
            return ResponseHelper.getResponse(
                    mapper(),
                    HttpURLConnection.HTTP_NOT_FOUND,
                    "Not Found",
                    "Ue with " + dst + " was not found"
            );
        }
        //log.info("############In postLinks########### with cellOptional.get().getEcgi() = " + cellOptional.get().getEcgi() + " \nAND ueOptional.get().getId() = " + ueOptional.get().getId() + " ##########");
        //log.info("######### xranStore.getLink(cellOptional.get().getEcgi(), ueOptional.get().getId()) = " + xranStore.getLink(cellOptional.get().getEcgi(), ueOptional.get().getId()) + "  ##########");
        //if (xranStore.getLink(cellOptional.get().getEcgi(), ueOptional.get().getId()) != null) {
        if (xranStore.getLink(cellOptional.get().getEcgi(), ueOptional.get().getId()).isPresent()) {
            return ResponseHelper.getResponse(
                    mapper(),
                    HttpURLConnection.HTTP_BAD_REQUEST,
                    "Bad Request",
                    "Link already exists use PATCH to modify"
            );
        }

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            JsonNode type = jsonTree.path("type");

            RnibLink link = new RnibLink(cellOptional.get(), ueOptional.get());
            // store it as non-serving when creating link
            xranStore.storeLink(link);
            if (!type.isMissingNode()) {
                log.info("\n@@@@@@@Calling handleTypeChange with link = " + link.toString() + " and RnibLink.Type.getEnum(type.asText()) = " + RnibLink.Type.getEnum(type.asText()) + " @@ \n");
                return handleTypeChange(link, RnibLink.Type.getEnum(type.asText()));
            }

            JsonNode trafficpercent = jsonTree.path("trafficpercent");
            if (!trafficpercent.isMissingNode()) {
                return handleTrafficChange(link, trafficpercent);
            }

            JsonNode rrmConf = jsonTree.path("RRMConf");
            if (!rrmConf.isMissingNode()) {
                return handleRrmChange(link, rrmConf);
            }

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

        return ResponseHelper.getResponse(
                mapper(),
                HttpURLConnection.HTTP_BAD_REQUEST,
                "Bad Request",
                "The command you specified is not implemented " +
                        "or doesn't exist. We support " +
                        "type/RRMConf/traficpercent commands."
        );
    }


    /**
     * Change link based on type of the link.
     *
     * @param link    Link
     * @param newType LinkType
     * @return Response
     * @throws InterruptedException Interrupted queue
     */

    private Response handleTypeChange(RnibLink link, RnibLink.Type newType)
            throws InterruptedException {
        if (newType.equals(RnibLink.Type.SERVING_PRIMARY)) {
            switch (link.getType()) {
                case SERVING_PRIMARY: {
                    return ResponseHelper.getResponse(
                            mapper(),
                            HttpURLConnection.HTTP_BAD_REQUEST,
                            "Bad Request",
                            "Link is already a primary link"
                    );
                }
                case SERVING_SECONDARY_CA:
                case SERVING_SECONDARY_DC:
                case NON_SERVING: {
                    List<RnibLink> linksByUeId = xranStore
                            .getLinks(link.getLinkId().getUeId());
                    //linksByUeId: list of links associated with  UeId in the curl request
                    return linksByUeId.stream()
                            .filter(l -> l.getType().equals(RnibLink.Type.SERVING_PRIMARY)) //This gives the 1st P.Link
                            .findFirst().map(primaryLink -> xranService.sendHoRequest(link, primaryLink)
                                    .flatMap(q -> {
                                        try {
                                            return Optional.ofNullable(q.poll(xranService
                                                                                      .getNorthboundTimeout(), TimeUnit.MILLISECONDS));
                                        } catch (InterruptedException e) {
                                            log.error(ExceptionUtils.getFullStackTrace(e));
                                            return Optional.empty();
                                        }
                                    }).map(poll ->
                                                   ResponseHelper.getResponse(
                                                           mapper(),
                                                           HttpURLConnection.HTTP_OK,
                                                           "Handoff Response",
                                                           poll
                                                   )
                                    ).orElse(
                                            ResponseHelper.getResponse(
                                                    mapper(),
                                                    HttpURLConnection.HTTP_CLIENT_TIMEOUT,
                                                    "Handoff Timeout",
                                                    "eNodeB did not send a HOComplete/HOFailure on time"
                                            )
                                    )
                            )
                            .orElseGet(
                                    () -> {
                                        link.setType(RnibLink.Type.SERVING_PRIMARY);
                                        return ResponseHelper.getResponse(
                                                mapper(),
                                                HttpURLConnection.HTTP_OK,
                                                "OK",
                                                "Link set to primary"
                                        );
                                    }
                            );
                }
                default:
            }
        } else if (newType.equals(RnibLink.Type.NON_SERVING)) {
            switch (link.getType()) {
                case NON_SERVING: {
                    return ResponseHelper.getResponse(
                            mapper(),
                            HttpURLConnection.HTTP_BAD_REQUEST,
                            "Bad Request",
                            "Link is already a primary link"
                    );
                }
                case SERVING_PRIMARY: {
                    return ResponseHelper.getResponse(
                            mapper(),
                            HttpURLConnection.HTTP_BAD_REQUEST,
                            "Bad Request",
                            "Cannot modify a primary link"
                    );
                }
                case SERVING_SECONDARY_CA:
                case SERVING_SECONDARY_DC: {
                    if (xranService.sendScellDelete(link)) {
                        return ResponseHelper.getResponse(
                                mapper(),
                                HttpURLConnection.HTTP_OK,
                                "OK",
                                "Link set to non-serving"
                        );
                    } else {
                        return ResponseHelper.getResponse(
                                mapper(),
                                HttpURLConnection.HTTP_NOT_FOUND,
                                "Not Found",
                                "Could not find cell config report to construct Scell Delete"
                        );
                    }
                }
                default:
            }
        } else if (newType.equals(RnibLink.Type.SERVING_SECONDARY_CA)) {
            switch (link.getType()) {
                case SERVING_PRIMARY: {
                    return ResponseHelper.getResponse(
                            mapper(),
                            HttpURLConnection.HTTP_BAD_REQUEST,
                            "Bad Request",
                            "Cannot modify a primary link"
                    );
                }
                case SERVING_SECONDARY_DC:
                case NON_SERVING: {
                    return xranService.sendScellAdd(link).flatMap(queue -> {
                        try {
                            return Optional.ofNullable(queue.poll(xranService
                                                                          .getNorthboundTimeout(), TimeUnit.MILLISECONDS));
                        } catch (InterruptedException e) {
                            log.error(ExceptionUtils.getFullStackTrace(e));
                            return Optional.empty();
                        }
                    }).map(poll ->
                                   ResponseHelper.getResponse(
                                           mapper(),
                                           HttpURLConnection.HTTP_OK,
                                           "ScellAdd Response",
                                           poll
                                   )
                    ).orElse(
                            ResponseHelper.getResponse(
                                    mapper(),
                                    HttpURLConnection.HTTP_CLIENT_TIMEOUT,
                                    "ScellAdd Timeout",
                                    "eNodeB did not send a ScellAddStatus on time"
                            )
                    );
                }
                case SERVING_SECONDARY_CA: {
                    return ResponseHelper.getResponse(
                            mapper(),
                            HttpURLConnection.HTTP_BAD_REQUEST,
                            "Bad Request",
                            "Link is already a secondary CA link"
                    );
                }
                default:
            }
        }

        return ResponseHelper.getResponse(
                mapper(),
                HttpURLConnection.HTTP_BAD_REQUEST,
                "Bad Request",
                "The command you specified is not implemented or doesn't exist."
        );
    }

    /**
     * Modify link based on the traffic percent.
     *
     * @param link           Link
     * @param trafficpercent Traffic Percent of the link to be modified
     * @return Response
     */
    private Response handleTrafficChange(RnibLink link, JsonNode trafficpercent) {
        JsonNode jsonNode = trafficpercent.path("traffic-percent-dl");
        if (!jsonNode.isMissingNode()) {
            link.getTrafficPercent().setTrafficPercentDl(new BerInteger(jsonNode.asInt()));
        }

        jsonNode = trafficpercent.path("traffic-percent-ul");
        if (!jsonNode.isMissingNode()) {
            link.getTrafficPercent().setTrafficPercentUl(new BerInteger(jsonNode.asInt()));
        }

        return ResponseHelper.getResponse(
                mapper(),
                HttpURLConnection.HTTP_OK,
                "OK",
                "Traffic Percent changed"
        );
    }

    /**
     * Modify link based on RRMConf parameters.
     *
     * @param link    Link
     * @param rrmConf RRMConfig of the Link to be modified
     * @return Response
     * @throws InterruptedException Interrupted queue
     */
    private Response handleRrmChange(RnibLink link, JsonNode rrmConf) throws InterruptedException {
        log.info("\n!!!!!!!! in handleRrmChange with link = "+link+" and rrmConf = "+rrmConf.toString());
        xranStore.modifyLinkRrmConf(link, rrmConf);

        return xranService.sendModifiedRrm(link.getRrmParameters()).flatMap(queue -> {
            try {
                return Optional.ofNullable(queue.poll(xranService.getNorthboundTimeout(), TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                log.error(ExceptionUtils.getFullStackTrace(e));
                return Optional.empty();
            }
        }).map(poll ->
                       ResponseHelper.getResponse(
                               mapper(),
                               HttpURLConnection.HTTP_OK,
                               "RRMConfig Response",
                               poll
                       )
        ).orElse(
                ResponseHelper.getResponse(
                        mapper(),
                        HttpURLConnection.HTTP_CLIENT_TIMEOUT,
                        "RRMConfig Timeout",
                        "eNodeB did not send a RRMConfingStatus on time"
                )
        );

    }
}