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

package org.onosproject.xran;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import io.netty.channel.ChannelHandlerContext;
import org.onosproject.store.Store;
import org.onosproject.xran.asn1lib.api.CRNTI;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.asn1lib.api.PCIARFCN;
import org.onosproject.xran.impl.entities.RnibCell;
import org.onosproject.xran.impl.entities.RnibLink;
import org.onosproject.xran.impl.entities.RnibSlice;
import org.onosproject.xran.impl.entities.RnibUe;
import org.onosproject.xran.impl.identifiers.EcgiCrntiPair;
import org.onosproject.xran.impl.identifiers.LinkId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by dimitris on 7/22/17.
 */
public interface XranStore extends Store {

    // LINKS STORE

    /**
     * Get all active links.
     *
     * @return list of links
     */
    List<RnibLink> getLinks();

    /**
     * Get all links for that CELL based on ECGI.
     *
     * @param ecgi CELL ECGI
     *
     * @return list of links
     */
    List<RnibLink> getLinks(ECGI ecgi);

    /**
     * Get all links for that CELL based on ECI.
     *
     * @param eciHex HEX string of ECI
     *
     * @return list of links
     */
    List<RnibLink> getLinks(String eciHex);

    /**
     * Get all links for the UE based on UE ID.
     *
     * @param ueId UE ID
     *
     * @return list of links
     */
    List<RnibLink> getLinks(long ueId);

    /**
     * Get a link between a CELL and UE.
     *
     * @param cellId HEX string ECI
     * @param ueId   UE id
     *
     * @return link
     */
    Optional<RnibLink> getLink(String cellId, long ueId);

    /**
     * Get a link between a CELL's ECGI and UE's id.
     *
     * @param ecgi CELL ECGI
     * @param ueId UE id
     *
     * @return link
     */
    Optional<RnibLink> getLink(ECGI ecgi, Long ueId);

    /**
     * Get link based on ECGI and CRNTI.
     *
     * @param src CELL ECGI
     * @param dst CELL unique CRNTI
     * @return link if found
     */
    Optional<RnibLink> getLink(ECGI src, CRNTI dst);

    /**
     * Modify specified link's RRM Configuration.
     *
     * @param link    LINK entity
     * @param rrmConf json node of RRM Configuration
     */
    void modifyLinkRrmConf(RnibLink link, JsonNode rrmConf);

    /**
     * Put new link to store.
     *
     * @param link LINK entity
     */
    void storeLink(RnibLink link);

    /**
     * Remove link from store.
     *
     * @param link LINK entity
     *
     * @return true if remove succeeded
     */
    boolean removeLink(LinkId link);

    // NODES

    /**
     * Get all CELLs and UEs.
     *
     * @return list of UEs and CELLs
     */
    List<Object> getNodes();

    /**
     * Get all CELLs.
     *
     * @return list of CELLs
     */
    List<RnibCell> getCellNodes();

    /**
     * Get all UEs.
     *
     * @return list of UEs
     */
    List<RnibUe> getUeNodes();

    /**
     * Get node by node id.
     *
     * @param nodeId HEX string ECI or UE id
     *
     * @return CELL or UE
     */
    Optional<Object> getNode(String nodeId);

    // CELL

    /**
     * Get cell based on HEX string ECI.
     *
     * @param eci HEX string ECI
     *
     * @return CELL if found
     */
    Optional<RnibCell> getCell(String eci);

    /**
     * Get cell based on ECGI.
     *
     * @param cellId CELL ECGI
     *
     * @return CELL if found
     */
    Optional<RnibCell> getCell(ECGI cellId);

    /**
     * Get cell based on PCI-ARFCN.
     *
     * @param id PCI-ARFCN
     *
     * @return CELL entity if found
     */
    Optional<RnibCell> getCell(PCIARFCN id);

    /**
     * Modify CELL's RRM Configuration.
     *
     * @param cell    CELL entity
     * @param rrmConf json node of RRM Configuration
     *
     * @throws Exception exception
     */
    void modifyCellRrmConf(RnibCell cell, JsonNode rrmConf) throws Exception;

    /**
     * Put new CELL to the store.
     *
     * @param cell CELL entity
     */
    void storeCell(RnibCell cell);

    /**
     * Remove CELL from the store.
     *
     * @param ecgi CELL's ECGI
     *
     * @return ture if remove succeeded
     */
    boolean removeCell(ECGI ecgi);

    /**
     * Remove cell from three maps based on PCI-ARFCN.
     *
     * @param pciarfcn pci-arfcn of cell to remove
     *
     * @return true if remove succeeded
     */
    boolean removeCell(PCIARFCN pciarfcn);

    // SLICE

