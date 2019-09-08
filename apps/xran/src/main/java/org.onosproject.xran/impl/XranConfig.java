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

package org.onosproject.xran.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.asn1lib.api.EUTRANCellIdentifier;
import org.onosproject.xran.asn1lib.api.PLMNIdentity;
import org.onosproject.xran.asn1lib.util.HexConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xran config.
 */
public class XranConfig extends Config<ApplicationId> {

    private static final String CELLS = "active_cells";

    private static final String PLMN_ID = "plmn_id";

    private static final String ECI_ID = "eci";

    private static final String IP_ADDR = "ip_addr";

    private static final String XRANC_IP = "xranc_bind_ip";

    private static final String XRANC_PORT = "xranc_port";

    private static final String XRANC_CELLCONFIG_INTERVAL = "xranc_cellconfigrequest_interval_seconds";

    private static final String RX_SIGNAL_MEAS_REPORT_INTERVAL = "rx_signal_meas_report_interval_ms";

    private static final String L2_MEAS_REPORT_INTERVAL = "l2_meas_report_interval_ms";

    private static final String ADMISSION_SUCCESS = "admission_success";

    private static final String BEARER_SUCCESS = "bearer_success";

    private static final String NO_MEAS_LINK_REMOVAL = "no_meas_link_removal_ms";

    private static final String IDLE_UE_REMOVAL = "idle_ue_removal_ms";

    private static final String NORTHBOUND_TIMEOUT = "nb_response_timeout_ms";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String GRID_X = "grid_x";


    private static final String GRID_Y = "grid_y";

    /**
     * Get a list of all CELLs inside the configuration file.
     *
     * @return Map of CELL IP to ECGI
     */
    public Map<IpAddress, ECGI> activeCellSet() {
        Map<IpAddress, ECGI> cells = new ConcurrentHashMap<>();

        JsonNode cellsNode = object.get(CELLS);
        if (cellsNode == null) {
            log.warn("no cells have been provided!");
            return cells;
        }

        cellsNode.forEach(cellNode -> {
            String plmnId = cellNode.get(PLMN_ID).asText();
            String eci = cellNode.get(ECI_ID).asText();

            String ipAddress = cellNode.get(IP_ADDR).asText();

            ECGI ecgi = hexToEcgi(plmnId, eci);
            cells.put(IpAddress.valueOf(ipAddress), ecgi);
        });

        return cells;
    }

    /**
     * Get flag for ADMISSION_SUCCESS field in configuration.
     *
     * @return boolean value in configuration
     */
    public boolean admissionFlag() {
        JsonNode flag = object.get(ADMISSION_SUCCESS);
        return flag != null && flag.asBoolean();
    }

    /**
     * Get flag for BEARER_SUCCESS field in configuration.
     *
     * @return boolean value in configuration
     */
    public boolean bearerFlag() {
        JsonNode flag = object.get(BEARER_SUCCESS);
        return flag != null && flag.asBoolean();
    }

    /**
     * Get IP where the XranServer binds to.
     *
     * @return IP address in configuration
     */
    public IpAddress getXrancIp() {
        return IpAddress.valueOf(object.get(XRANC_IP).asText());
    }

    /**
     * Get port for xRAN xranServer server to bind to from configuration.
     *
     * @return port number
     */
    public int getXrancPort() {
        return object.get(XRANC_PORT).asInt();
    }

    /**
     * Get config request interval from configuration.
     *
     * @return interval in seconds
     */
    public int getConfigRequestInterval() {
        return object.get(XRANC_CELLCONFIG_INTERVAL).asInt();
    }

    /**
     * Get rx signal interval from configuration.
     *
     * @return interval in milliseconds
     */
    public int getRxSignalInterval() {
        return object.get(RX_SIGNAL_MEAS_REPORT_INTERVAL).asInt();
    }

    /**
     * Get l2 measurement interval from configuration.
     *
     * @return interval in milliseconds
     */
    public int getL2MeasInterval() {
        return object.get(L2_MEAS_REPORT_INTERVAL).asInt();
    }

    /**
     * Get removal time of link after not getting measurement from configuration.
     *
     * @return interval in milliseconds
     */
    public int getNoMeasLinkRemoval() {
        return object.get(NO_MEAS_LINK_REMOVAL).asInt();
    }

    /**
     * Get removal time of UE after being IDLE from configuration.
     *
     * @return interval in milliseconds
     */
    public int getIdleUeRemoval() {
        return object.get(IDLE_UE_REMOVAL).asInt();
    }

    /**
     * Get northbound timeout when waiting for responses from configuration.
     *
     * @return interval in milliseconds
     */
    public int getNorthBoundTimeout() {
        return object.get(NORTHBOUND_TIMEOUT).asInt();
    }

    /**
     * Get ECGI from HEX representation of PLMN_ID and ECI.
     *
     * @param plmnId HEX string of PLMN_ID
     * @param eci    HEX string of ECI
     * @return new ECGI object
     */
    private ECGI hexToEcgi(String plmnId, String eci) {
        byte[] bytes = HexConverter.fromShortHexString(plmnId);
        byte[] bytearray = DatatypeConverter.parseHexBinary(eci);

        InputStream inputStream = new ByteArrayInputStream(bytearray);

        PLMNIdentity plmnIdentity = new PLMNIdentity(bytes);
        EUTRANCellIdentifier eutranCellIdentifier = new EUTRANCellIdentifier(bytearray, 28);

        ECGI ecgi = new ECGI();
        ecgi.setEUTRANcellIdentifier(eutranCellIdentifier);
        ecgi.setPLMNIdentity(plmnIdentity);
        try {
            ecgi.decode(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ecgi;
    }

    public Map<ECGI, String> activeCellxySet() {
        Map<IpAddress, ECGI> map; //;= this.activeCellSet();
        Map<ECGI, String> xyMap = new ConcurrentHashMap<>();

        JsonNode cellsNode = object.get(CELLS);
        if (cellsNode == null) {
            log.warn("no cells have been provided!");
            return xyMap;
        }

        cellsNode.forEach(cellNode -> {
            String longitude = cellNode.get(GRID_X).asText();
            String latitude = cellNode.get(GRID_Y).asText();

            String plmnId = cellNode.get(PLMN_ID).asText();
            String eci = cellNode.get(ECI_ID).asText();
            ECGI ecgi = hexToEcgi(plmnId, eci);
            xyMap.put(ecgi,latitude+","+longitude);
        });

        return xyMap;

    }
}
