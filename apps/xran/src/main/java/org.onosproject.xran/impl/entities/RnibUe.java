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

package org.onosproject.xran.impl.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.onlab.packet.MacAddress;
import org.onosproject.net.HostId;
import org.onosproject.xran.asn1lib.api.CRNTI;
import org.onosproject.xran.asn1lib.api.ENBUES1APID;
import org.onosproject.xran.asn1lib.api.MMEUES1APID;
import org.onosproject.xran.asn1lib.pdu.RRCMeasConfig;
import org.onosproject.xran.asn1lib.pdu.UECapabilityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.net.HostId.hostId;

/**
 * R-NIB UE and its properties.
 */
@JsonPropertyOrder({
        "Identifier",
        "Context-IDs",
        "RAN-ID",
        "State",
        "Capability",
        "MeasurementConfiguration"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RnibUe {
    @JsonIgnore
    private static final Logger log =
            LoggerFactory.getLogger(RnibUe.class);

    @JsonProperty("Identifier")
    private Long id;
    @JsonProperty("Context-IDs")
    private ContextIds contextIds = new ContextIds();
    @JsonProperty("RAN-ID")
    private CRNTI crnti;
    @JsonProperty("State")
    private State state = State.ACTIVE;
    @JsonProperty("Capability")
    private UECapabilityInfo capability;
    @JsonProperty("MeasurementConfiguration")
    private RRCMeasConfig measConfig;
    @JsonIgnore
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Convert Host ID to UE ID.
     *
     * @param hostId hostID
     * @return Long UE ID
     */
    public static Long hostIdtoUEId(HostId hostId) {
        String mac = hostId.mac().toString();
        mac = mac.replace(":", "");
        return Long.parseLong(mac, 16);
    }

    /**
     * Get timer.
     *
     * @return Timer
     */
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    /**
     * Set executor.
     *
     * @param executor executor
     */
    public void setExecutor(ScheduledExecutorService executor) {
        this.executor.shutdown();
        this.executor = executor;
    }

    /**
     * Get CRNTI.
     *
     * @return CRNTI
     */
    public CRNTI getCrnti() {
        return crnti;
    }

    /**
     * Set CRNTI.
     *
     * @param crnti CRNTI
     */
    public void setCrnti(CRNTI crnti) {
        this.crnti = crnti;
    }

    /**
     * Get Host ID.
     *
     * @return HostId
     */
    @JsonIgnore
    public HostId getHostId() {
        try {
            log.info("\n131.######### RnibUE.getHostId() = "+this.id+" ############\n");
            String text = Long.toHexString(this.id),
                    res = "";
            int charsLeft = 12 - text.length();
            if (charsLeft > 0) {
                res += Stream.generate(() -> "0").limit(charsLeft).collect(Collectors.joining(""));
            } else if (charsLeft < 0) {
                return null;
            }
            res += text;

            String insert = ":";
            int period = 2;

            StringBuilder builder = new StringBuilder(
                    res.length() + insert.length() * (res.length() / period) + 1);

            int index = 0;
            String prefix = "";
            while (index < res.length()) {
                // Don't putPrimaryLink the insert in the very first iteration.
                // This is easier than appending it *after* each substring
                builder.append(prefix);
                prefix = insert;
                builder.append(res.substring(index,
                        Math.min(index + period, res.length())));
                index += period;
            }
            log.info("\n159.######### RnibUE.builder = "+builder.toString()+" ############\n");
            return hostId(MacAddress.valueOf(builder.toString()));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Get RXMeasConfig Report.
     *
     * @return RXSigMeasConfig
     */
    public RRCMeasConfig getMeasConfig() {
        return measConfig;
    }

    /**
     * Set RXMeasConfig Report.
     *
     * @param measConfig RXSigMeasConfig
     */
    public void setMeasConfig(RRCMeasConfig measConfig) {
        this.measConfig = measConfig;
    }

    /**
     * Get UE Capability Info.
     *
     * @return UECapabilityInfo
     */
    public UECapabilityInfo getCapability() {
        return capability;
    }

    /**
     * Set UE Capability Info.
     *
     * @param capability UECapabilityInfo
     */
    public void setCapability(UECapabilityInfo capability) {
        this.capability = capability;
    }

    /**
     * Get State.
     *
     * @return State
     */
    public State getState() {
        return state;
    }

    /**
     * Set State.
     *
     * @param state State
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Get UE ID.
     *
     * @return Long UE ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Set UE ID.
     *
     * @param id Long UE ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    public ContextIds getContextIds() {
        return contextIds;
    }

    public void setContextIds(ContextIds contextIds) {
        this.contextIds = contextIds;
    }

    @Override
    public String toString() {
        return "RnibUe{" +
                "id=" + id +
                ", contextIds=" + contextIds +
                ", crnti=" + crnti +
                ", state=" + state +
                ", capability=" + capability +
                ", measConfig=" + measConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RnibUe rnibUe = (RnibUe) o;
        return Objects.equals(id, rnibUe.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    /**
     * Enum of State of UE.
     */
    public enum State {
        ACTIVE {
            @Override
            public String toString() {
                return "ACTIVE";
            }
        },
        IDLE {
            @Override
            public String toString() {
                return "IDLE";
            }
        }
    }

    /**
     * Context IDs.
     */
    @JsonPropertyOrder({
            "IMSI",
            "ENBUES1APID",
            "MMEUES1APID",
    })
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContextIds {
        @JsonProperty("IMSI")
        private String imsi = "";
        @JsonProperty("ENBUES1APID")
        private ENBUES1APID enbS1apId;
        @JsonProperty("MMEUES1APID")
        private MMEUES1APID mmeS1apId;

        public ContextIds() {
        }

        @JsonCreator
        public ContextIds(
                @JsonProperty("ENBUES1APID") ENBUES1APID enbS1apId,
                @JsonProperty("MMEUES1APID") MMEUES1APID mmeS1apId
        ) {
            this.enbS1apId = enbS1apId;
            this.mmeS1apId = mmeS1apId;
        }

        public ContextIds(String imsi, ENBUES1APID enbS1apId, MMEUES1APID mmeS1apId) {
            this.imsi = imsi;
            this.enbS1apId = enbS1apId;
            this.mmeS1apId = mmeS1apId;
        }

        /**
         * Get MMEUES1APID.
         *
         * @return MMEUES1APID
         */
        public MMEUES1APID getMmeS1apId() {
            return mmeS1apId;
        }

        /**
         * Set MMEUES1APID.
         *
         * @param mmeS1apId MMEUES1APID
         */
        public void setMmeS1apId(MMEUES1APID mmeS1apId) {
            this.mmeS1apId = mmeS1apId;
        }

        /**
         * Get ENBUES1APID.
         *
         * @return ENBUES1APID
         */
        public ENBUES1APID getEnbS1apId() {
            return enbS1apId;
        }

        /**
         * Set ENBUES1APID.
         *
         * @param enbS1apId ENBUES1APID
         */
        public void setEnbS1apId(ENBUES1APID enbS1apId) {
            this.enbS1apId = enbS1apId;
        }

        /**
         * Get IMSI.
         *
         * @return IMSI
         */
        public String getImsi() {
            return imsi;
        }

        /**
         * Set IMSI.
         *
         * @param imsi IMSI
         */
        public void setImsi(String imsi) {
            this.imsi = imsi;
        }

        @Override
        public String toString() {
            return "ContextIds{" +
                    "imsi='" + imsi + '\'' +
                    ", enbS1apId=" + enbS1apId +
                    ", mmeS1apId=" + mmeS1apId +
                    '}';
        }
    }
}