    /**
     * Get SLICE based on SLICE id.
     *
     * @param sliceId SLICE id
     * @return SLICE
     */
    Optional<RnibSlice> getSlice(long sliceId);

    /**
     * Put new SLICE to the store.
     *
     * @param attributes json node of SLICE attributes
     *
     * @return true if put succeeded
     */
    boolean createSlice(ObjectNode attributes);

    /**
     * Remove SLICE based on SLICE id.
     *
     * @param sliceId SLICE id
     *
     * @return true if remove succeeded
     */
    boolean removeCell(long sliceId);

    // CONTROLLER

    /**
     * Get the xran xranServer instance.
     *
     * @return xran xranServer
     */
    Optional<XranService> getController();

    /**
     * Set the xran xranServer instance.
     *
     * @param controller xran xranServer
     */
    void setController(XranService controller);

    // UE

    //Shubham: Adding this function to get all the existing links for a particular UE
    List<RnibCell> getAllCells(CRNTI crnti);

    /**
     * Get UE based on UE id.
     *
     * @param euId UE id
     *
     * @return UE entity
     */
    Optional<RnibUe> getUe(long euId);

    /**
     * Get UE based on ECGI and CRNTI.
     *
     * @param ecgi  CELL ECGI
     * @param crnti CELL unique CRNTI
     *
     * @return UE entity if found
     */
    Optional<RnibUe> getUe(ECGI ecgi, CRNTI crnti);

    /**
     * Put new UE to store.
     *
     * @param ue UE entity
     */
    void storeUe(RnibUe ue);

    /**
     * Put new UE to the store and update the ECGI, CRNTI pair.
     *
     * @param cell new primary CELL
     * @param ue   UE
     */
    void storeUe(RnibCell cell, RnibUe ue);

    /**
     * Remove UE from store.
     *
     * @param ueId UE id
     *
     * @return true if remove succeeded
     */
    boolean removeUe(long ueId);

    /**
     * Put the PCI-ARFCN to ECGI map from new cell.
     *
     * @param value CELL entity
     */
    void storePciArfcn(RnibCell value);


    /**
     * Put inside ECGI to CTX map.
     *
     * @param value CELL entity to get ECGI from
     * @param ctx   context channel
     */
    void storeCtx(RnibCell value, ChannelHandlerContext ctx);

    /**
     * Get context handler for specified ECGI.
     *
     * @param ecgi CELL ECGI
     * @return context handler if found
     */
    Optional<ChannelHandlerContext> getCtx(ECGI ecgi);

    /**
     * Get the ECGI, CRNTI to UE bimap.
     *
     * @return BiMap of EcgiCrntiPair to Long
     */
    BiMap<EcgiCrntiPair, Long> getCrnti();

    /**
     * Put new ECGI, CRNTI pair of primary link to UE and remove old one.
     *
     * @param cell new primary CELL
     * @param ue   UE
     */
    void storeCrnti(RnibCell cell, RnibUe ue);

    /**
     * Put a new primary link between a CELL and a UE.
     *
     * @param cell CELL entity
     * @param ue   UE entity
     */
    void putPrimaryLink(RnibCell cell, RnibUe ue);

    /**
     * Put non-serving link based on CELL and CRNTI.
     *
     * @param cell  CELL entity
     * @param crnti CRNTI
     * @return new link after creation
     */
    Optional<RnibLink> putNonServingLink(RnibCell cell, CRNTI crnti);

    /**
     * Put non-serving link based on CELL and UE id.
     *
     * @param cell CELL entity
     * @param ueId UE id
     * @return new link after creation
     */
    Optional<RnibLink> putNonServingLink(RnibCell cell, Long ueId);

    /**
     * Get CRNTI based on UE id.
     *
     * @param ueId UE id
     * @return UE if found
     */
    Optional<CRNTI> getCrnti(Long ueId);

    /*
        Get all the ues (all: having serving as well as non-serving links) attached to a particular cell
     */
    ConcurrentMap<ECGI, Set<CRNTI>> getSetofUEs();

    /*
        Get all the eNBs (all: having serving as well as non-serving links) attached to a particular UE
     */
    ConcurrentMap<CRNTI, Set<ECGI>> getSetofCells();


    /*
        Get all the CRNTIs present in the xranStore
     */
    Set<CRNTI> getAllCRNTI();

    /*
        Get a particular UE for given CRNTI
        @param crnti of Ue to find
        @return UE if found.
     */
    Optional<RnibUe> getUeForCRNTI(CRNTI crntiFinder);


    /**
     * Get primary CELL for specified UE.
     *
     * @param ue UE entity
     * @return primary CELL if found
     */
    Optional<RnibCell> getPrimaryCell(RnibUe ue);
}