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
import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.net.DeviceId;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.asn1lib.api.PRBUsage;
import org.onosproject.xran.asn1lib.api.XICICPA;
import org.onosproject.xran.asn1lib.ber.BerByteArrayOutputStream;
import org.onosproject.xran.asn1lib.ber.types.BerBitString;
import org.onosproject.xran.asn1lib.ber.types.BerInteger;
import org.onosproject.xran.asn1lib.pdu.CellConfigReport;
import org.onosproject.xran.asn1lib.pdu.L2MeasConfig;
import org.onosproject.xran.asn1lib.pdu.RRCMeasConfig;
import org.onosproject.xran.asn1lib.pdu.RRMConfig;
import org.onosproject.xran.asn1lib.pdu.RadioMeasReportPerCell;
import org.onosproject.xran.asn1lib.pdu.SchedMeasReportPerCell;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * R-NIB Cell and its properties.
 */
@JsonPropertyOrder({
        "ECGI",
        "Configuration",
        "RRMConfiguration",
        "MeasConfig",
        "Measurements"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RnibCell {
    @JsonIgnore
    private static final String SCHEME = "xran";

    @JsonProperty("ECGI")
    private ECGI ecgi;

    @JsonProperty("Configuration")
    private Optional<CellConfigReport> conf = Optional.empty();

    @JsonProperty("RRMConfiguration")
    private RRMConfig rrmConfig = new RRMConfig();

    @JsonProperty("MeasConfig")
    private MeasConfig measConfig = new MeasConfig();

    @JsonProperty("Measurements")
    private Measurements measurements = new Measurements();

    @JsonIgnore
    private String version = "5";

    protected static Logger log = LoggerFactory.getLogger(RnibCell.class);

    /**
     * Encode ECGI and obtain its URI.
     *
     * @param ecgi ECGI
     * @return URI
     */
    public static URI uri(ECGI ecgi) {
        if (ecgi != null) {
            try {
                BerByteArrayOutputStream os = new BerByteArrayOutputStream(4096);
                ecgi.encode(os);
                String message = DatatypeConverter.printHexBinary(os.getArray());
                return new URI(SCHEME, message, null);
            } catch (URISyntaxException | IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Obtain ECGI from the device ID.
     *
     * @param deviceId ID of the device
     * @return ECGI
     * @throws IOException I0 Exception for ByteArrayInputStream
     */
    public static ECGI decodeDeviceId(DeviceId deviceId) throws IOException {
        String uri = deviceId.toString();
        log.info("\n######## In decodeDeviceId of RnibCell with uri = "+uri+" #########");
        String hexEcgi = uri.substring(uri.lastIndexOf("xran:") + 5);

        ECGI ecgi = new ECGI();
        byte[] bytearray = DatatypeConverter.parseHexBinary(hexEcgi);
        InputStream inputStream = new ByteArrayInputStream(bytearray);

        ecgi.decode(inputStream);
        return ecgi;
    }

    /**
     * Get version ID.
     *
     * @return version ID
     */
    public int getVersion() {
        return Integer.parseInt(version);
    }

    /**
     * Set version ID.
     *
     * @param version version ID
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get RRMConfig.
     *
     * @return RRMConfig
     */
    public RRMConfig getRrmConfig() {
        return rrmConfig;
    }

    /**
     * Set RRMConfig properties.
     *
     * @param rrmConfig RRMConfig
     */
    public void setRrmConfig(RRMConfig rrmConfig) {
        this.rrmConfig = rrmConfig;
    }

    /**
     * Get ECGI.
     *
     * @return ECGI
     */
    public ECGI getEcgi() {
        return ecgi;
    }

    /**
     * Set ECGI.
     *
     * @param ecgi ECGI
     */
    public void setEcgi(ECGI ecgi) {
        this.ecgi = ecgi;
    }

    /**
     * Get cell config report.
     *
     * @return Optional CellConfig Report
     */
    public Optional<CellConfigReport> getOptConf() {
        return conf;
    }

    /**
     * Get cell config report.
     *
     * @return CellConfig Report
     */
    public CellConfigReport getConf() {
        return conf.get();
    }

    /**
     * Set cell config report.
     *
     * @param conf Cell config report
     */
    public void setConf(CellConfigReport conf) {
        this.conf = Optional.ofNullable(conf);
    }

    public MeasConfig getMeasConfig() {
        return measConfig;
    }

    public void setMeasConfig(MeasConfig measConfig) {
        this.measConfig = measConfig;
    }

    public Measurements getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Measurements measurements) {
        this.measurements = measurements;
    }

    /**
     * Modify the RRM Config parameters of cell.
     *
     * @param rrmConfigNode RRMConfig parameters to modify obtained from REST call
     * @param ueList        List of all UEs
     * @throws Exception p_a size not equal to UE size
     */
    public void modifyRrmConfig(JsonNode rrmConfigNode, List<RnibUe> ueList) throws Exception {
        RRMConfig.Crnti crnti = new RRMConfig.Crnti();
        ueList.forEach(ue -> crnti.getCRNTI().add(ue.getCrnti()));

        JsonNode pA = rrmConfigNode.path("p_a");
        if (!pA.isMissingNode()) {
            RRMConfig.Pa pa = new RRMConfig.Pa();
            if (pA.isArray()) {
                if (ueList.size() == pA.size()) {
                    List<XICICPA> collect = Stream.of(pA)
                            .map(val -> new XICICPA(val.asInt()))
                            .collect(Collectors.toList());
                    pa.getXICICPA().clear();
                    pa.getXICICPA().addAll(collect);
                } else {
                    throw new Exception("p_a size is not the same as UE size");
                }
            }
            rrmConfig.setPa(pa);
        }

        JsonNode startPrbDl1 = rrmConfigNode.path("start_prb_dl");
        if (!startPrbDl1.isMissingNode()) {
            RRMConfig.StartPrbDl startPrbDl = new RRMConfig.StartPrbDl();
            if (startPrbDl1.isArray()) {
                if (ueList.size() == startPrbDl1.size()) {
                    List<BerInteger> collect = Stream.of(startPrbDl1)
                            .map(val -> new BerInteger(val.asInt()))
                            .collect(Collectors.toList());
                    startPrbDl.getBerInteger().clear();
                    startPrbDl.getBerInteger().addAll(collect);
                } else {
                    throw new Exception("start_prb_dl size is not the same as UE size");
                }
            }
            rrmConfig.setStartPrbDl(startPrbDl);
        }

        JsonNode endPrbDl1 = rrmConfigNode.path("end_prb_dl");
        if (!endPrbDl1.isMissingNode()) {
            RRMConfig.EndPrbDl endPrbDl = new RRMConfig.EndPrbDl();
            if (endPrbDl1.isArray()) {
                if (ueList.size() == endPrbDl1.size()) {
                    List<BerInteger> collect = Stream.of(endPrbDl1)
                            .map(val -> new BerInteger(val.asInt()))
                            .collect(Collectors.toList());
                    endPrbDl.getBerInteger().clear();
                    endPrbDl.getBerInteger().addAll(collect);
                } else {
                    throw new Exception("end_prb_dl size is not the same as UE size");
                }
            }
            rrmConfig.setEndPrbDl(endPrbDl);
        }

        JsonNode frameBitmaskDl = rrmConfigNode.path("sub_frame_bitmask_dl");
        if (!frameBitmaskDl.isMissingNode()) {
            RRMConfig.SubframeBitmaskDl subframeBitmaskDl = new RRMConfig.SubframeBitmaskDl();
            if (frameBitmaskDl.isArray()) {
                List<BerBitString> collect = Stream.of(frameBitmaskDl)
                        .map(val -> new BerBitString(DatatypeConverter.parseHexBinary(val.asText()), 10))
                        .collect(Collectors.toList());
                subframeBitmaskDl.getBerBitString().clear();
                subframeBitmaskDl.getBerBitString().addAll(collect);
            } else {
                throw new Exception("sub_frame_bitmask_dl size is not the same as UE size");
            }
            rrmConfig.setSubframeBitmaskDl(subframeBitmaskDl);
        }

        JsonNode startPrbUl1 = rrmConfigNode.path("start_prb_ul");
        if (!startPrbUl1.isMissingNode()) {
            RRMConfig.StartPrbUl startPrbUl = new RRMConfig.StartPrbUl();
            if (startPrbUl1.isArray()) {
                if (ueList.size() == startPrbUl1.size()) {
                    List<BerInteger> collect = Stream.of(startPrbUl1)
                            .map(val -> new BerInteger(val.asInt()))
                            .collect(Collectors.toList());
                    startPrbUl.getBerInteger().clear();
                    startPrbUl.getBerInteger().addAll(collect);
                } else {
                    throw new Exception("start_prb_ul size is not the same as UE size");
                }
            }
            rrmConfig.setStartPrbUl(startPrbUl);
        }

        JsonNode endPrbUl1 = rrmConfigNode.path("end_prb_ul");
        if (!endPrbUl1.isMissingNode()) {
            RRMConfig.EndPrbUl endPrbUl = new RRMConfig.EndPrbUl();
            if (endPrbUl1.isArray()) {
                if (ueList.size() == endPrbUl1.size()) {
                    List<BerInteger> collect = Stream.of(endPrbUl1)
                            .map(val -> new BerInteger(val.asInt()))
                            .collect(Collectors.toList());
                    endPrbUl.getBerInteger().clear();
                    endPrbUl.getBerInteger().addAll(collect);
                } else {
                    throw new Exception("end_prb_ul size is not the same as UE size");
                }
            }
            rrmConfig.setEndPrbUl(endPrbUl);
        }

        JsonNode uePusch = rrmConfigNode.path("p0_ue_pusch");
        if (!uePusch.isMissingNode()) {
            RRMConfig.P0UePusch p0UePusch = new RRMConfig.P0UePusch();
            if (uePusch.isArray()) {
                if (ueList.size() == uePusch.size()) {
                    List<BerInteger> collect = Stream.of(uePusch)
                            .map(val -> new BerInteger(val.asInt()))
                            .collect(Collectors.toList());
                    p0UePusch.getBerInteger().clear();
                    p0UePusch.getBerInteger().addAll(collect);
                } else {
                    throw new Exception("p0_ue_pusch size is not the same as UE size");
                }
            }
            rrmConfig.setP0UePusch(p0UePusch);
        }

        JsonNode frameBitmaskUl = rrmConfigNode.path("sub_frame_bitmask_ul");
        if (!frameBitmaskUl.isMissingNode()) {
            RRMConfig.SubframeBitmaskUl subframeBitmaskUl = new RRMConfig.SubframeBitmaskUl();
            if (frameBitmaskUl.isArray()) {
                List<BerBitString> collect = Stream.of(frameBitmaskUl)
                        .map(val -> new BerBitString(DatatypeConverter.parseHexBinary(val.asText()), 10))
                        .collect(Collectors.toList());
                subframeBitmaskUl.getBerBitString().clear();
                subframeBitmaskUl.getBerBitString().addAll(collect);
            } else {
                throw new Exception("sub_frame_bitmask_ul size is not the same as UE size");
            }
            rrmConfig.setSubframeBitmaskUl(subframeBitmaskUl);
        }

        rrmConfig.setCrnti(crnti);
    }

    @Override
    public String toString(){
        return "{ ECGI =" + this.ecgi +"}";/*+
                " RRMConfiguration = " + this.rrmConfig + " MeasConfig = " + this.measConfig
                + " Measurements  = "+ this.measurements;*/
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RnibCell rnibCell = (RnibCell) o;

        return ecgi.equals(rnibCell.ecgi);
    }

    @Override
    public int hashCode() {
        return ecgi.hashCode();
    }

    /**
     * Object class for MeasConfig.
     */
    @JsonPropertyOrder({
            "RRCMeasConfig",
            "L2MeasConfig",
            "timestamp"
    })
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MeasConfig {
        @JsonProperty("timestamp")
        WallClockTimestamp timestamp = new WallClockTimestamp();
        @JsonProperty("RRCMeasConfig")
        private RRCMeasConfig rrcMeasConfig;
        @JsonProperty("L2MeasConfig")
        private L2MeasConfig l2MeasConfig;

        public MeasConfig() {
        }

        @JsonCreator
        public MeasConfig(@JsonProperty("RRCMeasConfig") RRCMeasConfig rrcMeasConfig,
                          @JsonProperty("L2MeasConfig") L2MeasConfig l2MeasConfig) {
            this.rrcMeasConfig = rrcMeasConfig;
            this.l2MeasConfig = l2MeasConfig;
        }

        public RRCMeasConfig getRrcMeasConfig() {
            return rrcMeasConfig;
        }

        public void setRrcMeasConfig(RRCMeasConfig rrcMeasConfig) {
            this.rrcMeasConfig = rrcMeasConfig;
        }

        public L2MeasConfig getL2MeasConfig() {
            return l2MeasConfig;
        }

        public void setL2MeasConfig(L2MeasConfig l2MeasConfig) {
            this.l2MeasConfig = l2MeasConfig;
        }

        public long getTimestamp() {
            return new WallClockTimestamp().unixTimestamp() - timestamp.unixTimestamp();
        }

        public void setTimestamp(WallClockTimestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "MeasConfig{" +
                    "rrcMeasConfig=" + rrcMeasConfig +
                    ", l2MeasConfig=" + l2MeasConfig +
                    ", timestamp=" + getTimestamp() +
                    '}';
        }
    }

    /**
     * Object class for PrbUsage.
     */
    @JsonPropertyOrder({
            "UL-InterferenceMeasurement",
            "PRB-Usage",
    })
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Measurements {
        @JsonProperty("UL-InterferenceMeasurement")
        ULInterferenceMeasurement ulInterferenceMeasurement = new ULInterferenceMeasurement();
        @JsonProperty("PRB-Usage")
        PrbUsage prbUsage = new PrbUsage();

        public Measurements() {
        }

        @JsonCreator
        public Measurements(
                @JsonProperty("UL-InterferenceMeasurement") ULInterferenceMeasurement ulInterferenceMeasurement,
                @JsonProperty("PRB-Usage") PrbUsage prbUsage
        ) {
            this.ulInterferenceMeasurement = ulInterferenceMeasurement;
            this.prbUsage = prbUsage;
        }

        public ULInterferenceMeasurement getUlInterferenceMeasurement() {
            return ulInterferenceMeasurement;
        }

        public void setUlInterferenceMeasurement(ULInterferenceMeasurement ulInterferenceMeasurement) {
            this.ulInterferenceMeasurement = ulInterferenceMeasurement;
        }

        public PrbUsage getPrbUsage() {
            return prbUsage;
        }

        public void setPrbUsage(PrbUsage prbUsage) {
            this.prbUsage = prbUsage;
        }

        @Override
        public String toString() {
            return "Measurements{" +
                    "ulInterferenceMeasurement=" + ulInterferenceMeasurement +
                    ", prbUsage=" + prbUsage +
                    '}';
        }

        /**
         * Object class for PrbUsage.
         */
        @JsonPropertyOrder({
                "PUSCH",
                "PUCCH",
                "timestamp"
        })
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ULInterferenceMeasurement {
            @JsonProperty("PUSCH")
            RadioMeasReportPerCell.PuschIntfPowerHist pusch;
            @JsonProperty("PUCCH")
            RadioMeasReportPerCell.PucchIntfPowerHist pucch;
            @JsonProperty("timestamp")
            WallClockTimestamp timestamp = new WallClockTimestamp();

            public ULInterferenceMeasurement() {
            }

            @JsonCreator
            public ULInterferenceMeasurement(@JsonProperty("PUSCH") RadioMeasReportPerCell.PuschIntfPowerHist pusch,
                                             @JsonProperty("PUCCH") RadioMeasReportPerCell.PucchIntfPowerHist pucch) {
                this.pusch = pusch;
                this.pucch = pucch;
            }

            public RadioMeasReportPerCell.PuschIntfPowerHist getPusch() {
                return pusch;
            }

            public void setPusch(RadioMeasReportPerCell.PuschIntfPowerHist pusch) {
                this.pusch = pusch;
            }

            public RadioMeasReportPerCell.PucchIntfPowerHist getPucch() {
                return pucch;
            }

            public void setPucch(RadioMeasReportPerCell.PucchIntfPowerHist pucch) {
                this.pucch = pucch;
            }

            public long getTimestamp() {
                return new WallClockTimestamp().unixTimestamp() - timestamp.unixTimestamp();
            }

            public void setTimestamp(WallClockTimestamp timestamp) {
                this.timestamp = timestamp;
            }

            @Override
            public String toString() {
                return "ULInterferenceMeasurement{" +
                        "pusch=" + pusch +
                        ", pucch=" + pucch +
                        ", timestamp=" + getTimestamp() +
                        '}';
            }
        }

        /**
         * Object class for PrbUsage.
         */
        @JsonPropertyOrder({
                "QCI",
                "primary",
                "secondary",
                "timestamp"
        })
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PrbUsage {
            @JsonProperty("QCI")
            SchedMeasReportPerCell.QciVals qci;
            @JsonProperty("primary")
            PRBUsage primary;
            @JsonProperty("secondary")
            PRBUsage secondary;
            @JsonProperty("timestamp")
            WallClockTimestamp timestamp = new WallClockTimestamp();

            public PrbUsage() {
            }

            @JsonCreator
            public PrbUsage(
                    @JsonProperty("QCI") SchedMeasReportPerCell.QciVals qci,
                    @JsonProperty("primary") PRBUsage primary,
                    @JsonProperty("secondary") PRBUsage secondary
            ) {
                this.qci = qci;
                this.primary = primary;
                this.secondary = secondary;
            }

            /**
             * Get QCI
             *
             * @return QCI
             */
            public SchedMeasReportPerCell.QciVals getQci() {
                return qci;
            }

            /**
             * Set QCI.
             *
             * @param qci
             */
            public void setQci(SchedMeasReportPerCell.QciVals qci) {
                this.qci = qci;
            }

            /**
             * Get primary PrbUsage.
             *
             * @return PrbUsage
             */
            public PRBUsage getPrimary() {
                return primary;
            }

            /**
             * Set secondary PrbUsage.
             *
             * @param primary PrbUsage
             */
            public void setPrimary(PRBUsage primary) {
                this.primary = primary;
            }

            /**
             * Get secondary PrbUsage.
             *
             * @return PrbUsage
             */
            public PRBUsage getSecondary() {
                return secondary;
            }

            /**
             * Set secondary PrbUsage.
             *
             * @param secondary PrbUsage
             */
            public void setSecondary(PRBUsage secondary) {
                this.secondary = secondary;
            }

            /**
             * Get time since last update.
             *
             * @return long Time
             */
            public long getTimestamp() {
                return new WallClockTimestamp().unixTimestamp() - timestamp.unixTimestamp();
            }


            /**
             * Set time since last update.
             *
             * @param timestamp time since last update
             */
            public void setTimestamp(WallClockTimestamp timestamp) {
                this.timestamp = timestamp;
            }

            @Override
            public String toString() {
                return "PrbUsage{" +
                        "qci=" + qci +
                        ", primary=" + primary +
                        ", secondary=" + secondary +
                        ", timestamp=" + getTimestamp() +
                        '}';
            }
        }
    }
}
