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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.store.AbstractStore;
import org.onosproject.xran.XranService;
import org.onosproject.xran.XranStore;
import org.onosproject.xran.asn1lib.api.CRNTI;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.asn1lib.api.EUTRANCellIdentifier;
import org.onosproject.xran.asn1lib.api.PCIARFCN;
import org.onosproject.xran.impl.entities.RnibCell;
import org.onosproject.xran.impl.entities.RnibLink;
import org.onosproject.xran.impl.entities.RnibSlice;
import org.onosproject.xran.impl.entities.RnibUe;
import org.onosproject.xran.impl.identifiers.EcgiCrntiPair;
import org.onosproject.xran.impl.identifiers.LinkId;
import org.slf4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default xran store.
 */
@Component(immediate = true)
@Service
public class DefaultXranStore extends AbstractStore implements XranStore {
    private static final String XRAN_APP_ID = "org.onosproject.xran";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    protected ConcurrentMap<LinkId, RnibLink> linkMap = new ConcurrentHashMap<>();
    private ConcurrentMap<ECGI, RnibCell> cellMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Long, RnibUe> ueMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Object, RnibSlice> sliceMap = new ConcurrentHashMap<>();
    //private Set<CRNTI> crntiSet = new HashSet<>();

    private XranService controller;
    private IdGenerator ueIdGenerator;
    private Set<CRNTI> crntiSet = new HashSet<>();
    private ConcurrentMap<ECGI, Set<CRNTI>> ecCrntiMap = new ConcurrentHashMap<ECGI, Set<CRNTI>>();
    private Set<ECGI> ecgiSet = new HashSet<>();
    private ConcurrentMap<CRNTI, Set<ECGI>> crECGIMap = new ConcurrentHashMap<>();
    // map to get the context channel based on ecgi
    private ConcurrentMap<ECGI, ChannelHandlerContext> ctxMap = new ConcurrentHashMap<>();
    // pci-arfcn to ecgi bimap
    private BiMap<PCIARFCN, ECGI> pciarfcnMap = HashBiMap.create();
    // ECGI, CRNTI pair of primary cell for specified UE.
    private BiMap<EcgiCrntiPair, Long> crntiMap = HashBiMap.create();


    //Shubham: Creating this maps for storing handoff data.
    /*public BiMap<EcgiCrntiPair, RnibLink.Type> typeBiMap = HashBiMap.create();
    public BiMap<EcgiCrntiPair, Long> AvgBiMap = HashBiMap.create();*/

    public static ConcurrentMap<EcgiCrntiPair, RnibLink.Type> typeBiMap = new ConcurrentHashMap<>();
    public static ConcurrentMap<EcgiCrntiPair, Double> avgBiMap = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        ApplicationId appId = coreService.getAppId(XRAN_APP_ID);

        // create ue id generator
        ueIdGenerator = coreService.getIdGenerator("xran-ue-id");

