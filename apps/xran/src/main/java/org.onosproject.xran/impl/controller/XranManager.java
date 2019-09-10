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

package org.onosproject.xran.impl.controller;

import com.google.common.collect.Sets;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.sctp.SctpMessage;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.layout.LayoutAlgorithm;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.xran.XranDeviceAgent;
import org.onosproject.xran.XranDeviceListener;
import org.onosproject.xran.XranHostAgent;
import org.onosproject.xran.XranHostListener;
import org.onosproject.xran.XranPacketProcessor;
import org.onosproject.xran.XranService;
import org.onosproject.xran.XranStore;
import org.onosproject.xran.asn1lib.api.*;
import org.onosproject.xran.asn1lib.ber.types.BerBoolean;
import org.onosproject.xran.asn1lib.ber.types.BerEnum;
import org.onosproject.xran.asn1lib.ber.types.BerInteger;
import org.onosproject.xran.asn1lib.pdu.BearerAdmissionRequest;
import org.onosproject.xran.asn1lib.pdu.BearerAdmissionResponse;
import org.onosproject.xran.asn1lib.pdu.BearerAdmissionStatus;
import org.onosproject.xran.asn1lib.pdu.BearerReleaseInd;
import org.onosproject.xran.asn1lib.pdu.CellConfigReport;
import org.onosproject.xran.asn1lib.pdu.CellConfigRequest;
import org.onosproject.xran.asn1lib.pdu.HOCause;
import org.onosproject.xran.asn1lib.pdu.HOComplete;
import org.onosproject.xran.asn1lib.pdu.HOFailure;
import org.onosproject.xran.asn1lib.pdu.HORequest;
import org.onosproject.xran.asn1lib.pdu.L2MeasConfig;
import org.onosproject.xran.asn1lib.pdu.PDCPMeasReportPerUe;
import org.onosproject.xran.asn1lib.pdu.RRCMeasConfig;
import org.onosproject.xran.asn1lib.pdu.RRMConfig;
import org.onosproject.xran.asn1lib.pdu.RRMConfigStatus;
import org.onosproject.xran.asn1lib.pdu.RXSigMeasConfig;
import org.onosproject.xran.asn1lib.pdu.RXSigMeasReport;
import org.onosproject.xran.asn1lib.pdu.RadioMeasReportPerCell;
import org.onosproject.xran.asn1lib.pdu.RadioMeasReportPerUE;
import org.onosproject.xran.asn1lib.pdu.ScellAdd;
import org.onosproject.xran.asn1lib.pdu.ScellAddStatus;
import org.onosproject.xran.asn1lib.pdu.ScellDelete;
import org.onosproject.xran.asn1lib.pdu.SchedMeasReportPerCell;
import org.onosproject.xran.asn1lib.pdu.SchedMeasReportPerUE;
import org.onosproject.xran.asn1lib.pdu.SeNBAdd;
import org.onosproject.xran.asn1lib.pdu.SeNBAddStatus;
import org.onosproject.xran.asn1lib.pdu.SeNBDelete;
import org.onosproject.xran.asn1lib.pdu.TrafficSplitConfig;
import org.onosproject.xran.asn1lib.pdu.UEAdmissionRequest;
import org.onosproject.xran.asn1lib.pdu.UEAdmissionResponse;
import org.onosproject.xran.asn1lib.pdu.UEAdmissionStatus;
import org.onosproject.xran.asn1lib.pdu.UECapabilityEnquiry;
import org.onosproject.xran.asn1lib.pdu.UECapabilityInfo;
import org.onosproject.xran.asn1lib.pdu.UEContextUpdate;
import org.onosproject.xran.asn1lib.pdu.UEReconfigInd;
import org.onosproject.xran.asn1lib.pdu.UEReleaseInd;
import org.onosproject.xran.asn1lib.pdu.XrancPdu;
import org.onosproject.xran.impl.DefaultXranStore;
import org.onosproject.xran.impl.XranConfig;
import org.onosproject.xran.impl.entities.RnibCell;
import org.onosproject.xran.impl.entities.RnibLink;
import org.onosproject.xran.impl.entities.RnibUe;
import org.onosproject.xran.impl.identifiers.ContextUpdateHandler;
import org.onosproject.xran.impl.identifiers.EcgiCrntiPair;
import org.onosproject.xran.impl.identifiers.LinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.client.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.xran.impl.controller.XranChannelHandler.getSctpMessage;
import static org.onosproject.xran.impl.entities.RnibCell.decodeDeviceId;
import static org.onosproject.xran.impl.entities.RnibCell.uri;
import static org.onosproject.xran.impl.entities.RnibUe.hostIdtoUEId;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by dimitris on 7/20/17.
 */
@Component(immediate = true)
@Service
public class XranManager implements XranService {
    protected static final String XRAN_APP_ID = "org.onosproject.xran";
    protected static final Class<XranConfig> CONFIG_CLASS = XranConfig.class;

    protected static final Logger log =
            LoggerFactory.getLogger(XranManager.class);
    protected static long CQI_AVG = 17;

    /* CONFIG */
    protected final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    /* VARIABLES */
    protected final XranServer xranServer = new XranServer();
    protected XranConfig xranConfig;
    protected ApplicationId appId;
    protected int northboundTimeout;

    /* Services */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected XranStore xranStore;

