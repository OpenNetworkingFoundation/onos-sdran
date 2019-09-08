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
import com.google.common.collect.Lists;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.xran.asn1lib.api.ERABParams;
import org.onosproject.xran.asn1lib.api.PRBUsage;
import org.onosproject.xran.asn1lib.api.RadioRepPerServCell;
import org.onosproject.xran.asn1lib.api.SchedMeasRepPerServCell;
import org.onosproject.xran.asn1lib.api.TrafficSplitPercentage;
import org.onosproject.xran.asn1lib.api.XICICPA;
import org.onosproject.xran.asn1lib.ber.types.BerBitString;
import org.onosproject.xran.asn1lib.ber.types.BerInteger;
import org.onosproject.xran.asn1lib.pdu.PDCPMeasReportPerUe;
import org.onosproject.xran.asn1lib.pdu.RRMConfig;
import org.onosproject.xran.asn1lib.pdu.RXSigMeasReport;
import org.onosproject.xran.impl.identifiers.LinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * R-NIB Link and its properties.
 */
@JsonPropertyOrder({
        "Link-ID",
        "Type",
        "RRMConfiguration",
        "TrafficPercent",
        "BearerParameters",
        "Measurements"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RnibLink {
    @JsonIgnore
    private static final Logger log =
            LoggerFactory.getLogger(RnibLink.class);
    @JsonProperty("Measurements")
    private Measurements measurements = new Measurements();
    @JsonProperty("Link-ID")
    private LinkId linkId;
    @JsonProperty("RRMConfiguration")
    private RRMConfig rrmParameters;
    @JsonProperty("TrafficPercent")
    private TrafficSplitPercentage trafficPercent;
    @JsonProperty("BearerParameters")
    private ERABParams bearerParameters;
    @JsonProperty("Type")
    private Type type;
    @JsonIgnore
    private ScheduledExecutorService executor;

    public RnibLink(RnibCell cell, RnibUe ue) {
        trafficPercent = new TrafficSplitPercentage();
        trafficPercent.setEcgi(cell.getEcgi());
        trafficPercent.setTrafficPercentDl(new BerInteger(100));
        trafficPercent.setTrafficPercentUl(new BerInteger(100));

        executor = Executors.newSingleThreadScheduledExecutor();

        type = Type.NON_SERVING;

        linkId = LinkId.valueOf(cell, ue);

        rrmParameters = new RRMConfig();
        RRMConfig.Crnti crnti = new RRMConfig.Crnti();
        crnti.getCRNTI().add(linkId.getUe().getCrnti());
        rrmParameters.setCrnti(crnti);
        rrmParameters.setEcgi(linkId.getEcgi());
    }

    /**
     * Get executor.
     *
     * @return Timer
     */
    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ScheduledExecutorService executor) {
        this.executor.shutdown();
        this.executor = executor;
    }

    /**
     * Get Link ID.
     *
     * @return LinkID
     */
    @JsonProperty("Link-ID")
    public LinkId getLinkId() {
        return linkId;
    }

    /**
     * Set the Link ID.
     *
     * @param linkId Link ID
     */
    @JsonProperty("Link-ID")
    public void setLinkId(LinkId linkId) {
        this.linkId = linkId;
    }

    /**
     * Set the LINK ID with cell and ue.
     *
     * @param cell Rnib CELL
     * @param ue   Rnib UE
     */
    public void setLinkId(RnibCell cell, RnibUe ue) {
        this.linkId = LinkId.valueOf(cell, ue);
        trafficPercent.setEcgi(cell.getEcgi());
    }

    /**
     * Get the link type.
     *
     * @return Link-type
     */
    @JsonProperty("Type")
    public Type getType() {
        return type;
    }

    /**
     * Set the link type.
     *
     * @param type Link-type
     */
    @JsonProperty("Type")
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Get traffic percent.
     *
     * @return TrafficSplitPercentage
     */
    @JsonProperty("TrafficPercent")
    public TrafficSplitPercentage getTrafficPercent() {
        return trafficPercent;
    }

    /**
     * Set traffic percent.
     *
     * @param trafficPercent TrafficSplitPercentage
     */
    @JsonProperty("TrafficPercent")
    public void setTrafficPercent(TrafficSplitPercentage trafficPercent) {
        this.trafficPercent = trafficPercent;
    }

    /**
     * Get the Bearer Parameters.
     *
     * @return ERABParams
     */
    @JsonProperty("BearerParameters")
    public ERABParams getBearerParameters() {
        return bearerParameters;
    }

    /**
     * Set the Bearer Parameters.
     *
     * @param bearerParameters ERABParams
     */
    @JsonProperty("BearerParameters")
    public void setBearerParameters(ERABParams bearerParameters) {
        this.bearerParameters = bearerParameters;
    }

    /**
     * Get RRM Configuration.
     *
     * @return RRMConfig
     */
    @JsonProperty("RRMConfiguration")
    public RRMConfig getRrmParameters() {
        return rrmParameters;
    }

    /**
     * Set RRM Configuration.
     *
     * @param rrmParameters RRMConfig
     */
    @JsonProperty("RRMConfiguration")
    public void setRrmParameters(RRMConfig rrmParameters) {
        this.rrmParameters = rrmParameters;
    }

    public Measurements getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Measurements measurements) {
        this.measurements = measurements;
    }

    /**
     * Modify the RRM Config parameters of link.
     *
     * @param rrmConfigNode RRMConfig parameters to modify obtained from REST call
     */
    public void modifyRrmParameters(JsonNode rrmConfigNode) {
        log.info("\n!!!!!!!! in modifyRrmParameters with rrmConfig = "+rrmConfigNode.toString());
        JsonNode pA = rrmConfigNode.path("p_a");
        if (!pA.isMissingNode()) {
            RRMConfig.Pa pa = new RRMConfig.Pa();

            List<XICICPA> collect = Lists.newArrayList();
            collect.add(new XICICPA(pA.asInt()));
            pa.getXICICPA().clear();
            pa.getXICICPA().addAll(collect);

            rrmParameters.setPa(pa);
        }

        JsonNode startPrbDl1 = rrmConfigNode.path("start_prb_dl");
        if (!startPrbDl1.isMissingNode()) {
            RRMConfig.StartPrbDl startPrbDl = new RRMConfig.StartPrbDl();

            List<BerInteger> collect = Lists.newArrayList();
            collect.add(new BerInteger(startPrbDl1.asInt()));
            startPrbDl.getBerInteger().clear();
            startPrbDl.getBerInteger().addAll(collect);
            log.info("\n!!!!! setting startPrbDl = "+startPrbDl.toString());
            rrmParameters.setStartPrbDl(startPrbDl);
        }

        JsonNode endPrbDl1 = rrmConfigNode.path("end_prb_dl");
        if (!endPrbDl1.isMissingNode()) {
            RRMConfig.EndPrbDl endPrbDl = new RRMConfig.EndPrbDl();
            log.info("\n!!!!! setting endPrbDl1 = "+endPrbDl.toString());
            List<BerInteger> collect = Lists.newArrayList();
            collect.add(new BerInteger(endPrbDl1.asInt()));
            endPrbDl.getBerInteger().clear();
            endPrbDl.getBerInteger().addAll(collect);
            log.info("\n!!!!! setting endPrbDl = "+endPrbDl.toString());
            rrmParameters.setEndPrbDl(endPrbDl);
        }

        JsonNode subFrameBitmaskDl = rrmConfigNode.path("sub_frame_bitmask_dl");
        if (!subFrameBitmaskDl.isMissingNode()) {
            RRMConfig.SubframeBitmaskDl subframeBitmaskDl = new RRMConfig.SubframeBitmaskDl();
            List<BerBitString> collect = Lists.newArrayList();

            byte[] hexString = DatatypeConverter.parseHexBinary(subFrameBitmaskDl.asText());
            collect.add(new BerBitString(hexString, 10));
            subframeBitmaskDl.getBerBitString().clear();
            subframeBitmaskDl.getBerBitString().addAll(collect);

            rrmParameters.setSubframeBitmaskDl(subframeBitmaskDl);
        }

        JsonNode startPrbUl1 = rrmConfigNode.path("start_prb_ul");
        if (!startPrbUl1.isMissingNode()) {
            RRMConfig.StartPrbUl startPrbUl = new RRMConfig.StartPrbUl();

            List<BerInteger> collect = Lists.newArrayList();
            collect.add(new BerInteger(startPrbUl1.asInt()));
            startPrbUl.getBerInteger().clear();
            startPrbUl.getBerInteger().addAll(collect);

            rrmParameters.setStartPrbUl(startPrbUl);
        }

        JsonNode endPrbUl1 = rrmConfigNode.path("end_prb_ul");
        if (!endPrbUl1.isMissingNode()) {
            RRMConfig.EndPrbUl endPrbUl = new RRMConfig.EndPrbUl();

            List<BerInteger> collect = Lists.newArrayList();
            collect.add(new BerInteger(endPrbUl1.asInt()));
            endPrbUl.getBerInteger().clear();
            endPrbUl.getBerInteger().addAll(collect);

            rrmParameters.setEndPrbUl(endPrbUl);
        }


        JsonNode p0UePusch1 = rrmConfigNode.path("p0_ue_pusch");
        if (!p0UePusch1.isMissingNode()) {
            RRMConfig.P0UePusch p0UePusch = new RRMConfig.P0UePusch();

            List<BerInteger> collect = Lists.newArrayList();
            collect.add(new BerInteger(p0UePusch1.asInt()));
            p0UePusch.getBerInteger().clear();
            p0UePusch.getBerInteger().addAll(collect);

            rrmParameters.setP0UePusch(p0UePusch);
        }

        JsonNode subFrameBitmaskUl = rrmConfigNode.path("sub_frame_bitmask_ul");
        if (!subFrameBitmaskUl.isMissingNode()) {
            RRMConfig.SubframeBitmaskUl subframeBitmaskUl = new RRMConfig.SubframeBitmaskUl();
            List<BerBitString> collect = Lists.newArrayList();

            byte[] hexString = DatatypeConverter.parseHexBinary(subFrameBitmaskUl.asText());
            collect.add(new BerBitString(hexString, 10));
            subframeBitmaskUl.getBerBitString().clear();
            subframeBitmaskUl.getBerBitString().addAll(collect);

            rrmParameters.setSubframeBitmaskUl(subframeBitmaskUl);
        }
    }

    @Override
    public String toString() {
        return "RnibLink{" +
                "measurements=" + measurements +
                ", linkId=" + linkId +
                ", rrmParameters=" + rrmParameters +
                ", trafficPercent=" + trafficPercent +
                ", bearerParameters=" + bearerParameters +
                ", type=" + type +
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

        RnibLink link = (RnibLink) o;

        return linkId.equals(link.linkId);
    }

    @Override
    public int hashCode() {
        return linkId.hashCode();
    }

    /**
     * Enum of Link-Type.
     */
    public enum Type {
        SERVING_PRIMARY("serving/primary") {
            @Override
            public String toString() {
                return "serving/primary";
            }
        },
        SERVING_SECONDARY_CA("serving/secondary/ca") {
            @Override
            public String toString() {
                return "serving/secondary/ca";
            }
        },
        SERVING_SECONDARY_DC("serving/secondary/dc") {
            @Override
            public String toString() {
                return "serving/secondary/dc";
            }
        },
        NON_SERVING("non-serving") {
            @Override
            public String toString() {
                return "non-serving";
            }
        };

        private String name;

        Type(String name) {
            this.name = name;
        }

        /**
         * Get enum value of link-type.
         *
         * @param name String representation of Enum Type
         * @return Type
         */
        public static Type getEnum(String name) {
            Optional<Type> any = Arrays.stream(Type.values()).filter(typeStr -> typeStr.name.equals(name)).findAny();
            if (any.isPresent()) {
                return any.get();
            }
            throw new IllegalArgumentException("No enum defined for string: " + name);
        }
    }

    @JsonPropertyOrder({
            "RXSigReport",
            "RadioReport",
            "SchedMeasReport",
            "PDCPMeasReport",

    })
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Measurements {
        @JsonProperty("RXSigReport")
        RXSigReport rxSigReport = new RXSigReport();
        @JsonProperty("RadioReport")
        RadioReport radioReport = new RadioReport();
        @JsonProperty("SchedMeasReport")
        SchedMeasReport schedMeasReport = new SchedMeasReport();
        @JsonProperty("PDCPMeasReport")
        PdcpMeasReport pdcpMeasReport = new PdcpMeasReport();

        public Measurements() {
        }

        @JsonCreator
        public Measurements(@JsonProperty("RXSigReport") RXSigReport rxSigReport,
                            @JsonProperty("RadioReport") RadioReport radioReport,
                            @JsonProperty("SchedMeasReport") SchedMeasReport schedMeasReport,
                            @JsonProperty("PDCPMeasReport") PdcpMeasReport pdcpMeasReport) {
            this.rxSigReport = rxSigReport;
            this.radioReport = radioReport;
            this.schedMeasReport = schedMeasReport;
            this.pdcpMeasReport = pdcpMeasReport;
        }

        public RXSigReport getRxSigReport() {
            return rxSigReport;
        }

        public void setRxSigReport(RXSigReport rxSigReport) {
            this.rxSigReport = rxSigReport;
        }

        public RadioReport getRadioReport() {
            return radioReport;
        }

        public void setRadioReport(RadioReport radioReport) {
            this.radioReport = radioReport;
        }

        public SchedMeasReport getSchedMeasReport() {
            return schedMeasReport;
        }

        public void setSchedMeasReport(SchedMeasReport schedMeasReport) {
            this.schedMeasReport = schedMeasReport;
        }

        public PdcpMeasReport getPdcpMeasReport() {
            return pdcpMeasReport;
        }

        public void setPdcpMeasReport(PdcpMeasReport pdcpMeasReport) {
            this.pdcpMeasReport = pdcpMeasReport;
        }

        @Override
        public String toString() {
            return "Measurements{" +
                    "rxSigReport=" + rxSigReport +
                    ", radioReport=" + radioReport +
                    ", schedMeasReport=" + schedMeasReport +
                    ", pdcpMeasReport=" + pdcpMeasReport +
                    '}';
        }

        @JsonPropertyOrder({
                "RSRP",
                "RSRQ",
                "meas-id",
                "timestamp"
        })
        public static class RXSigReport {
            @JsonProperty("RSRP")
            double rsrp;
            @JsonProperty("RSRQ")
            double rsrq;
            @JsonProperty("meas-id")
            RXSigMeasReport.CellMeasReports measReports;
            @JsonProperty("timestamp")
            WallClockTimestamp timesincelastupdate = new WallClockTimestamp();

            public RXSigReport() {
            }

            @JsonCreator
            public RXSigReport(@JsonProperty("RSRP") double rsrp,
                               @JsonProperty("RSRQ") double rsrq,
                               @JsonProperty("meas-id") RXSigMeasReport.CellMeasReports measReports
            ) {
                this.rsrp = rsrp;
                this.rsrq = rsrq;
                this.measReports = measReports;
            }

            /**
             * Get rsrp.
             *
             * @return double rsrp
             */
            public double getRsrp() {
                return rsrp;
            }

            /**
             * Set rsrp.
             *
             * @param rsrp rsrp
             */
            public void setRsrp(double rsrp) {
                this.rsrp = rsrp;
            }

            /**
             * Get rsrq.
             *
             * @return double rsrq
             */
            public double getRsrq() {
                return rsrq;
            }

            /**
             * Set rsrq.
             *
             * @param rsrq rsrq
             */
            public void setRsrq(double rsrq) {
                this.rsrq = rsrq;
            }

            /**
             * Get time since last update.
             *
             * @return long Time
             */
            public long getTimesincelastupdate() {
                return new WallClockTimestamp().unixTimestamp() - timesincelastupdate.unixTimestamp();
            }

            /**
             * Set time since last update.
             *
             * @param timesincelastupdate time since last update
             */
            public void setTimesincelastupdate(WallClockTimestamp timesincelastupdate) {
                this.timesincelastupdate = timesincelastupdate;
            }

            public RXSigMeasReport.CellMeasReports getMeasReports() {
                return measReports;
            }

            public void setMeasReports(RXSigMeasReport.CellMeasReports measReports) {
                this.measReports = measReports;
            }

            @Override
            public String toString() {
                return "RXSigReport{" +
                        "rsrp=" + rsrp +
                        ", rsrq=" + rsrq +
                        ", measReports=" + measReports +
                        ", timestamp=" + getTimesincelastupdate() +
                        '}';
            }
        }

        @JsonPropertyOrder({
                "CQI",
                "Rank_hist",
                "Pusch_sinr_hist",
                "Pucch_sinr_hist",
                "timestamp"
        })
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RadioReport {
            @JsonProperty("CQI")
            Cqi cqi = new Cqi();
            @JsonProperty("Rank_hist")
            RadioRepPerServCell.RiHist riHist;
            @JsonProperty("Pucch_sinr_hist")
            RadioRepPerServCell.PucchSinrHist pucchSinrHist;
            @JsonProperty("Pusch_sinr_hist")
            RadioRepPerServCell.PuschSinrHist puschSinrHist;
            @JsonProperty("timestamp")
            WallClockTimestamp timesincelastupdate = new WallClockTimestamp();

            public RadioReport() {
            }

            @JsonCreator
            public RadioReport(@JsonProperty("CQI") Cqi cqi,
                               @JsonProperty("Rank_hist") RadioRepPerServCell.RiHist riHist,
                               @JsonProperty("Pucch_sinr_hist") RadioRepPerServCell.PucchSinrHist pucchSinrHist,
                               @JsonProperty("Pusch_sinr_hist") RadioRepPerServCell.PuschSinrHist puschSinrHist) {
                this.cqi = cqi;
                this.riHist = riHist;
                this.pucchSinrHist = pucchSinrHist;
                this.puschSinrHist = puschSinrHist;
            }

            public Cqi getCqi() {
                return cqi;
            }

            public void setCqi(Cqi cqi) {
                this.cqi = cqi;
            }

            public RadioRepPerServCell.RiHist getRiHist() {
                return riHist;
            }

            public void setRiHist(RadioRepPerServCell.RiHist riHist) {
                this.riHist = riHist;
            }

            public RadioRepPerServCell.PucchSinrHist getPucchSinrHist() {
                return pucchSinrHist;
            }

            public void setPucchSinrHist(RadioRepPerServCell.PucchSinrHist pucchSinrHist) {
                this.pucchSinrHist = pucchSinrHist;
            }

            public RadioRepPerServCell.PuschSinrHist getPuschSinrHist() {
                return puschSinrHist;
            }

            public void setPuschSinrHist(RadioRepPerServCell.PuschSinrHist puschSinrHist) {
                this.puschSinrHist = puschSinrHist;
            }

            /**
             * Get time since last update.
             *
             * @return long Time
             */
            public long getTimesincelastupdate() {
                return new WallClockTimestamp().unixTimestamp() - timesincelastupdate.unixTimestamp();
            }

            public void setTimesincelastupdate(WallClockTimestamp timesincelastupdate) {
                this.timesincelastupdate = timesincelastupdate;
            }

            @Override
            public String toString() {
                return "RadioReport{" +
                        "cqi=" + cqi +
                        ", riHist=" + riHist +
                        ", pucchSinrHist=" + pucchSinrHist +
                        ", puschSinrHist=" + puschSinrHist +
                        ", timestamp=" + getTimesincelastupdate() +
                        '}';
            }

            @JsonPropertyOrder({
                    "Hist",
                    "Mode",
                    "Mean",
                    "timestamp"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Cqi {
                @JsonProperty("Hist")
                RadioRepPerServCell.CqiHist hist;
                @JsonProperty("Mode")
                double mode;
                @JsonProperty("Mean")
                double mean;

                public Cqi() {
                }

                @JsonCreator
                public Cqi(@JsonProperty("Hist") RadioRepPerServCell.CqiHist hist,
                           @JsonProperty("Mode") double mode,
                           @JsonProperty("Mean") double mean) {
                    this.hist = hist;
                    this.mode = mode;
                    this.mean = mean;
                }

                /**
                 * Get CQIHist.
                 *
                 * @return CqiHist
                 */
                public RadioRepPerServCell.CqiHist getHist() {
                    return hist;
                }

                /**
                 * Get CQIHist.
                 *
                 * @param hist CqiHist
                 */
                public void setHist(RadioRepPerServCell.CqiHist hist) {
                    this.hist = hist;
                }

                /**
                 * Get mode.
                 *
                 * @return double mode
                 */
                public double getMode() {
                    return mode;
                }

                /**
                 * Set mode.
                 *
                 * @param mode mode
                 */
                public void setMode(double mode) {
                    this.mode = mode;
                }

                /**
                 * Get mean.
                 *
                 * @return double mean
                 */
                public double getMean() {
                    return mean;
                }

                /**
                 * Set mean.
                 *
                 * @param mean mean
                 */
                public void setMean(double mean) {
                    this.mean = mean;
                }

                @Override
                public String toString() {
                    return "Cqi{" +
                            "hist=" + hist +
                            ", mode=" + mode +
                            ", mean=" + mean +
                            '}';
                }
            }
        }

        @JsonPropertyOrder({
                "QCI",
                "ResourceUsage",
                "MCS",
                "Num_Sched_TTIs",
                "DL_rank_stats",
                "timestamp"
        })
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SchedMeasReport {
            @JsonProperty("QCI")
            SchedMeasRepPerServCell.QciVals qci;
            @JsonProperty("ResourceUsage")
            ResourceUsage resourceUsage = new ResourceUsage();
            @JsonProperty("MCS")
            Mcs mcs = new Mcs();
            @JsonProperty("Num_Sched_TTIs")
            NumSchedTtis numSchedTtis = new NumSchedTtis();
            @JsonProperty("DL_rank_stats")
            DlRankStats dlRankStats = new DlRankStats();
            @JsonProperty("timestamp")
            WallClockTimestamp timesincelastupdate = new WallClockTimestamp();

            public SchedMeasReport() {
            }

            @JsonCreator
            public SchedMeasReport(
                    @JsonProperty("QCI") SchedMeasRepPerServCell.QciVals qci,
                    @JsonProperty("ResourceUsage") ResourceUsage resourceUsage,
                    @JsonProperty("MCS") Mcs mcs,
                    @JsonProperty("Num_Sched_TTIs") NumSchedTtis numSchedTtis,
                    @JsonProperty("DL_rank_stats") DlRankStats dlRankStats
            ) {
                this.qci = qci;
                this.resourceUsage = resourceUsage;
                this.mcs = mcs;
                this.numSchedTtis = numSchedTtis;
                this.dlRankStats = dlRankStats;
            }

            public SchedMeasRepPerServCell.QciVals getQci() {
                return qci;
            }

            public void setQci(SchedMeasRepPerServCell.QciVals qci) {
                this.qci = qci;
            }

            public ResourceUsage getResourceUsage() {
                return resourceUsage;
            }

            public void setResourceUsage(ResourceUsage resourceUsage) {
                this.resourceUsage = resourceUsage;
            }

            public Mcs getMcs() {
                return mcs;
            }

            public void setMcs(Mcs mcs) {
                this.mcs = mcs;
            }

            public NumSchedTtis getNumSchedTtis() {
                return numSchedTtis;
            }

            public void setNumSchedTtis(NumSchedTtis numSchedTtis) {
                this.numSchedTtis = numSchedTtis;
            }

            public DlRankStats getDlRankStats() {
                return dlRankStats;
            }

            public void setDlRankStats(DlRankStats dlRankStats) {
                this.dlRankStats = dlRankStats;
            }

            public long getTimesincelastupdate() {
                return new WallClockTimestamp().unixTimestamp() - timesincelastupdate.unixTimestamp();
            }

            public void setTimesincelastupdate(WallClockTimestamp timesincelastupdate) {
                this.timesincelastupdate = timesincelastupdate;
            }

            @Override
            public String toString() {
                return "SchedMeasReport{" +
                        "qci=" + qci +
                        ", resourceUsage=" + resourceUsage +
                        ", mcs=" + mcs +
                        ", numSchedTtis=" + numSchedTtis +
                        ", dlRankStats=" + dlRankStats +
                        ", timesincelastupdate=" + getTimesincelastupdate() +
                        '}';
            }

            @JsonPropertyOrder({
                    "dl",
                    "ul"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class ResourceUsage {
                @JsonProperty("dl")
                PRBUsage.PrbUsageDl dl;
                @JsonProperty("ul")
                PRBUsage.PrbUsageUl ul;

                public ResourceUsage() {
                }

                @JsonCreator
                public ResourceUsage(@JsonProperty("dl") PRBUsage.PrbUsageDl dl,
                                     @JsonProperty("ul") PRBUsage.PrbUsageUl ul) {
                    this.dl = dl;
                    this.ul = ul;
                }

                /**
                 * Get DL.
                 *
                 * @return Dl
                 */
                public PRBUsage.PrbUsageDl getDl() {
                    return dl;
                }

                /**
                 * Set DL.
                 *
                 * @param dl DL
                 */
                public void setDl(PRBUsage.PrbUsageDl dl) {
                    this.dl = dl;
                }

                /**
                 * Get UL.
                 *
                 * @return Ul
                 */
                public PRBUsage.PrbUsageUl getUl() {
                    return ul;
                }

                /**
                 * Set UL.
                 *
                 * @param ul Ul
                 */
                public void setUl(PRBUsage.PrbUsageUl ul) {
                    this.ul = ul;
                }

                @Override
                public String toString() {
                    return "ResourceUsage{" +
                            "dl=" + dl +
                            ", ul=" + ul +
                            '}';
                }
            }

            @JsonPropertyOrder({
                    "dl",
                    "ul"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Mcs {
                SchedMeasRepPerServCell.McsDl dl;
                SchedMeasRepPerServCell.McsUl ul;

                public Mcs() {
                }

                @JsonCreator
                public Mcs(@JsonProperty("dl") SchedMeasRepPerServCell.McsDl dl,
                           @JsonProperty("ul") SchedMeasRepPerServCell.McsUl ul) {
                    this.dl = dl;
                    this.ul = ul;
                }

                /**
                 * Get DL.
                 *
                 * @return Dl
                 */
                public SchedMeasRepPerServCell.McsDl getDl() {
                    return dl;
                }

                /**
                 * Set DL.
                 *
                 * @param dl DL
                 */
                public void setDl(SchedMeasRepPerServCell.McsDl dl) {
                    this.dl = dl;
                }

                /**
                 * Get UL.
                 *
                 * @return Ul
                 */
                public SchedMeasRepPerServCell.McsUl getUl() {
                    return ul;
                }

                /**
                 * Set UL.
                 *
                 * @param ul Ul
                 */
                public void setUl(SchedMeasRepPerServCell.McsUl ul) {
                    this.ul = ul;
                }

                @Override
                public String toString() {
                    return "mcs{" +
                            "dl=" + dl +
                            ", ul=" + ul +
                            '}';
                }
            }

            @JsonPropertyOrder({
                    "dl",
                    "ul"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class NumSchedTtis {
                @JsonProperty("dl")
                SchedMeasRepPerServCell.NumSchedTtisDl dl;
                @JsonProperty("ul")
                SchedMeasRepPerServCell.NumSchedTtisUl ul;

                public NumSchedTtis() {
                }

                @JsonCreator
                public NumSchedTtis(@JsonProperty("dl") SchedMeasRepPerServCell.NumSchedTtisDl dl,
                                    @JsonProperty("ul") SchedMeasRepPerServCell.NumSchedTtisUl ul) {
                    this.dl = dl;
                    this.ul = ul;
                }

                public SchedMeasRepPerServCell.NumSchedTtisDl getDl() {
                    return dl;
                }

                public void setDl(SchedMeasRepPerServCell.NumSchedTtisDl dl) {
                    this.dl = dl;
                }

                public SchedMeasRepPerServCell.NumSchedTtisUl getUl() {
                    return ul;
                }

                public void setUl(SchedMeasRepPerServCell.NumSchedTtisUl ul) {
                    this.ul = ul;
                }

                @Override
                public String toString() {
                    return "NumSchedTtis{" +
                            "dl=" + dl +
                            ", ul=" + ul +
                            '}';
                }
            }

            @JsonPropertyOrder({
                    "Rank-1",
                    "Rank-2"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class DlRankStats {
                @JsonProperty("Rank-1")
                SchedMeasRepPerServCell.RankDl1 rankDl1;
                @JsonProperty("Rank-2")
                SchedMeasRepPerServCell.RankDl2 rankDl2;

                public DlRankStats() {
                }

                @JsonCreator
                public DlRankStats(@JsonProperty("Rank-1") SchedMeasRepPerServCell.RankDl1 rankDl1,
                                   @JsonProperty("Rank-2") SchedMeasRepPerServCell.RankDl2 rankDl2) {
                    this.rankDl1 = rankDl1;
                    this.rankDl2 = rankDl2;
                }

                public SchedMeasRepPerServCell.RankDl1 getRankDl1() {
                    return rankDl1;
                }

                public void setRankDl1(SchedMeasRepPerServCell.RankDl1 rankDl1) {
                    this.rankDl1 = rankDl1;
                }

                public SchedMeasRepPerServCell.RankDl2 getRankDl2() {
                    return rankDl2;
                }

                public void setRankDl2(SchedMeasRepPerServCell.RankDl2 rankDl2) {
                    this.rankDl2 = rankDl2;
                }

                @Override
                public String toString() {
                    return "DlRankStats{" +
                            "rankDl1=" + rankDl1 +
                            ", rankDl2=" + rankDl2 +
                            '}';
                }
            }
        }

        @JsonPropertyOrder({
                "QCI",
                "PDCPThroughput",
                "Data_vol",
                "Pkt_delay_dl",
                "Pkt_discard_rate_dl",
                "Pkt_loss_rate",
                "timestamp"
        })
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PdcpMeasReport {
            @JsonProperty("QCI")
            PDCPMeasReportPerUe.QciVals qci = new PDCPMeasReportPerUe.QciVals();
            @JsonProperty("PDCPThroughput")
            PdcpThroughput pdcpThroughput = new PdcpThroughput();
            @JsonProperty("Data_vol")
            DataVol dataVol = new DataVol();
            @JsonProperty("Pkt_delay_dl")
            PDCPMeasReportPerUe.PktDelayDl pktDelayDl;
            @JsonProperty("Pkt_discard_rate_dl")
            PDCPMeasReportPerUe.PktDiscardRateDl pktDiscardRateDl;
            @JsonProperty("Pkt_loss_rate")
            PktLossRate pktLossRate = new PktLossRate();
            @JsonProperty("timestamp")
            WallClockTimestamp timesincelastupdate = new WallClockTimestamp();

            public PdcpMeasReport() {
            }

            @JsonCreator
            public PdcpMeasReport(
                    @JsonProperty("QCI") PDCPMeasReportPerUe.QciVals qci,
                    @JsonProperty("PDCPThroughput") PdcpThroughput pdcpThroughput,
                    @JsonProperty("Data_vol") DataVol dataVol,
                    @JsonProperty("Pkt_delay_dl") PDCPMeasReportPerUe.PktDelayDl pktDelayDl,
                    @JsonProperty("Pkt_discard_rate_dl") PDCPMeasReportPerUe.PktDiscardRateDl pktDiscardRateDl,
                    @JsonProperty("Pkt_loss_rate") PktLossRate pktLossRate
            ) {
                this.qci = qci;
                this.pdcpThroughput = pdcpThroughput;
                this.dataVol = dataVol;
                this.pktDelayDl = pktDelayDl;
                this.pktDiscardRateDl = pktDiscardRateDl;
                this.pktLossRate = pktLossRate;
            }

            public PDCPMeasReportPerUe.QciVals getQci() {
                return qci;
            }

            public void setQci(PDCPMeasReportPerUe.QciVals qci) {
                this.qci = qci;
            }

            public PdcpThroughput getPdcpThroughput() {
                return pdcpThroughput;
            }

            public void setPdcpThroughput(PdcpThroughput pdcpThroughput) {
                this.pdcpThroughput = pdcpThroughput;
            }

            public DataVol getDataVol() {
                return dataVol;
            }

            public void setDataVol(DataVol dataVol) {
                this.dataVol = dataVol;
            }

            public PDCPMeasReportPerUe.PktDelayDl getPktDelayDl() {
                return pktDelayDl;
            }

            public void setPktDelayDl(PDCPMeasReportPerUe.PktDelayDl pktDelayDl) {
                this.pktDelayDl = pktDelayDl;
            }

            public PDCPMeasReportPerUe.PktDiscardRateDl getPktDiscardRateDl() {
                return pktDiscardRateDl;
            }

            public void setPktDiscardRateDl(PDCPMeasReportPerUe.PktDiscardRateDl pktDiscardRateDl) {
                this.pktDiscardRateDl = pktDiscardRateDl;
            }

            public PktLossRate getPktLossRate() {
                return pktLossRate;
            }

            public void setPktLossRate(PktLossRate pktLossRate) {
                this.pktLossRate = pktLossRate;
            }

            public long getTimesincelastupdate() {
                return new WallClockTimestamp().unixTimestamp() - timesincelastupdate.unixTimestamp();
            }

            public void setTimesincelastupdate(WallClockTimestamp timesincelastupdate) {
                this.timesincelastupdate = timesincelastupdate;
            }

            @Override
            public String toString() {
                return "PdcpMeasReport{" +
                        "qci=" + qci +
                        ", pdcpThroughput=" + pdcpThroughput +
                        ", dataVol=" + dataVol +
                        ", pktDelayDl=" + pktDelayDl +
                        ", pktDiscardRateDl=" + pktDiscardRateDl +
                        ", pktLossRate=" + pktLossRate +
                        ", timesincelastupdate=" + getTimesincelastupdate() +
                        '}';
            }

            @JsonPropertyOrder({
                    "dl",
                    "ul"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class PdcpThroughput {
                @JsonProperty("dl")
                private PDCPMeasReportPerUe.ThroughputDl dl;
                @JsonProperty("ul")
                private PDCPMeasReportPerUe.ThroughputUl ul;

                public PdcpThroughput() {
                }

                @JsonCreator
                public PdcpThroughput(@JsonProperty("dl") PDCPMeasReportPerUe.ThroughputDl dl,
                                      @JsonProperty("ul") PDCPMeasReportPerUe.ThroughputUl ul) {
                    this.dl = dl;
                    this.ul = ul;
                }

                /**
                 * Get DL.
                 *
                 * @return Dl
                 */
                public PDCPMeasReportPerUe.ThroughputDl getDl() {
                    return dl;
                }

                /**
                 * Set DL.
                 *
                 * @param dl DL
                 */
                public void setDl(PDCPMeasReportPerUe.ThroughputDl dl) {
                    this.dl = dl;
                }

                /**
                 * Get UL.
                 *
                 * @return Ul
                 */
                public PDCPMeasReportPerUe.ThroughputUl getUl() {
                    return ul;
                }

                /**
                 * Set UL.
                 *
                 * @param ul Ul
                 */
                public void setUl(PDCPMeasReportPerUe.ThroughputUl ul) {
                    this.ul = ul;
                }

                @Override
                public String
                toString() {
                    return "PdcpThroughput{" +
                            "dl=" + dl +
                            ", ul=" + ul +
                            '}';
                }
            }

            @JsonPropertyOrder({
                    "dl",
                    "ul"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class DataVol {
                @JsonProperty("dl")
                private PDCPMeasReportPerUe.DataVolDl dl;
                @JsonProperty("ul")
                private PDCPMeasReportPerUe.DataVolUl ul;

                public DataVol() {
                }

                @JsonCreator
                public DataVol(@JsonProperty("dl") PDCPMeasReportPerUe.DataVolDl dl,
                               @JsonProperty("ul") PDCPMeasReportPerUe.DataVolUl ul) {
                    this.dl = dl;
                    this.ul = ul;
                }

                /**
                 * Get DL.
                 *
                 * @return Dl
                 */
                public PDCPMeasReportPerUe.DataVolDl getDl() {
                    return dl;
                }

                /**
                 * Set DL.
                 *
                 * @param dl DL
                 */
                public void setDl(PDCPMeasReportPerUe.DataVolDl dl) {
                    this.dl = dl;
                }

                /**
                 * Get UL.
                 *
                 * @return Ul
                 */
                public PDCPMeasReportPerUe.DataVolUl getUl() {
                    return ul;
                }

                /**
                 * Set UL.
                 *
                 * @param ul Ul
                 */
                public void setUl(PDCPMeasReportPerUe.DataVolUl ul) {
                    this.ul = ul;
                }

                @Override
                public String
                toString() {
                    return "PdcpThroughput{" +
                            "dl=" + dl +
                            ", ul=" + ul +
                            '}';
                }
            }

            @JsonPropertyOrder({
                    "dl",
                    "ul"
            })
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class PktLossRate {
                @JsonProperty("dl")
                PDCPMeasReportPerUe.PktLossRateDl dl;
                @JsonProperty("ul")
                PDCPMeasReportPerUe.PktLossRateUl ul;

                public PktLossRate() {
                }

                @JsonCreator
                public PktLossRate(@JsonProperty("dl") PDCPMeasReportPerUe.PktLossRateDl dl,
                                   @JsonProperty("ul") PDCPMeasReportPerUe.PktLossRateUl ul) {
                    this.dl = dl;
                    this.ul = ul;
                }

                /**
                 * Get DL.
                 *
                 * @return Dl
                 */
                public PDCPMeasReportPerUe.PktLossRateDl getDl() {
                    return dl;
                }

                /**
                 * Set DL.
                 *
                 * @param dl DL
                 */
                public void setDl(PDCPMeasReportPerUe.PktLossRateDl dl) {
                    this.dl = dl;
                }

                /**
                 * Get UL.
                 *
                 * @return Ul
                 */
                public PDCPMeasReportPerUe.PktLossRateUl getUl() {
                    return ul;
                }

                /**
                 * Set UL.
                 *
                 * @param ul Ul
                 */
                public void setUl(PDCPMeasReportPerUe.PktLossRateUl ul) {
                    this.ul = ul;
                }

                @Override
                public String toString() {
                    return "PdcpPacketdelay{" +
                            "dl=" + dl +
                            ", ul=" + ul +
                            '}';
                }
            }
        }
    }
}
//static nested class not inner class