        log.info("XRAN Default Store Started");
    }

    @Deactivate
    public void deactivate() {
        linkMap.clear();
        cellMap.clear();
        ueMap.clear();
        sliceMap.clear();

        controller = null;
        ueIdGenerator = null;

        log.info("XRAN Default Store Stopped");
    }

    @Override
    public List<RnibLink> getLinks() {
        List<RnibLink> list = Lists.newArrayList();
        list.addAll(linkMap.values());
        return list;
    }

    @Override
    public List<RnibLink> getLinks(ECGI ecgi) {
        List<RnibLink> list = Lists.newArrayList();
        list.addAll(
                linkMap.keySet()
                        .stream()
                        .filter(k -> k.getEcgi().equals(ecgi))
                        .map(v -> linkMap.get(v))
                        .collect(Collectors.toList()));
        return list;
    }

    @Override
    public List<RnibLink> getLinks(String eciHex) {
        List<RnibLink> list = Lists.newArrayList();
        EUTRANCellIdentifier eci = hexToEci(eciHex);

        list.addAll(
                linkMap.keySet()
                        .stream()
                        .filter(k -> k.getEcgi().getEUTRANcellIdentifier().equals(eci))
                        .map(v -> linkMap.get(v))
                        .collect(Collectors.toList()));

        return list;
    }

    @Override
    public List<RnibLink> getLinks(long euId) {
        List<RnibLink> list = Lists.newArrayList();

        list.addAll(
                linkMap.keySet()
                        .stream()
                        .filter(k -> k.getUeId().equals(euId))
                        .map(v -> linkMap.get(v))
                        .collect(Collectors.toList()));

        return list;
    }

    @Override
    public Optional<RnibLink> getLink(String eciHex, long euId) {
        EUTRANCellIdentifier eci = hexToEci(eciHex);

        Optional<LinkId> first = linkMap.keySet()
                .stream()
                .filter(linkId -> linkId.getEcgi().getEUTRANcellIdentifier().equals(eci) &&
                        linkId.getUeId().equals(euId))
                .findFirst();

        return first.map(linkId -> linkMap.get(linkId));
    }

    @Override
    public void storeLink(RnibLink link) {
        synchronized (this) {
            if (link.getLinkId() != null) {
                // if we add a primary link then change the primary to non serving
                if (link.getType().equals(RnibLink.Type.SERVING_PRIMARY)) {
                    RnibUe ue = link.getLinkId().getUe();
                    getLinks(ue.getId())
                            .stream()
                            .filter(l -> l.getType().equals(RnibLink.Type.SERVING_PRIMARY))
                            .forEach(l -> l.setType(RnibLink.Type.NON_SERVING));
                }
                linkMap.put(link.getLinkId(), link);
            }
        }
    }

    @Override
    public boolean removeLink(LinkId link) {
        return linkMap.remove(link) != null;
    }

    @Override
    public Optional<RnibLink> getLink(ECGI ecgi, Long ueId) {
        LinkId linkId = LinkId.valueOf(ecgi, ueId);
        return Optional.ofNullable(linkMap.get(linkId));
    }

    @Override
    public Optional<RnibLink> getLink(ECGI src, CRNTI dst) {
        return getUe(src, dst).flatMap(ue -> getLink(src, ue.getId()));
    }

    @Override
    public void modifyLinkRrmConf(RnibLink link, JsonNode rrmConf) {
        link.modifyRrmParameters(rrmConf);
    }

    @Override
    public List<Object> getNodes() {
        List<Object> list = Lists.newArrayList();
        list.addAll(cellMap.values());
        list.addAll(ueMap.values());
        return list;
    }

    @Override
    public List<RnibCell> getCellNodes() {
        List<RnibCell> list = Lists.newArrayList();
        list.addAll(cellMap.values());
        return list;
    }

    @Override
    public List<RnibUe> getUeNodes() {
        List<RnibUe> list = Lists.newArrayList();
        list.addAll(ueMap.values());
        return list;
    }

    @Override
    public Optional<Object> getNode(String nodeId) {
        try {
            return Optional.ofNullable(getCell(nodeId));
        } catch (Exception ignored) {
        }
        return Optional.ofNullable(getUe(Long.parseLong(nodeId)));
    }

    @Override
    public void storeCell(RnibCell cell) {
        if (cell.getEcgi() != null) {
            cellMap.putIfAbsent(cell.getEcgi(), cell);
        }
    }

    @Override
    public boolean removeCell(ECGI ecgi) {
        pciarfcnMap.inverse().remove(ecgi);
        ctxMap.remove(ecgi);
        return cellMap.remove(ecgi) != null;
    }

    @Override
    public boolean removeCell(PCIARFCN pciarfcn) {
        return removeCell(pciarfcnMap.get(pciarfcn));
    }

    @Override
    public Optional<RnibCell> getCell(String hexeci) {
        EUTRANCellIdentifier eci = hexToEci(hexeci);
        Optional<ECGI> first = cellMap.keySet()
                .stream()
                .filter(ecgi -> ecgi.getEUTRANcellIdentifier().equals(eci))
                .findFirst();
        return first.map(ecgi -> cellMap.get(ecgi));
    }

    @Override
    public Optional<RnibCell> getCell(ECGI ecgi) {
        return Optional.ofNullable(cellMap.get(ecgi));
    }

    @Override
    public Optional<RnibCell> getCell(PCIARFCN id) {
        ECGI ecgi;
        ecgi = pciarfcnMap.get(id);
        return getCell(ecgi);
    }

    @Override
    public void modifyCellRrmConf(RnibCell cell, JsonNode rrmConf) throws Exception {
        List<RnibLink> linkList = getLinks(cell.getEcgi());
        List<RnibUe> ueList = linkList.stream()
                .map(link -> link.getLinkId().getUe())
                .collect(Collectors.toList());

        cell.modifyRrmConfig(rrmConf, ueList);
    }

    @Override
    public Optional<RnibSlice> getSlice(long sliceId) {
        return Optional.ofNullable(sliceMap.get(sliceId));
    }

    @Override
    public boolean createSlice(ObjectNode attributes) {
        return false;
    }

    @Override
    public boolean removeCell(long sliceId) {
        return sliceMap.remove(sliceId) != null;
    }

    @Override
    public Optional<XranService> getController() {
        return Optional.ofNullable(controller);
    }

    @Override
    public void setController(XranService controller) {
        this.controller = controller;
    }

    @Override
    public void storeUe(RnibUe ue) {
        long newId;
        if (ue.getId() == null) {
            newId = ueIdGenerator.getNewId();
            ue.setId(newId);
        } else {
            newId = ue.getId();
        }
        ueMap.put(newId, ue);
    }

    @Override
    public void storeUe(RnibCell cell, RnibUe ue) {
        storeUe(ue);
        storeCrnti(cell, ue);
    }

    @Override
    public boolean removeUe(long ueId) {
        log.info("removing ue {} {}", ueId, ueMap);
        crntiMap.inverse().remove(ueId);
        return ueMap.remove(ueId) != null;
    }

    @Override
    public void storePciArfcn(RnibCell value) {
        value.getOptConf().ifPresent(cfg -> {
            PCIARFCN pciarfcn = new PCIARFCN();
            pciarfcn.setPci(cfg.getPci());
            pciarfcn.setEarfcnDl(cfg.getEarfcnDl());
            pciarfcnMap.put(pciarfcn, value.getEcgi());
        });
    }

    @Override
    public void storeCtx(RnibCell value, ChannelHandlerContext ctx) {
        if (value.getEcgi() != null) {
            ctxMap.put(value.getEcgi(), ctx);
            storeCell(value);
        }
    }

    @Override
    public Optional<ChannelHandlerContext> getCtx(ECGI ecgi) {
        return Optional.ofNullable(ctxMap.get(ecgi));
    }

    @Override
    public BiMap<EcgiCrntiPair, Long> getCrnti() {
        return crntiMap;
    }

    @Override
    public void storeCrnti(RnibCell cell, RnibUe ue) {
        CRNTI crnti = ue.getCrnti();
        ECGI ecgi = cell.getEcgi();

        if (crnti != null && ecgi != null) {
            // check if there is an ecgi, crnti pair for this UE id.
            EcgiCrntiPair oldPair = crntiMap.inverse().get(ue.getId()),
                    newPair = EcgiCrntiPair.valueOf(cell.getEcgi(), ue.getCrnti());

            if (oldPair == null) {
                crntiMap.put(newPair, ue.getId());
            } else {
                // remove old pair and add the new pair which corresponds to the primary cell.
                crntiMap.inverse().remove(ue.getId());
                crntiMap.put(newPair, ue.getId());
            }
        }
    }

    @Override
    public void putPrimaryLink(RnibCell cell, RnibUe ue) {
        RnibLink link = new RnibLink(cell, ue);
        // set link to primary before storing
        link.setType(RnibLink.Type.SERVING_PRIMARY);
        storeLink(link);
        storeCrnti(cell, ue);
    }

    @Override
    public Optional<RnibLink> putNonServingLink(RnibCell cell, CRNTI crnti) {
        return getUe(cell.getEcgi(), crnti).map(ue -> {
            RnibLink link = new RnibLink(cell, ue);
            storeLink(link);
            return Optional.of(link);
        }).orElse(Optional.empty());
    }

    @Override
    public Optional<RnibLink> putNonServingLink(RnibCell cell, Long ueId) {
        return getUe(ueId).map(ue -> {
            RnibLink link = new RnibLink(cell, ue);
            storeLink(link);
            return Optional.of(link);
        }).orElse(Optional.empty());
    }

    @Override
    public Optional<CRNTI> getCrnti(Long ueId) {
        return Optional.ofNullable(getCrnti().inverse().get(ueId).getValue());
    }

    @Override
    public Optional<RnibCell> getPrimaryCell(RnibUe ue) {
        // search all links for this UE and find PRIMARY.
        Optional<RnibLink> primary = getLinks().stream()
                .filter(l -> l.getType().equals(RnibLink.Type.SERVING_PRIMARY))
                .filter(l -> l.getLinkId().getUe().equals(ue))
                .findFirst();

        return primary.flatMap(l -> Optional.of(l.getLinkId().getCell()));
    }
    //Shubham: Adding this function to get all the existing links for a particular UE
    @Override
    public List<RnibCell> getAllCells(CRNTI crnti){
        List<RnibCell> allcells = Lists.newArrayList();
        getLinks().stream()
                .filter(l -> l.getLinkId().getUe().getCrnti().equals(crnti))
                .filter(l -> l.getType().equals(RnibLink.Type.NON_SERVING) || l.getType().equals(RnibLink.Type.SERVING_PRIMARY))
                .forEach(l -> allcells.add(l.getLinkId().getCell()));
        return allcells;
    }
    @Override
    public Optional<RnibUe> getUe(long ueId) {
        return Optional.ofNullable(ueMap.get(ueId));
    }

    @Override
    public Optional<RnibUe> getUe(ECGI ecgi, CRNTI crnti) {
        return Optional.ofNullable(crntiMap.get(EcgiCrntiPair.valueOf(ecgi, crnti)))
                .flatMap(this::getUe);
    }

    //Shubham: creating this Datastores for a map of ECGI and all the UEs connected to it.


    //Shubham: creating this methods to map all the UES connected to a particular CEll
    @Override
    public ConcurrentMap<ECGI, Set<CRNTI>> getSetofUEs(){
        // get all the existing ECGI's for each ECGI get all the CRNTI's attached.
        getCellNodes().forEach(cell -> {
            ecCrntiMap.put(cell.getEcgi(),
            getLinks(cell.getEcgi()).stream()
                                    .map(link -> link.getLinkId().getUe().getCrnti())
                                    .collect(Collectors.toSet())
            );
        });
        //log.info("********* ecCrntiMap = "+ecCrntiMap.toString());
        return ecCrntiMap;
    }

    @Override
    public ConcurrentMap<CRNTI, Set<ECGI>> getSetofCells(){
        getUeNodes().forEach(ue -> {
            crECGIMap.put(ue.getCrnti(),
                          getLinks(ue.getId()).stream()
                          .map(link -> link.getLinkId().getEcgi())
                          .collect(Collectors.toSet())
            );
        });
        //log.info("********* ecCrntiMap = "+ecCrntiMap.toString());
        return crECGIMap;
    }

    @Override
    public Set<CRNTI> getAllCRNTI(){
        getUeNodes().forEach( ue->{
            crntiSet.add(ue.getCrnti());
        });
        return crntiSet;
    }

    @Override
    public Optional<RnibUe> getUeForCRNTI(CRNTI crntiFinder){
        Optional<RnibUe> returnUe = getUeNodes().stream()
                .filter(ue -> ue.getCrnti().equals(crntiFinder))
                .findFirst();
        return returnUe;
    }

    public static ConcurrentMap<EcgiCrntiPair,RnibLink.Type> getTypeMap() {
        return typeBiMap;
    }

    public static ConcurrentMap<EcgiCrntiPair,Double> getAvgMap() {
        return avgBiMap;
    }

    /**
     * Get from HEX string the according ECI class object.
     *
     * @param eciHex HEX string
     * @return ECI object if created successfully
     */
    private EUTRANCellIdentifier hexToEci(String eciHex) {
        byte[] hexBinary = DatatypeConverter.parseHexBinary(eciHex);
        return new EUTRANCellIdentifier(hexBinary, 28);
    }
}