    protected ConfigFactory<ApplicationId, XranConfig> xranConfigFactory =
            new ConfigFactory<ApplicationId, XranConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, CONFIG_CLASS, "xran") {
                @Override
                public XranConfig createConfig() {
                    return new XranConfig();
                }
            };


    /* MAPS */
    protected ConcurrentMap<IpAddress, ECGI> legitCells = new ConcurrentHashMap<>();
    protected static ConcurrentMap<ECGI, String> legitCellsXY = new ConcurrentHashMap<>();
    protected ConcurrentMap<ECGI, SynchronousQueue<String>> hoMap = new ConcurrentHashMap<>();
    protected ConcurrentMap<ECGI, SynchronousQueue<String>> rrmCellMap = new ConcurrentHashMap<>();
    protected ConcurrentMap<CRNTI, SynchronousQueue<String>> scellAddMap = new ConcurrentHashMap<>();
    // Map used to keep messages in pairs (HO Complete - CTX Update, Adm Status - CTX Update)
    protected ConcurrentMap<EcgiCrntiPair, ContextUpdateHandler> contextUpdateMap = new ConcurrentHashMap<>();

    /* QUEUE */
    protected BlockingQueue<Long> ueIdQueue = new LinkedBlockingQueue<>();

    /* AGENTS */
    protected InternalXranDeviceAgent deviceAgent = new InternalXranDeviceAgent();
    protected InternalXranHostAgent hostAgent = new InternalXranHostAgent();
    protected InternalXranPacketAgent packetAgent = new InternalXranPacketAgent();

    /* LISTENERS */
    protected Set<XranDeviceListener> xranDeviceListeners = new CopyOnWriteArraySet<>();
    protected Set<XranHostListener> xranHostListeners = new CopyOnWriteArraySet<>();
    protected InternalDeviceListener deviceListener = new InternalDeviceListener();
    protected InternalHostListener hostListener = new InternalHostListener();
    protected int hoFlag=0;
    protected static int indexofRRPSC = 0;
    protected static int countLines;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(XRAN_APP_ID);

        configService.addListener(configListener);
        registry.registerConfigFactory(xranConfigFactory);
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);

        xranStore.setController(this);

        log.info("XRAN XranServer v5 Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        configService.removeListener(configListener);
        registry.unregisterConfigFactory(xranConfigFactory);

        cleanup();

        log.info("XRAN XranServer v5 Stopped");
    }

    /**
     * Cleanup when application is deactivated.
     */
    private void cleanup() {
        xranStore.getUeNodes().forEach(ue -> xranHostListeners.forEach(l -> l.hostRemoved(ue.getHostId())));

        xranStore.getCellNodes().forEach(cell -> xranDeviceListeners
                .forEach(l -> l.deviceRemoved(deviceId(uri(cell.getEcgi())))));

        xranServer.stop();

        legitCells.clear();
        hoMap.clear();
        rrmCellMap.clear();
        scellAddMap.clear();
        contextUpdateMap.clear();
        ueIdQueue.clear();
        xranDeviceListeners.clear();
        xranHostListeners.clear();
    }

    public static Map<ECGI,String> provideXY() {
        System.out.println("Ankit----xy>" + legitCellsXY);
        return legitCellsXY;
    }
    public static void readCSV(){
//log.info("\n$$$$$$$$$$$$ File => "+ System.getProperty("user.home")+"/file_test13.csv");
        int countLine = 0;
        FileReader csvFile = null;
        String charArr[];
        String line;
        List<String> coordinates = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.home")+"/file_test21.csv"));
            while((line = br.readLine())!=null){
                charArr = line.split(",");
                coordinates.add(charArr[1]);
                countLine++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("$$$$$$$$$$$ => " + countLine);
        countLines = countLine;
    }

    @Override
    public Optional<SynchronousQueue<String>> sendHoRequest(RnibLink linkT, RnibLink linkS) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ECGI ecgiT = linkT.getLinkId().getEcgi(),
                ecgiS = linkS.getLinkId().getEcgi();
        SynchronousQueue<String> queue = new SynchronousQueue<>();

        Optional<ChannelHandlerContext> ctxT = xranStore.getCtx(ecgiT),
                ctxS = xranStore.getCtx(ecgiS);
        executorService.submit(() -> {
            return xranStore.getCrnti(linkT.getLinkId().getUeId()).map(crnti -> {


                XrancPdu xrancPdu = HORequest.constructPacket(crnti, ecgiS, ecgiT);

                // temporary map that has ECGI source of a handoff to a queue waiting for REST response.
                hoMap.put(ecgiS, queue);
                //log.info("\nSending : " + xrancPdu.toString() +"\n");
                ctxT.ifPresent(ctx -> ctx.writeAndFlush(getSctpMessage(xrancPdu)));
                //log.info("Ankit CTXT" + ctxT );
                //log.info("\nSending : " + xrancPdu.toString() +"\n");
                ctxS.ifPresent(ctx -> ctx.writeAndFlush(getSctpMessage(xrancPdu)));
                //log.info("Ankit CTXS" + ctxS );

                // FIXME: only works for one HO at a time.
                try {
                    ueIdQueue.put(linkT.getLinkId().getUeId());
                } catch (InterruptedException e) {
                    log.error(ExceptionUtils.getFullStackTrace(e));
                }

                return Optional.of(queue);
            }).orElse(Optional.empty());
        });
        return Optional.of(queue);

    }

    public static int getRRPSC(){
        if(indexofRRPSC+1 >= countLines){
            indexofRRPSC = 0;
        }
        return indexofRRPSC;
    }
    @Override
    public void addListener(XranDeviceListener listener) {
        xranDeviceListeners.add(listener);
    }

    @Override
    public void addListener(XranHostListener listener) {
        xranHostListeners.add(listener);
    }

    @Override
    public void removeListener(XranDeviceListener listener) {
        xranDeviceListeners.remove(listener);
    }

    @Override
    public void removeListener(XranHostListener listener) {
        xranHostListeners.remove(listener);
    }

    @Override
    public int getNorthboundTimeout() {
        return northboundTimeout;
    }

    @Override
    public Optional<SynchronousQueue<String>> sendModifiedRrm(RRMConfig rrmConfig) {
        ECGI ecgi = rrmConfig.getEcgi();
        Optional<ChannelHandlerContext> optionalCtx = xranStore.getCtx(ecgi);

        // if ctx exists then create the queue and send the message
        return optionalCtx.flatMap(ctx -> {
            XrancPdu pdu;
            pdu = RRMConfig.constructPacket(rrmConfig);
            //log.info("\n!!! rrmConfig = "+rrmConfig.toString());
            //log.info("\nSending : " + pdu.toString() +"\n");
            ctx.writeAndFlush(getSctpMessage(pdu));
            SynchronousQueue<String> queue = new SynchronousQueue<>();
            rrmCellMap.put(ecgi, queue);
            return Optional.of(queue);
        });
    }

    @Override
    public Optional<SynchronousQueue<String>> sendScellAdd(RnibLink link) {
        RnibCell secondaryCell = link.getLinkId().getCell();
        // find primary cell
        return xranStore.getPrimaryCell(link.getLinkId().getUe()).flatMap(primaryCell -> {
                                                                              ECGI primaryEcgi = primaryCell.getEcgi();
                                                                              // get ctx for the primary cell
                                                                              return xranStore.getCtx(primaryEcgi).flatMap(ctx ->
                                                                                                                                   // check if configuration exists
                                                                                                                                   secondaryCell.getOptConf().flatMap(cellReport -> {
                                                                                                                                                                          PCIARFCN pciarfcn = new PCIARFCN();
                                                                                                                                                                          pciarfcn.setPci(cellReport.getPci());
                                                                                                                                                                          pciarfcn.setEarfcnDl(cellReport.getEarfcnDl());

                                                                                                                                                                          PropScell propScell = new PropScell();
                                                                                                                                                                          propScell.setPciArfcn(pciarfcn);

                                                                                                                                                                          // search crnti of specific UE
                                                                                                                                                                          return xranStore.getCrnti(link.getLinkId().getUeId()).flatMap(crnti -> {
                                                                                                                                                                              SynchronousQueue<String> queue;
                                                                                                                                                                              XrancPdu pdu = ScellAdd
                                                                                                                                                                                      .constructPacket(primaryEcgi, crnti, propScell);
                                                                                                                                                                              //log.info("\nSending : " + pdu.toString() +"\n");
                                                                                                                                                                              ctx.writeAndFlush(getSctpMessage(pdu));
                                                                                                                                                                              queue = new SynchronousQueue<>();
                                                                                                                                                                              scellAddMap.put(crnti, queue);

                                                                                                                                                                              return Optional.of(queue);
                                                                                                                                                                          });
                                                                                                                                                                      }
                                                                                                                                   )
                                                                              );
                                                                          }
        );
    }

    @Override
    public boolean sendScellDelete(RnibLink link) {
        RnibCell secondaryCell = link.getLinkId().getCell();
        // find primary cell
        return xranStore.getPrimaryCell(link.getLinkId().getUe()).map(primaryCell -> {
            ECGI primaryEcgi = primaryCell.getEcgi();
            // get ctx for the primary cell
            return xranStore.getCtx(primaryEcgi).map(ctx ->
                                                             // check if config exists
                                                             secondaryCell.getOptConf().map(cellReport -> {
                                                                 PCIARFCN pciarfcn = new PCIARFCN();
                                                                 pciarfcn.setPci(cellReport.getPci());
                                                                 pciarfcn.setEarfcnDl(cellReport.getEarfcnDl());

                                                                 // check if crnti for UE exists
                                                                 return xranStore.getCrnti(link.getLinkId().getUeId()).map(crnti -> {
                                                                     XrancPdu pdu = ScellDelete.constructPacket(primaryEcgi, crnti, pciarfcn);
                                                                     //log.info("\nSending : " + pdu.toString() +"\n");
                                                                     ctx.writeAndFlush(getSctpMessage(pdu));
                                                                     link.setType(RnibLink.Type.NON_SERVING);
                                                                     return true;
                                                                 }).orElse(false);
                                                             }).orElse(false)
            ).orElse(false);
        }).orElse(false);
    }

    /**
     * Timer to delete UE after being IDLE.
     *
     * @param ue UE entity
     */
    private void restartTimer(RnibUe ue) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ue.setExecutor(executor);
        executor.schedule(
                () -> {
                    if (ue.getState().equals(RnibUe.State.IDLE)) {
                        hostAgent.removeConnectedHost(ue);
                        log.info("UE is removed after {} ms of IDLE", xranConfig.getIdleUeRemoval());
                    } else {
                        log.info("UE not removed cause its ACTIVE");
                    }
                },
                xranConfig.getIdleUeRemoval(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Timer to delete LINK after not receiving measurements.
     *
     * @param link LINK entity
     */
    private void restartTimer(RnibLink link) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        link.setExecutor(executor);
        executor.schedule(
                () -> {
                    LinkId linkId = link.getLinkId();
                    xranStore.removeLink(linkId);
                    log.info("Link is removed after not receiving Meas Reports for {} ms",
                             xranConfig.getNoMeasLinkRemoval());
                },
                xranConfig.getNoMeasLinkRemoval(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Request measurement configuration field of specified UE.
     *
     * @param primary primary CELL
     * @param ue      UE entity
     */
    // TODO
    private void populateMeasConfig(RnibCell primary, RnibUe ue) {
        RRCMeasConfig.MeasObjects measObjects = new RRCMeasConfig.MeasObjects();
        RRCMeasConfig.MeasIds measIds = new RRCMeasConfig.MeasIds();
        // get ctx for cell
        //log.info("\n***********populateMeasConfig");
        xranStore.getCtx(primary.getEcgi()).ifPresent(ctx -> {
            // iterate through all cells
            final int[] index = {0};
            xranStore.getCellNodes().forEach(cell ->
                                                     // set pciarfcn if config exists
                                                     cell.getOptConf().ifPresent(cellReport -> {
                                                                                     // PCIARFCN
                                                                                     PCIARFCN pciarfcn = new PCIARFCN();
                                                                                     pciarfcn.setPci(cellReport.getPci());
                                                                                     pciarfcn.setEarfcnDl(cellReport.getEarfcnDl());

                                                                                     // MEAS OBJECT
                                                                                     MeasObject measObject = new MeasObject();
                                                                                     MeasObject.MeasCells measCells = new MeasObject.MeasCells();
                                                                                     measCells.setPci(cellReport.getPci());
                                                                                     measCells.setCellIndividualOffset(new QOffsetRange(0));
                                                                                     measObject.setMeasCells(measCells);
                                                                                     measObject.setDlFreq(cellReport.getEarfcnDl());
                                                                                     measObjects.getMeasObject().add(measObject);

                                                                                     // MEAS ID
                                                                                     MeasID measID = new MeasID();
                                                                                     MeasID.Action action = new MeasID.Action();
                                                                                     action.setHototarget(new BerBoolean(false));
                                                                                     measID.setAction(action);
                                                                                     measID.setReportconfigId(new BerInteger(0));
                                                                                     measID.setMeasobjectId(new BerInteger(index[0]++));
                                                                                     measIds.getMeasID().add(measID);
                                                                                 }
                                                     )
            );
            // REPORT CONFIG

            RRCMeasConfig.ReportConfigs reportConfigs = new RRCMeasConfig.ReportConfigs();

            ReportConfig reportConfig = new ReportConfig();//reportConfigs.getReportConfig().get(0);
            //reportConfigs.getReportConfig().stream().findAny();
            //ReportConfig reportConfig = reportConfigs.getReportConfig();
            //ReportConfig reportConfig = reportConfigs.getReportConfig().get(0);
            reportConfig.setReportQuantity(new BerEnum(0));
            reportConfig.setTriggerQuantity(new BerEnum(0));

            ReportConfig.ReportParams reportParams = new ReportConfig.ReportParams();
            reportParams.setHysteresis(new Hysteresis(0));

            ReportConfig.ReportParams.Params params = new ReportConfig.ReportParams.Params();
            PerParam perParam = new PerParam();
            perParam.setReportIntervalMs(new BerEnum(120));

            params.setPerParam(perParam);//.setReportIntervalMs(new BerEnum(0)));
            reportParams.setParams(params);//.setPerParam(new PerParam()));;

            reportParams.setTimetotrigger(new TimeToTrigger(0));

            reportConfig.setReportParams(reportParams);
            reportConfigs.getReportConfig().add(reportConfig);
            //log.info("\n********* "+reportConfigs.toString()+"\n");
            // construct a rx sig meas conf packet
            XrancPdu xrancPdu = RRCMeasConfig.constructPacket(
                    primary.getEcgi(),
                    ue.getCrnti(),
                    measObjects,
                    reportConfigs,
                    measIds,
                    xranConfig.getRxSignalInterval()
            );
            ue.setMeasConfig(xrancPdu.getBody().getRRCMeasConfig());
            //log.info("\nSending : " + xrancPdu.toString() +"\n");
            ctx.writeAndFlush(getSctpMessage(xrancPdu));
        });
    }

    public void panic(XrancPdu recvPdu) {
        throw new IllegalArgumentException("Received illegal packet: " + recvPdu.toString());
    }

    /**
     * Internal device listener.
     */
    class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            log.info("Device Event {}", event);
            switch (event.type()) {
                case DEVICE_ADDED: {
                    try {
                        ECGI ecgi = decodeDeviceId(event.subject().id());
                        // move this to a routine service
                        xranStore.getCell(ecgi).ifPresent(cell -> {
                            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                            executor.scheduleAtFixedRate(
                                    () -> {

                                        /*Shubham : TODO - Not sure whether this is correct place to write this code
                                         : FixMe - Intention is to create a secondary link between existing UE and
                                         : newly added Cell.
                                        */
                                        /* xranStore.getUeNodes().forEach( ue -> {
                                             RnibLink secondaryLink = new RnibLink(cell,ue);
                                             secondaryLink.setType(RnibLink.Type.NON_SERVING);

                                         });*/

                                        // populate config if it does not exist
                                        if (!cell.getOptConf().isPresent()) {
                                            // if channel context is present then send the config request
                                            xranStore.getCtx(ecgi).ifPresent(ctx -> {
                                                XrancPdu xrancPdu = CellConfigRequest.constructPacket(ecgi);
                                                ctx.writeAndFlush(getSctpMessage(xrancPdu));
                                            });
                                        } else {
                                            // iterate through all UEs
                                            xranStore.getUeNodes().forEach(ue -> xranStore.getPrimaryCell(ue)
                                                    .ifPresent(primaryCell -> populateMeasConfig(primaryCell, ue)));

                                            // send l2 meas interval
                                            xranStore.getCtx(ecgi).ifPresent(ctx -> {
                                                // 17 apiid pass
                                                XrancPdu xrancPdu = L2MeasConfig
                                                        .constructPacket(ecgi, xranConfig.getL2MeasInterval());

                                                cell.getMeasConfig().setL2MeasConfig(xrancPdu.getBody()
                                                                                             .getL2MeasConfig());
                                                //log.info("\nSending : " + xrancPdu.toString() +"\n");
                                                SctpMessage sctpMessage = getSctpMessage(xrancPdu);
                                                ctx.writeAndFlush(sctpMessage);

                                                executor.shutdown();
                                            });
                                        }
                                    },
                                    0,
                                    xranConfig.getConfigRequestInterval(),
                                    TimeUnit.SECONDS
                            );
                        });
                    } catch (IOException e) {
                        log.error(ExceptionUtils.getFullStackTrace(e));
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    /**
     * Internal host listener.
     */
    class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            //log.info("Host Event {}", event);
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_MOVED: {
                    xranStore.getUe(hostIdtoUEId(event.subject().id())).ifPresent(ue -> xranStore.getPrimaryCell(ue)
                            .ifPresent(cell -> {
                                ue.setMeasConfig(null);

                                // move this to a routine service
                                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                                executor.scheduleAtFixedRate(
                                        () -> {
                                            if (cell.getVersion() >= 3) {
                                                if (!Optional.ofNullable(ue.getCapability()).isPresent()) {
                                                    xranStore.getCtx(cell.getEcgi()).ifPresent(ctx -> {
                                                        XrancPdu xrancPdu = UECapabilityEnquiry.constructPacket(
                                                                cell.getEcgi(),
                                                                ue.getCrnti());
                                                        //log.info("\nSending : " + xrancPdu.toString() +"\n");
                                                        ctx.writeAndFlush(getSctpMessage(xrancPdu));
                                                    });
                                                } else {
                                                    executor.shutdown();
                                                }
                                            } else {
                                                executor.shutdown();
                                            }

                                        },
                                        0,
                                        xranConfig.getConfigRequestInterval(),
                                        TimeUnit.MILLISECONDS
                                );
                                if (ue.getMeasConfig() == null) {
                                    populateMeasConfig(cell, ue);
                                }
                            }));
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    /**
     * Internal xran device agent.
     */
    public class InternalXranDeviceAgent implements XranDeviceAgent {

        private final Logger log = LoggerFactory.getLogger(InternalXranDeviceAgent.class);

        @Override
        public boolean addConnectedCell(String host, ChannelHandlerContext ctx) {
            log.info("addConnectedCell: {}", host);
            // check configuration if the cell is inside the accepted list
            return Optional.ofNullable(legitCells.get(IpAddress.valueOf(host))).map(ecgi -> {
                log.info("Device exists in configuration; registering...");
                // check if cell is not already registered
                if (!xranStore.getCell(ecgi).isPresent()) {
                    RnibCell storeCell = new RnibCell();
                    storeCell.setEcgi(ecgi);
                    xranStore.storeCtx(storeCell, ctx);
                    xranDeviceListeners.forEach(l -> l.deviceAdded(storeCell));
                    return true;
                }
                return false;
            }).orElseGet(() -> {
                             log.error("Device is not a legit source; ignoring...");
                             ctx.close();
                             return false;
                         }
            );
        }

        @Override
        public boolean removeConnectedCell(String host) {
            log.info("removeConnectedCell: {}", host);
            ECGI ecgi = legitCells.get(IpAddress.valueOf(host));

            xranStore.getLinks(ecgi).forEach(rnibLink -> {
                rnibLink.getLinkId().getUe().setState(RnibUe.State.IDLE);
                restartTimer(rnibLink.getLinkId().getUe());
                xranStore.removeLink(rnibLink.getLinkId());
            });

            if (xranStore.removeCell(ecgi)) {
                xranDeviceListeners.forEach(l -> l.deviceRemoved(deviceId(uri(ecgi))));
                return true;
            }
            return false;
        }
    }

    /**
     * Internal xran host agent.
     */
    public class InternalXranHostAgent implements XranHostAgent {

        @Override
        public boolean addConnectedHost(RnibUe ue, RnibCell cell, ChannelHandlerContext ctx) {
            log.info("addConnectedHost: {}", ue);
            if (ue.getId() != null && xranStore.getUe(ue.getId()).isPresent()) {
                xranStore.putPrimaryLink(cell, ue);

                Set<ECGI> ecgiSet = Sets.newConcurrentHashSet();

                xranStore.getLinks(ue.getId())
                        .stream()
                        .filter(l -> l.getType().equals(RnibLink.Type.SERVING_PRIMARY))
                        .findFirst()
                        .ifPresent(l -> ecgiSet.add(l.getLinkId().getEcgi()));

                xranHostListeners.forEach(l -> l.hostAdded(ue, ecgiSet));
                return true;
            } else {
                xranStore.storeUe(cell, ue);
                xranStore.putPrimaryLink(cell, ue);

                Set<ECGI> ecgiSet = Sets.newConcurrentHashSet();
                ecgiSet.add(cell.getEcgi());
                xranHostListeners.forEach(l -> l.hostAdded(ue, ecgiSet));
                return true;
            }

        }

        @Override
        public boolean removeConnectedHost(RnibUe ue) {
            log.info("removeConnectedHost: {}", ue);
            xranStore.getLinks(ue.getId()).forEach(rnibLink -> xranStore.removeLink(rnibLink.getLinkId()));
            if (xranStore.removeUe(ue.getId())) {
                xranHostListeners.forEach(l -> l.hostRemoved(ue.getHostId()));
                return true;
            }
            return false;
        }
    }

    public class InternalXranPacketAgent implements XranPacketProcessor {
        @Override
        public void handlePacket(XrancPdu recvPdu, ChannelHandlerContext ctx)
                throws IOException, InterruptedException {
            int apiID = recvPdu.getHdr().getApiId().intValue();
            log.debug("******* Received message: { }", recvPdu.toString());

            switch (apiID) {
                // Cell Config Report
                case 1: {
                    CellConfigReport report = recvPdu.getBody().getCellConfigReport();
                    handleCellconfigreport(report, recvPdu.getHdr().getVer().toString());
                    break;
                }
                // UE Admission Request
                case 2: {
                    UEAdmissionRequest ueAdmissionRequest = recvPdu.getBody().getUEAdmissionRequest();
                    handleUeadmissionRequest(ueAdmissionRequest, ctx);
                    break;
                }
                // UE Admission Status
                case 4: {
                    UEAdmissionStatus ueAdmissionStatus = recvPdu.getBody().getUEAdmissionStatus();
                    handleAdmissionStatus(ueAdmissionStatus, ctx);
                    break;
                }
                // UE Context Update
                case 5: {
                    UEContextUpdate ueContextUpdate = recvPdu.getBody().getUEContextUpdate();
                    handleUeContextUpdate(ueContextUpdate, ctx);
                    break;
                }
                // UE Reconfig Ind
                case 6: {
                    UEReconfigInd ueReconfigInd = recvPdu.getBody().getUEReconfigInd();
                    handleUeReconfigInd(ueReconfigInd);
                    break;
                }
                // UE Release Ind
                case 7: {
                    // If xRANc wants to deactivate UE, we pass UEReleaseInd from xRANc to eNB.
                    UEReleaseInd ueReleaseInd = recvPdu.getBody().getUEReleaseInd();
                    handleUeReleaseInd(ueReleaseInd);
                    break;
                }
                // Bearer Admission Request
                case 8: {
                    BearerAdmissionRequest bearerAdmissionRequest = recvPdu.getBody().getBearerAdmissionRequest();
                    handleBearerAdmissionRequest(bearerAdmissionRequest, ctx);
                    break;
                }
                // Bearer Admission Status
                case 10: {
                    BearerAdmissionStatus bearerAdmissionStatus = recvPdu.getBody().getBearerAdmissionStatus();
                    // TODO: implement
                    break;
                }
                // Bearer Release Ind
                case 11: {

                    BearerReleaseInd bearerReleaseInd = recvPdu.getBody().getBearerReleaseInd();
                    //hostManager.updateHost(xranStore.getUeForCRNTI(bearerReleaseInd.getCrnti()).get().getHostId());
                    handleBearerReleaseInd(bearerReleaseInd);
                    break;
                }
                // HO Failure
                case 13: {
                    HOFailure hoFailure = recvPdu.getBody().getHOFailure();
                    handleHoFailure(hoFailure);
                    break;
                }
                // HO Complete
                case 14: {
                    HOComplete hoComplete = recvPdu.getBody().getHOComplete();
                    handleHoComplete(hoComplete, ctx);
                    break;
                }

                // RX Sig Meas Report
                case 15: {
                    log.info("\n*****RxSigMeasConfig****\n");
                    RXSigMeasConfig rxSigMeasConfig = recvPdu.getBody().getRXSigMeasConfig();
                }
                case 16: {
                    RXSigMeasReport rxSigMeasReport = recvPdu.getBody().getRXSigMeasReport();
                    handleRxSigMeasReport(rxSigMeasReport);
                    break;
                }
                // This is implemented in eNB Simulator
                case 17: {
                    // Api Id will send l2MeasConfig Report.
                    break;
                }
                // Radio Meas Report per UE
                case 18: {



                    //log.info("Calling handleRadioMeasReportPerUE");
                    RadioMeasReportPerUE radioMeasReportPerUE = recvPdu.getBody().getRadioMeasReportPerUE();
                    handleRadioMeasReportPerUe_storage(radioMeasReportPerUE);
//                    for (int i = 0; i<1000; i++) {
//                        Thread.sleep(500);
//                        algorithm.place(xranStore.getUeForCRNTI(radioMeasReportPerUE.
//                                getCrnti()).get().getHostId(),new Double(i),new Double(0));
//                    }
                    handleCqiBasedHandOver();
                    break;
                }
                // Radio Meas Report per Cell
                case 19: {
                    RadioMeasReportPerCell radioMeasReportPerCell = recvPdu.getBody().getRadioMeasReportPerCell();
                    handleRadioMeasReportPerCell(radioMeasReportPerCell);
                    break;
                }
                // Sched Meas Report per UE
                case 20: {
                    SchedMeasReportPerUE schedMeasReportPerUE = recvPdu.getBody().getSchedMeasReportPerUE();
                    handleSchedMeasReportPerUe(schedMeasReportPerUE);
                    break;
                }
                // Sched Meas Report per Cell
                case 21: {
                    SchedMeasReportPerCell schedMeasReportPerCell = recvPdu.getBody().getSchedMeasReportPerCell();
                    handleSchedMeasReportPerCell(schedMeasReportPerCell);
                    break;
                }
                // PDCP Meas Report per UE
                case 22: {
                    PDCPMeasReportPerUe pdcpMeasReportPerUe = recvPdu.getBody().getPDCPMeasReportPerUe();
                    handlePdcpMeasReportPerUe(pdcpMeasReportPerUe);
                    break;
                }

                case 23: {
                    //XICIC
                }

                // UE Capability Enquiry
                case 24: {


                }
                // UE Capability Info
                case 25: {
                    UECapabilityInfo capabilityInfo = recvPdu.getBody().getUECapabilityInfo();
                    handleCapabilityInfo(capabilityInfo);
                    break;
                }
                case 26: {
                    ScellAdd scellAdd;
                }
                // Scell Add Status
                case 27: {
                    ScellAddStatus scellAddStatus = recvPdu.getBody().getScellAddStatus();
                    handleScellAddStatus(scellAddStatus);
                    break;
                }
                case 28: {
                    ScellDelete scellDelete = recvPdu.getBody().getScellDelete();
                    log.info("\n*** Scell add from xran ***\n");
                }
                case 29: {
                    RRMConfig rrmConfig;
                    // TODO: implement
                    break;
                }
                case 30: {
                    RRMConfigStatus rrmConfigStatus = recvPdu.getBody().getRRMConfigStatus();
                    log.info("\n!!!!! Case 30 calling handleRrmConfigStatus");
                    handleRrmConfigStatus(rrmConfigStatus);
                    break;
                }

                case 31: {
                    SeNBAdd seNBAdd;
                    // TODO: implement
                    break;
                }

                case 32: {
                    SeNBAddStatus seNBAddStatus;
                    // TODO: implement
                    break;
                }

                case 33: {
                    SeNBDelete seNBDelete;
                    // TODO: implement
                    break;
                }

                case 34: {
                    TrafficSplitConfig trafficSplitConfig = recvPdu.getBody().getTrafficSplitConfig();
                    handleTrafficSplitConfig(trafficSplitConfig);
                    break;
                }

                case 35: {
                    HOCause hoCause;
                    // TODO: implement
                    break;
                }

                case 36: {
                    RRCMeasConfig rrcMeasConfig = recvPdu.getBody().getRRCMeasConfig();
                    // TODO: implement
                    break;
                }
                //-------------------------------------------------------------------------------------------------
                // RRM Config Status
             /*   case 28: {
                    // Decode RRMConfig Status
                    RRMConfigStatus rrmConfigStatus = recvPdu.getBody().getRRMConfigStatus();
                    log.info("\n!!!!! Case 28 calling handleRrmConfigStatus");
                    handleRrmConfigStatus(rrmConfigStatus);
                    break;
                }
                // SeNB Add
                case 29: {
                    // TODO: implement
                    break;
                }
                // SeNB Add Status
                case 30: {
                    // TODO: implement
                    break;
                }
                // SeNB Delete
                case 31: {
                    // TODO: implement
                    break;
                }
                // Traffic Split Config
                case 32: {
                    TrafficSplitConfig trafficSplitConfig = recvPdu.getBody().getTrafficSplitConfig();
                    handleTrafficSplitConfig(trafficSplitConfig);
                    break;
                }
                // HO Cause
                case 33: {
                    // TODO: implement
                    break;
                }
                case 34: {
                    // TODO: implement
                    break;
                }*/
                // Cell Config Request
                case 0:
                    // UE Admission Response
                case 3:
                    // Bearer Admission Response
                case 9:
                    // HO Request
                case 12:
                    // L2 Meas Config
                    //case 16:
                    // Scell Add
                    //case 24:
                    // Scell Delete
                    //case 26:
                    // RRM Config
                    //case 27:
                    log.info("\n!!!!! Calling panic");
                default: {
                    panic(recvPdu);
                }
            }

        }

        /**
         * Handle Cellconfigreport.
         *
         * @param report  CellConfigReport
         * @param version String version ID
         */
        private void handleCellconfigreport(CellConfigReport report, String version) {
            ECGI ecgi = report.getEcgi();

            xranStore.getCell(ecgi).ifPresent(cell -> {
                cell.setVersion(version);
                cell.setConf(report);
                xranStore.storePciArfcn(cell);
            });
        }

        /**
         * Handle Ueadmissionrequest.
         *
         * @param ueAdmissionRequest UEAdmissionRequest
         * @param ctx                ChannelHandlerContext
         * @throws IOException IO Exception
         */
        private void handleUeadmissionRequest(UEAdmissionRequest ueAdmissionRequest, ChannelHandlerContext ctx)
                throws IOException {
            ECGI ecgi = ueAdmissionRequest.getEcgi();

            xranStore.getCell(ecgi).map(c -> {
                CRNTI crnti = ueAdmissionRequest.getCrnti();
                XrancPdu sendPdu = UEAdmissionResponse.constructPacket(ecgi, crnti, xranConfig.admissionFlag());
                //log.info("\nSending : " + sendPdu.toString() +"\n");
                ctx.writeAndFlush(getSctpMessage(sendPdu));
                return 1;
            }).orElseGet(() -> {
                log.warn("Could not find ECGI in registered cells: {}", ecgi);
                return 0;
            });
        }

        /**
         * Handle UEAdmissionStatus.
         *
         * @param ueAdmissionStatus UEAdmissionStatus
         * @param ctx               ChannelHandlerContext
         */
        private void handleAdmissionStatus(UEAdmissionStatus ueAdmissionStatus, ChannelHandlerContext ctx) {
            log.info("\n##### handleAdmissionStatus #######");
            xranStore.getUe(ueAdmissionStatus.getEcgi(), ueAdmissionStatus.getCrnti()).ifPresent(ue -> {
                if (ueAdmissionStatus.getAdmEstStatus().value.intValue() == 0) {
                    ue.setState(RnibUe.State.ACTIVE);
                } else {
                    ue.setState(RnibUe.State.IDLE);
                }
            });

            if (ueAdmissionStatus.getAdmEstStatus().value.intValue() == 0) {
                EcgiCrntiPair ecgiCrntiPair = EcgiCrntiPair
                        .valueOf(ueAdmissionStatus.getEcgi(), ueAdmissionStatus.getCrnti());
                contextUpdateMap.compute(ecgiCrntiPair, (k, v) -> {
                    if (v == null) {
                        v = new ContextUpdateHandler();
                    }
                    if (v.setAdmissionStatus(ueAdmissionStatus)) {
                        handlePairedPackets(v.getContextUpdate(), ctx, false);
                        v.reset();
                    }
                    return v;
                });
            }
        }

        /**
         * Handle UEContextUpdate.
         *
         * @param ueContextUpdate UEContextUpdate
         * @param ctx             ChannelHandlerContext
         */
        private void handleUeContextUpdate(UEContextUpdate ueContextUpdate, ChannelHandlerContext ctx) {
            EcgiCrntiPair ecgiCrntiPair = EcgiCrntiPair
                    .valueOf(ueContextUpdate.getEcgi(), ueContextUpdate.getCrnti());
            log.info("\n########## In handleUeContextUpdate() #########\n");
            contextUpdateMap.compute(ecgiCrntiPair, (k, v) -> {
                if (v == null) {
                    v = new ContextUpdateHandler();
                }
                if (v.setContextUpdate(ueContextUpdate)) {
                    HOComplete hoComplete = v.getHoComplete();
                    handlePairedPackets(ueContextUpdate, ctx, hoComplete != null);
                    if (hoComplete != null) {
                        try {
                            hoMap.get(hoComplete.getEcgiS()).put("Hand Over Completed");
                        } catch (InterruptedException e) {
                            log.error(ExceptionUtils.getFullStackTrace(e));
                        } finally {
                            hoMap.remove(hoComplete.getEcgiS());
                        }
                    }
                    v.reset();
                }
                return v;
            });
        }

        /**
         * Handle UEReconfigInd.
         *
         * @param ueReconfigInd UEReconfigInd
         */
        private void handleUeReconfigInd(UEReconfigInd ueReconfigInd) {
            Optional<RnibUe> ue = xranStore.getUe(ueReconfigInd.getEcgi(), ueReconfigInd.getCrntiOld());
            Optional<RnibCell> cell = xranStore.getCell(ueReconfigInd.getEcgi());

            if (ue.isPresent() && cell.isPresent()) {
                ue.get().setCrnti(ueReconfigInd.getCrntiNew());
                xranStore.storeCrnti(cell.get(), ue.get());
            } else {
                log.warn("Could not find UE with this CRNTI: {}", ueReconfigInd.getCrntiOld());
            }
        }

        /**
         * Handle UEReleaseInd.
         *
         * @param ueReleaseInd UEReleaseInd
         */
        private void handleUeReleaseInd(UEReleaseInd ueReleaseInd) {
            ECGI ecgi = ueReleaseInd.getEcgi();
            CRNTI crnti = ueReleaseInd.getCrnti();

            // Check if there is an ongoing handoff and only remove if ue is not part of the handoff.
            Long peek = ueIdQueue.peek();
            if (peek != null) {
                EcgiCrntiPair ecgiCrntiPair = xranStore.getCrnti().inverse().get(peek);
                if (ecgiCrntiPair != null && ecgiCrntiPair.equals(EcgiCrntiPair.valueOf(ecgi, crnti))) {
                    return;
                }
            }

            xranStore.getUe(ecgi, crnti).ifPresent(ue -> {
                ue.setState(RnibUe.State.IDLE);
                restartTimer(ue);
            });
        }

        /**
         * Handle BearerAdmissionRequest.
         *
         * @param bearerAdmissionRequest BearerAdmissionRequest
         * @param ctx                    ChannelHandlerContext
         */
        private void handleBearerAdmissionRequest(BearerAdmissionRequest bearerAdmissionRequest,
                                                  ChannelHandlerContext ctx) {
            ECGI ecgi = bearerAdmissionRequest.getEcgi();
            CRNTI crnti = bearerAdmissionRequest.getCrnti();
            ERABParams erabParams = bearerAdmissionRequest.getErabParams();
            xranStore.getLink(ecgi, crnti).ifPresent(link -> link.setBearerParameters(erabParams));

            BerInteger numErabs = bearerAdmissionRequest.getNumErabs();
            // Encode and send Bearer Admission Response
            XrancPdu sendPdu = BearerAdmissionResponse
                    .constructPacket(ecgi, crnti, erabParams, numErabs, xranConfig.bearerFlag());
            //log.info("\nSending : " + sendPdu.toString() +"\n");
            ctx.writeAndFlush(getSctpMessage(sendPdu));
        }

        /**
         * Handle BearerReleaseInd.
         *
         * @param bearerReleaseInd bearer release ind
         */
        private void handleBearerReleaseInd(BearerReleaseInd bearerReleaseInd) {
            ECGI ecgi = bearerReleaseInd.getEcgi();
            CRNTI crnti = bearerReleaseInd.getCrnti();

            xranStore.getLink(ecgi, crnti).ifPresent(link -> {
                List<ERABID> erabidsRelease = bearerReleaseInd.getErabIds().getERABID();
                List<ERABParamsItem> erabParamsItem = link.getBearerParameters().getERABParamsItem();

                List<ERABParamsItem> unreleased = erabParamsItem
                        .stream()
                        .filter(item -> {
                            Optional<ERABID> any = erabidsRelease.stream()
                                    .filter(id -> id.equals(item.getId())).findAny();
                            return !any.isPresent();
                        }).collect(Collectors.toList());
                link.getBearerParameters().getERABParamsItem().clear();
                link.getBearerParameters().getERABParamsItem().addAll(new ArrayList<>(unreleased));
            });
        }

        /**
         * Handle HOFailure.
         *
         * @param hoFailure HOFailure
         * @throws InterruptedException ueIdQueue interruption
         */
        private void handleHoFailure(HOFailure hoFailure) throws InterruptedException {
            try {
                hoMap.get(hoFailure.getEcgi())
                        .put("Hand Over Failed with cause: " + hoFailure.getCause());
            } catch (InterruptedException e) {
                log.error(ExceptionUtils.getFullStackTrace(e));
            } finally {
                hoMap.remove(hoFailure.getEcgi());
                ueIdQueue.take();
            }
        }

        /**
         * Handle HOComplete.
         *
         * @param hoComplete HOComplete
         * @param ctx        ChannelHandlerContext
         */
        private void handleHoComplete(HOComplete hoComplete, ChannelHandlerContext ctx) {
            EcgiCrntiPair ecgiCrntiPair = EcgiCrntiPair.valueOf(hoComplete.getEcgiT(),
                                                                hoComplete.getCrntiNew());
            log.info("\n####### In handleHoComplete() ######\n");
            contextUpdateMap.compute(ecgiCrntiPair, (k, v) -> {
                if (v == null) {
                    v = new ContextUpdateHandler();
                }
                if (v.setHoComplete(hoComplete)) {
                    handlePairedPackets(v.getContextUpdate(), ctx, true);

                    try {
                        hoMap.get(hoComplete.getEcgiS()).put("Hand Over Completed");
                    } catch (InterruptedException e) {
                        log.error(ExceptionUtils.getFullStackTrace(e));
                    } finally {
                        hoMap.remove(hoComplete.getEcgiS());
                    }
                    v.reset();
                }
                return v;
            });
        }

        /**
         * Handle RXSigMeasReport.
         *
         * @param rxSigMeasReport RXSigMeasReport
         */
        private void handleRxSigMeasReport(RXSigMeasReport rxSigMeasReport) {
            rxSigMeasReport.getCellMeasReports().getSEQUENCEOF().forEach(
                    cellMeasReport -> cellMeasReport.getRXSigReport().forEach(
                            rxSigReport -> {
                                rxSigMeasReport.getCrnti().getCRNTI().forEach(
                                        crnti -> xranStore.getUe(rxSigMeasReport.getEcgi(), crnti).ifPresent(ue -> {
                                            Long ueId = ue.getId();
                                            xranStore.getCell(rxSigReport.getPciArfcn()).ifPresent(cell -> {
                                                ECGI ecgi = cell.getEcgi();

                                                Optional<RnibLink> link = xranStore.getLink(ecgi, ueId);
                                                if (!link.isPresent()) {
                                                    log.warn("Could not find link between: {}-{} " +
                                                                     "| Creating non-serving link..",
                                                             ecgi, ueId);
                                                    link = xranStore.putNonServingLink(cell, ueId);
                                                }

                                                if (link.isPresent()) {

                                                    link.get().getMeasurements().setRxSigReport(
                                                        new RnibLink.Measurements.RXSigReport(
                                                                rxSigReport.getRsrq().longValue(),
                                                                rxSigReport.getRsrp().longValue(),
                                                                rxSigMeasReport.getCellMeasReports()
                                                        )
                                                    );
                                                }
                                            });
                                        })
                                );
                            }
                    )
            );
            //log.info("********* ecCrntiMap = " + xranStore.getSetofUEs().toString());
        }
        private EcgiCrntiPair keyForPair;
        private void handleRadioMeasReportPerUe_storage(RadioMeasReportPerUE radioMeasReportPerUE) {
            byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xCD};
            CRNTI checkcrnti = new CRNTI(bytes, 16);       //xranStore.getUe(radioMeasReportPerUE.getEcgi(), radioMeasReportPerUE.getCrnti()).ifPresent(ue -> {

            xranStore.getUeForCRNTI(radioMeasReportPerUE.getCrnti()).ifPresent(ue ->{
                //log.info("#### inside handleRadioMeasReport with ue = "+ ue.toString());
                if(ue.getCrnti().equals(checkcrnti)){
                    indexofRRPSC++;
                }
                Long ueId = ue.getId();
                //This will give us the UE
                List<RadioRepPerServCell> servCells = radioMeasReportPerUE.getRadioReportServCells()
                        .getRadioRepPerServCell(); // This will give us the cells out of which we need to
                //log.info("**** We have servCells, "+ servCells.toString() +" iterating them ");

                servCells.forEach(servCell -> xranStore.getCell(servCell.getEcgi())
                        .ifPresent(cell -> {
                            //log.info("Getting the link for " + cell.getEcgi() + " and " + ueId);
                            xranStore.getLink(cell.getEcgi(), ueId)
                                    .ifPresent(link -> {
                                                   RadioRepPerServCell.CqiHist cqiHist = servCell.getCqiHist();
                                                   final double[] values = {0, 0, 0};
                                                   final int[] i = {1};
                                                   cqiHist.getBerInteger().forEach(value -> {
                                                                                       values[0] = Math.max(values[0], value.intValue());// gets the maximum of the array
                                                                                       values[1] += i[0] * value.intValue();// (Place value) * (value)
                                                                                       values[2] += value.intValue();// Sums the array
                                                                                       i[0]++;
                                                                                   }
                                                   );
                                                  // log.info("Link is Present between " + cell.toString() + "and " + ueId);
                                                   //cQiMap.put(ueId,);
                                                   link.getMeasurements().setRadioReport(
                                                           new RnibLink.Measurements.RadioReport(
                                                                   new RnibLink.Measurements.RadioReport.Cqi(
                                                                           cqiHist,
                                                                           values[0],
                                                                           values[1] / values[0]
                                                                   ),
                                                                   servCell.getRiHist(),
                                                                   servCell.getPucchSinrHist(),
                                                                   servCell.getPuschSinrHist()

                                                           )
                                                   );
                                                   //log.info("report = "+link.getMeasurements().getRadioReport().toString());

                                        if(link.getLinkId().getUe().getCrnti().equals(checkcrnti)){
                                            keyForPair = EcgiCrntiPair.valueOf(link.getLinkId().getEcgi(),link.getLinkId().getUe().getCrnti());
                                            if ((DefaultXranStore.typeBiMap.get(keyForPair)) != null && !(DefaultXranStore.typeBiMap.get(keyForPair).equals(link.getType())))
                                            {
                                                DefaultXranStore.typeBiMap.replace(keyForPair, link.getType());
                                                //log.info("Ankit ........ typeBiMap " + DefaultXranStore.typeBiMap.toString());
                                            }
                                            else{
                                                DefaultXranStore.typeBiMap.put(keyForPair,link.getType());
                                            }
                                            DefaultXranStore.avgBiMap.put(keyForPair,values[2]);
                                            //log.info("Ankit ........ avgBiMap " + DefaultXranStore.avgBiMap.toString());
                                        }
                                    }
                                    );
                        })
                );
            });
        }

        /**
         * Handle RadioMeasReportPerUE.
         *
         * @param radioMeasReportPerUE RadioMeasReportPerUE
         */
        /*private void handleRadioMeasReportPerUe(RadioMeasReportPerUE radioMeasReportPerUE) {
            xranStore.getUe(radioMeasReportPerUE.getEcgi(), radioMeasReportPerUE.getCrnti()).ifPresent(ue -> {
                Long ueId = ue.getId();
                //This will give us the UE
                List<RadioRepPerServCell> servCells = radioMeasReportPerUE.getRadioReportServCells()
                        .getRadioRepPerServCell(); // This will give us the cells
                log.info("**** We have servCells, iterating them ");
                servCells.forEach(servCell -> xranStore.getCell(servCell.getEcgi())
                        .ifPresent(cell -> xranStore.getLink(cell.getEcgi(), ueId)
                                .ifPresent(link -> {
                                            RadioRepPerServCell.
                                            Hist cqiHist = servCell.getCqiHist();
                                            log.info(" cqiHist = "+ cqiHist.toString());
                                            final double[] values = {0, 0, 0};
                                            final int[] i = {1};
                                            // Code walk through from here what following code is doing
                                            // Create a map of map which has a store of values of cqi of primary link as well as the
                                            // cqi of sec link.
                                            cqiHist.getBerInteger().forEach(value -> {
                                                        values[0] = Math.max(values[0], value.intValue());// gets the maximum of the array
                                                        values[1] += i[0] * value.intValue();// (Place value) * (value)
                                                        values[2] += value.intValue();// Sums the array
                                                        i[0]++;
                                                        log.info("values[0] = "+values[0] + "values[1] = "+values[1] + "values[2] = "+values[2] +" and i = "+i[0]);
                                                        log.info("mean = " + values[1]/values[0]);
                                                    }
                                            );
                                            log.info(" cqiHist = "+ cqiHist.toString());
                                            link.getMeasurements().setRadioReport(
                                                    new RnibLink.Measurements.RadioReport(
                                                            new RnibLink.Measurements.RadioReport.Cqi(
                                                                    cqiHist,
                                                                    values[0],
                                                                    values[1] / values[0]
                                                            ),
                                                            servCell.getRiHist(),
                                                            servCell.getPucchSinrHist(),
                                                            servCell.getPuschSinrHist()

                                                    )
                                            );
                                        }
                                )
                        )
                );
            });
        }*/
        // This is a map of map which will have the ueId and RnibCell along with cqihist of Rnibcell
        protected Map<Long, Map<RnibCell, RadioRepPerServCell.CqiHist>> cQiMap = new HashMap();



        /**
         * Handle RadioMeasReportPerCell.
         *
         * @param radioMeasReportPerCell RadioMeasReportPerCell
         */
        private void handleRadioMeasReportPerCell(RadioMeasReportPerCell radioMeasReportPerCell) {
            xranStore.getCell(radioMeasReportPerCell.getEcgi()).ifPresent(
                    cell -> cell.getMeasurements().setUlInterferenceMeasurement(
                            new RnibCell.Measurements.ULInterferenceMeasurement(
                                    radioMeasReportPerCell.getPuschIntfPowerHist(),
                                    radioMeasReportPerCell.getPucchIntfPowerHist()
                            )
                    )
            );
        }

        /**
         * Handle SchedMeasReportPerUE.
         *
         * @param schedMeasReportPerUE SchedMeasReportPerUE
         */
        private void handleSchedMeasReportPerUe(SchedMeasReportPerUE schedMeasReportPerUE) {
            xranStore.getUe(schedMeasReportPerUE.getEcgi(), schedMeasReportPerUE.getCrnti()).ifPresent(ue -> {
                Long ueId = ue.getId();

                List<SchedMeasRepPerServCell> servCells = schedMeasReportPerUE.getSchedReportServCells()
                        .getSchedMeasRepPerServCell();

                servCells.forEach(servCell -> xranStore.getCell(servCell.getEcgi())
                        .ifPresent(cell -> xranStore.getLink(cell.getEcgi(), ueId)
                                .ifPresent(link -> link.getMeasurements().setSchedMeasReport(
                                        new RnibLink.Measurements.SchedMeasReport(
                                                servCell.getQciVals(),
                                                new RnibLink.Measurements.SchedMeasReport.ResourceUsage(
                                                        servCell.getPrbUsage().getPrbUsageDl(),
                                                        servCell.getPrbUsage().getPrbUsageUl()
                                                ),
                                                new RnibLink.Measurements.SchedMeasReport.Mcs(
                                                        servCell.getMcsDl(),
                                                        servCell.getMcsUl()
                                                ),
                                                new RnibLink.Measurements.SchedMeasReport.NumSchedTtis(
                                                        servCell.getNumSchedTtisDl(),
                                                        servCell.getNumSchedTtisUl()
                                                ),
                                                new RnibLink.Measurements.SchedMeasReport.DlRankStats(
                                                        servCell.getRankDl1(),
                                                        servCell.getRankDl2()
                                                )
                                        )
                                           )
                                )
                        )
                );
            });
        }

        /**
         * Handle SchedMeasReportPerCell.
         *
         * @param schedMeasReportPerCell SchedMeasReportPerCell
         */
        private void handleSchedMeasReportPerCell(SchedMeasReportPerCell schedMeasReportPerCell) {
            xranStore.getCell(schedMeasReportPerCell.getEcgi()).ifPresent(cell -> cell.getMeasurements().setPrbUsage(
                    new RnibCell.Measurements.PrbUsage(
                            schedMeasReportPerCell.getQciVals(),
                            schedMeasReportPerCell.getPrbUsagePcell(),
                            schedMeasReportPerCell.getPrbUsageScell()
                    )
            ));
        }

        /**
         * Handle PDCPMeasReportPerUe.
         *
         * @param pdcpMeasReportPerUe PDCPMeasReportPerUe
         */
        private void handlePdcpMeasReportPerUe(PDCPMeasReportPerUe pdcpMeasReportPerUe) {
            xranStore.getUe(pdcpMeasReportPerUe.getEcgi(), pdcpMeasReportPerUe.getCrnti()).ifPresent(ue -> {
                Long ueId = ue.getId();
                xranStore.getLink(pdcpMeasReportPerUe.getEcgi(), ueId).ifPresent(link ->
                                                                                         link.getMeasurements().setPdcpMeasReport(
                                                                                                 new RnibLink.Measurements.PdcpMeasReport(
                                                                                                         pdcpMeasReportPerUe.getQciVals(),
                                                                                                         new RnibLink.Measurements.PdcpMeasReport.PdcpThroughput(
                                                                                                                 pdcpMeasReportPerUe.getThroughputDl(),
                                                                                                                 pdcpMeasReportPerUe.getThroughputUl()
                                                                                                         ),
                                                                                                         new RnibLink.Measurements.PdcpMeasReport.DataVol(
                                                                                                                 pdcpMeasReportPerUe.getDataVolDl(),
                                                                                                                 pdcpMeasReportPerUe.getDataVolUl()
                                                                                                         ),
                                                                                                         pdcpMeasReportPerUe.getPktDelayDl(),
                                                                                                         pdcpMeasReportPerUe.getPktDiscardRateDl(),
                                                                                                         new RnibLink.Measurements.PdcpMeasReport.PktLossRate(
                                                                                                                 pdcpMeasReportPerUe.getPktLossRateDl(),
                                                                                                                 pdcpMeasReportPerUe.getPktLossRateUl()
                                                                                                         )
                                                                                                 )
                                                                                         )
                );
            });
        }

        /**
         * Handle UECapabilityInfo.
         *
         * @param capabilityInfo UECapabilityInfo
         */
        private void handleCapabilityInfo(UECapabilityInfo capabilityInfo) {
            xranStore.getUe(capabilityInfo.getEcgi(), capabilityInfo.getCrnti())
                    .ifPresent(
                            ue -> ue.setCapability(capabilityInfo)
                    );
        }

        /**
         * Handle UECapabilityEnquiry.
         *
         * @param ueCapabilityEnquiry UECapabilityEnquiry
         * @param ctx                 ChannelHandlerContext
         */
        private void handleUecapabilityenquiry(UECapabilityEnquiry ueCapabilityEnquiry, ChannelHandlerContext ctx) {
            XrancPdu xrancPdu = UECapabilityEnquiry.constructPacket(ueCapabilityEnquiry.getEcgi(),
                                                                    ueCapabilityEnquiry.getCrnti());
            //log.info("\nSending : " + xrancPdu.toString() +"\n");
            ctx.writeAndFlush(getSctpMessage(xrancPdu));
        }

        /**
         * Handle ScellAddStatus.
         *
         * @param scellAddStatus ScellAddStatus
         */
        private void handleScellAddStatus(ScellAddStatus scellAddStatus) {
            xranStore.getUe(scellAddStatus.getEcgi(), scellAddStatus.getCrnti()).ifPresent(ue -> {
                Long ueId = ue.getId();
                try {
                    scellAddMap.get(scellAddStatus.getCrnti()).put("Scell's status: " +
                                                                           scellAddStatus.getStatus());
                    final int[] i = {0};
                    scellAddStatus.getScellsInd().getPCIARFCN().forEach(
                            pciarfcn -> {
                                if (scellAddStatus.getStatus().getBerEnum().get(i[0]).value.intValue() == 0) {
                                    xranStore.getCell(pciarfcn)
                                            .ifPresent(cell -> xranStore.getLink(cell.getEcgi(), ueId)
                                                    .ifPresent(link -> link.setType(RnibLink.Type.SERVING_SECONDARY_CA))
                                            );
                                }
                                i[0]++;
                            }
                    );

                } catch (InterruptedException e) {
                    log.error(ExceptionUtils.getFullStackTrace(e));
                } finally {
                    scellAddMap.remove(scellAddStatus.getCrnti());
                }
            });
        }

        /**
         * Handle RRMConfigStatus.
         *
         * @param rrmConfigStatus RRMConfigStatus
         */
        private void handleRrmConfigStatus(RRMConfigStatus rrmConfigStatus) {
            try {
                rrmCellMap.get(rrmConfigStatus.getEcgi())
                        .put("RRM Config's status: " + rrmConfigStatus.getStatus());
            } catch (InterruptedException e) {
                log.error(ExceptionUtils.getFullStackTrace(e));
            } finally {
                rrmCellMap.remove(rrmConfigStatus.getEcgi());
            }
        }

        /**
         * Handle TrafficSplitConfig.
         *
         * @param trafficSplitConfig TrafficSplitConfig
         */
        private void handleTrafficSplitConfig(TrafficSplitConfig trafficSplitConfig) {
            xranStore.getUe(trafficSplitConfig.getEcgi(), trafficSplitConfig.getCrnti()).ifPresent(ue -> {
                Long ueId = ue.getId();
                List<TrafficSplitPercentage> splitPercentages = trafficSplitConfig
                        .getTrafficSplitPercent().getTrafficSplitPercentage();

                splitPercentages.forEach(trafficSplitPercentage -> xranStore.getCell(trafficSplitPercentage.getEcgi())
                        .ifPresent(cell -> xranStore.getLink(cell.getEcgi(), ueId)
                                .ifPresent(link -> link.setTrafficPercent(trafficSplitPercentage))));
            });
        }

        /**
         * Handle context update depending if its handoff or not.
         *
         * @param contextUpdate context update packet
         * @param ctx           channel context for the CELL
         * @param handoff       true if we handle a Hand Off
         */
        private void handlePairedPackets(UEContextUpdate contextUpdate, ChannelHandlerContext ctx, boolean handoff) {
            //log.info("\n######### In handlePairedPackets () ########\n");
            xranStore.getCell(contextUpdate.getEcgi()).ifPresent(cell -> {
                                                                     Optional<RnibUe> optionalUe;
                                                                     if (handoff) {
                                                                         try {
                                                                             optionalUe = xranStore.getUe(ueIdQueue.take());
                                                                         } catch (InterruptedException e) {
                                                                             log.error(ExceptionUtils.getFullStackTrace(e));
                                                                             optionalUe = Optional.of(new RnibUe());
                                                                         }
                                                                     } else {
                                                                         optionalUe = Optional.of(new RnibUe());
                                                                     }

                                                                     optionalUe.ifPresent(ue -> {
                                                                         ue.getContextIds().setMmeS1apId(contextUpdate.getMMEUES1APID());
                                                                         ue.getContextIds().setEnbS1apId(contextUpdate.getENBUES1APID());
                                                                         ue.setCrnti(contextUpdate.getCrnti());
                                                                         hostAgent.addConnectedHost(ue, cell, ctx);
                                                                     });
                                                                 }
            );
        }
    }

    /**
     * Internal class for NetworkConfigListener.
     */
    class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            //log.info("\n#### in event() of InternalNetworkConfigListener with event = "+event.type()+" ###\n");
            switch (event.type()) {
                case CONFIG_REGISTERED:
                    break;
                case CONFIG_UNREGISTERED:
                    break;
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    if (event.configClass() == CONFIG_CLASS) {
                        handleConfigEvent(event.config());
                    }
                    break;
                case CONFIG_REMOVED:
                    break;
                default:
                    break;
            }
        }

        /**
         * Handle config event.
         *
         * @param configOptional config
         */
        private void handleConfigEvent(Optional<Config> configOptional) {
            //log.info("\n##### In handleConfigEvent ######\n");
            readCSV();
            configOptional.ifPresent(config -> {
                xranConfig = (XranConfig) config;
                northboundTimeout = xranConfig.getNorthBoundTimeout();
                legitCells.putAll(xranConfig.activeCellSet());
                legitCellsXY.putAll(xranConfig.activeCellxySet());
                xranServer.start(deviceAgent, hostAgent, packetAgent,
                                 xranConfig.getXrancIp(), xranConfig.getXrancPort());
            });
        }
    }

    Map<EcgiCrntiPair, Double> map;

    public void handleCqiBasedHandOver() {
        Double max;
       //log.info("####### ankit typeBiMap => " + DefaultXranStore.typeBiMap.toString());
       //log.info("####### ankit AvgBiMap => " + DefaultXranStore.avgBiMap.toString());

        RnibUe ueTemp = null;
        ECGI tempEcgi = null;
        for (EcgiCrntiPair e : DefaultXranStore.getTypeMap().keySet()) {
            //log.info("\n------index of RRPSC = "+ indexofRRPSC);
        //if (indexofRRPSC % 10 == 0) {
            if (DefaultXranStore.getTypeMap().get(e) == RnibLink.Type.SERVING_PRIMARY) {

                if (DefaultXranStore.getAvgMap().get(e) >= CQI_AVG) {
                    continue;
                } else {
                    map =
                            DefaultXranStore.getAvgMap().entrySet().stream().filter(u -> u.getKey()
                                    .getValue().equals(e.getValue())).filter(u -> DefaultXranStore.typeBiMap.get(u.getKey())
                                    .equals(RnibLink.Type.NON_SERVING)).filter(u -> u.getValue() >= CQI_AVG).collect(Collectors.toMap(u -> u.getKey(), u -> u.getValue()));
                    log.info("---- Ankit internal map " + map.toString());
                    if (!map.isEmpty()) {
                        max = map.entrySet().stream().max(Map.Entry.comparingByValue(Double::compareTo)).get().getValue();
                        log.info("Ankit: max  : " + max);
                        Double finalMax = max;

                        Optional<RnibUe> ue =
                                xranStore.getUeForCRNTI(map.entrySet().stream().filter(x -> x.getValue()
                                        .equals(finalMax)).findFirst().get().getKey().getValue());
                        if (ue.isPresent()) {
                            ueTemp = ue.get();
                            tempEcgi = map.entrySet().stream().max(Map.Entry.comparingByValue(Double::compareTo)).get().getKey().getKey();
                            //log.info("Ankit: UEid  : " + ue.get().getId());
                            //log.info("Ankit -------- ECGI  > " + map.entrySet().stream().max(Map.Entry.comparingByValue(Double::compareTo)).get().getKey().getKey().getEUTRANcellIdentifier());

                        }

                    }
                }
            }
        //}
        }
        if(hoFlag==0 && ueTemp != null && tempEcgi !=null) {
//            log.error("tempEcgi     -->" +tempEcgi.toString());
//            log.error("ueTemp       -->" + ueTemp.toString());

            synchronized (this){

                firePost(tempEcgi, ueTemp.getId());
                map.clear();
            }
        }

    }


    private void firePost(ECGI ecgiPost, Long ueId){
        try {
            String usernameColonPassword = "onos:rocks";
            String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
            HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("onos", "rocks");


            URL url = new URL("http://localhost:8181/onos/xran/links/" + ecgiPost.getEUTRANcellIdentifier()+","+ueId);


            String input = "{\"type\" : \"serving/primary\"}";



            try {
                Field methodsField = HttpURLConnection.class.getDeclaredField("methods");
                methodsField.setAccessible(true);
                // get the methods field modifiers
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                // bypass the "private" modifier
                modifiersField.setAccessible(true);

                // remove the "final" modifier
                modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

                /* valid HTTP methods */
                String[] methods = {
                        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH"
                };
                // set the new methods - including patch
                methodsField.set(null, methods);

            } catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
             conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", basicAuthPayload);
            //log.info("############ firePost : 1430 with url = "+ url.toString() + "input = " +input);
            DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
            writer.write(input.getBytes());
            writer.flush();

            log.info("\n######### closed the writer");

            try{
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonString = new StringBuilder();
            String line;
            //log.info("\n############# bufferedReader = " + br.toString());
            while ((line = br.readLine()) != null) {
                log.info("\n############# bufferedReader = " + jsonString);
                jsonString.append(line);
            }
            br.close();

            log.info("Ankit .......String "+jsonString.toString());}
            finally {
                conn.disconnect();
                writer.close();
            }
        } catch (Exception e) {
            //throw new RuntimeException(e.getMessage());
     }
/*} catch (MalformedURLException e) {
e.printStackTrace();
} catch (IOException e) {
e.printStackTrace();
}*/
    }
}
