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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.core.Response;

/**
 * Various types of responses.
 */
public final class ResponseHelper {

    private ResponseHelper() {
    }

    public static Response getResponse(ObjectMapper mapper, int status, String title, String detail) {
        ObjectNode rootNode = mapper.createObjectNode();

        switch (status) {
            case 200: {
                ArrayNode data = rootNode.putArray("data");
                ObjectNode addObject = data.addObject();
                addObject.put("status", status);
                addObject.put("title", title);
                addObject.put("detail", detail);
                return Response.status(status)
                        .entity(rootNode.toString())
                        .build();
            }
            case 400:
            case 501:
            case 408:
            case 500:
            case 404: {
                ArrayNode errors = rootNode.putArray("errors");
                ObjectNode addObject = errors.addObject();
                addObject.put("status", status);
                addObject.put("title", title);
                addObject.put("detail", detail);
                return Response.status(status)
                        .entity(rootNode.toString())
                        .build();
            }
            default:
                return Response.noContent().build();
        }
    }

    public static Response getResponse(ObjectMapper mapper, int status, JsonNode node) {
        ObjectNode rootNode = mapper.createObjectNode();

        switch (status) {
            case 200:
            case 400:
            case 501:
            case 408:
            case 500:
            case 404: {
                ArrayNode data = rootNode.putArray("data");
                data.add(node);
                return Response.status(status)
                        .entity(rootNode.toString())
                        .build();
            }
            default:
                return Response.noContent().build();
        }
    }
}
