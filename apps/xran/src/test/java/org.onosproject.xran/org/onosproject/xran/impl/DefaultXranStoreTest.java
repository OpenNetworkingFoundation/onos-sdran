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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.xran.asn1lib.api.CRNTI;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.asn1lib.api.EUTRANCellIdentifier;
import org.onosproject.xran.asn1lib.api.PLMNIdentity;
import org.onosproject.xran.asn1lib.util.HexConverter;
import org.onosproject.xran.impl.entities.RnibCell;
import org.onosproject.xran.impl.entities.RnibLink;
import org.onosproject.xran.impl.entities.RnibUe;

import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class DefaultXranStoreTest {
    private static final CRNTI CRNTI0 = new CRNTI(new byte[]{(byte) 0xFF, (byte) 0xFF}, 16);
    private static final CRNTI CRNTI1 = new CRNTI(new byte[]{(byte) 0xFA, (byte) 0xAF}, 16);

    private static final RnibUe UE0 = new RnibUe();
    private static final RnibUe UE1 = new RnibUe();

    private static final RnibCell CELL0 = new RnibCell();
    private static final RnibCell CELL1 = new RnibCell();
    private static final String ECI0 = "00000010";
    private static final String ECI1 = "00000020";
    private static final long UEID0 = 0L;
    private static final long UEID1 = 1L;

    private static DefaultXranStore store = new DefaultXranStore();

    private static RnibLink primaryLink0;
    private static RnibLink primaryLink1;
    private static RnibLink nonServingLink0;
    private static RnibLink nonServingLink1;

    private Supplier exception = () -> {
        throw new IllegalArgumentException("item not found");
    };

    @Before
    public void setUp() throws Exception {
        CELL0.setEcgi(hexToEcgi("000001", ECI0));
        CELL1.setEcgi(hexToEcgi("000002", ECI1));

        UE0.setCrnti(CRNTI0);
        UE0.setId(UEID0);

        UE1.setCrnti(CRNTI1);
        UE1.setId(UEID1);

        store.storeCell(CELL0);
        store.storeCell(CELL1);

        store.storeUe(CELL0, UE0);
        store.storeUe(CELL1, UE1);

        store.putPrimaryLink(CELL0, UE0);
        store.putPrimaryLink(CELL1, UE1);

        store.putNonServingLink(CELL0, UEID1);
        store.putNonServingLink(CELL1, UEID0);

        primaryLink0 = store.getLink(CELL0.getEcgi(), UEID0).orElseThrow(exception);
        primaryLink1 = store.getLink(CELL1.getEcgi(), UEID1).orElseThrow(exception);

        nonServingLink0 = store.getLink(CELL0.getEcgi(), UEID1).orElseThrow(exception);
        nonServingLink1 = store.getLink(CELL1.getEcgi(), UEID0).orElseThrow(exception);
    }

    @After
    public void tearDown() throws Exception {
        assertEquals("wrong remove", true, store.removeCell(CELL0.getEcgi()));
        assertEquals("wrong remove", true, store.removeCell(CELL1.getEcgi()));

        assertEquals("wrong remove", true, store.removeUe(UEID0));
        assertEquals("wrong remove", true, store.removeUe(UEID1));

        assertEquals("wrong remove", true, store.removeLink(primaryLink0.getLinkId()));
        assertEquals("wrong remove", true, store.removeLink(primaryLink1.getLinkId()));

        assertEquals("wrong remove", true, store.removeLink(nonServingLink0.getLinkId()));
        assertEquals("wrong remove", true, store.removeLink(nonServingLink1.getLinkId()));

        assertEquals("wrong len", 0, store.getCellNodes().size());
        assertEquals("wrong len", 0, store.getUeNodes().size());
        assertEquals("wrong len", 0, store.getNodes().size());
        assertEquals("wrong len", 0, store.getLinks().size());
    }

    private ECGI hexToEcgi(String plmnId, String eci) throws Exception {
        byte[] bytes = HexConverter.fromShortHexString(plmnId);
        byte[] bytearray = DatatypeConverter.parseHexBinary(eci);

        PLMNIdentity plmnIdentity = new PLMNIdentity(bytes);
        EUTRANCellIdentifier eutranCellIdentifier = new EUTRANCellIdentifier(bytearray, 28);

        ECGI ecgi = new ECGI();
        ecgi.setEUTRANcellIdentifier(eutranCellIdentifier);
        ecgi.setPLMNIdentity(plmnIdentity);

        return ecgi;
    }

    @Test
    public void getPrimaryLink() throws Exception {
        assertEquals("wrong cell", CELL0, store.getPrimaryCell(UE0).orElseThrow(exception));
        assertEquals("wrong cell", CELL1, store.getPrimaryCell(UE1).orElseThrow(exception));
    }

    @Test
    public void mapSize() throws Exception {
        assertEquals("wrong len", 2, store.getCellNodes().size());
        assertEquals("wrong len", 2, store.getUeNodes().size());
        assertEquals("wrong len", 4, store.getNodes().size());
        assertEquals("wrong len", 4, store.getLinks().size());
    }

    @Test
    public void getUe() throws Exception {
        // GET FROM ID
        assertEquals("wrong ue", UE0, store.getUe(UEID0).orElseThrow(exception));
        assertEquals("wrong ue", UE1, store.getUe(UEID1).orElseThrow(exception));

        // GET FROM ECGI-CRNTI
        assertEquals("wrong ue", UE0, store.getUe(CELL0.getEcgi(), CRNTI0).orElseThrow(exception));
        assertEquals("wrong ue", UE1, store.getUe(CELL1.getEcgi(), CRNTI1).orElseThrow(exception));

        // GET FROM CRNTI
        assertEquals("wrong crnti", CRNTI0, store.getCrnti(UEID0).orElseThrow(exception));
        assertEquals("wrong crnti", CRNTI1, store.getCrnti(UEID1).orElseThrow(exception));
    }

    @Test
    public void getLink() throws Exception {
        // GET FROM ID
        assertEquals("wrong link", primaryLink0, store.getLink(CELL0.getEcgi(), UEID0).orElseThrow(exception));
        assertEquals("wrong link", primaryLink1, store.getLink(CELL1.getEcgi(), UEID1).orElseThrow(exception));
        assertEquals("wrong link", nonServingLink0, store.getLink(CELL0.getEcgi(), UEID1).orElseThrow(exception));
        assertEquals("wrong link", nonServingLink1, store.getLink(CELL1.getEcgi(), UEID0).orElseThrow(exception));

        // GET FROM CRNTI
        assertEquals("wrong link", primaryLink0, store.getLink(CELL0.getEcgi(), CRNTI0).orElseThrow(exception));
        assertEquals("wrong link", primaryLink1, store.getLink(CELL1.getEcgi(), CRNTI1).orElseThrow(exception));
        // GET FROM ECIHEX
        assertEquals("wrong link", primaryLink0, store.getLink(ECI0, UEID0).orElseThrow(exception));
        assertEquals("wrong link", primaryLink1, store.getLink(ECI1, UEID1).orElseThrow(exception));
        assertEquals("wrong link", nonServingLink0, store.getLink(ECI0, UEID1).orElseThrow(exception));
        assertEquals("wrong link", nonServingLink1, store.getLink(ECI1, UEID0).orElseThrow(exception));

        // LINKS SIZE
        assertEquals("wrong link", 2, store.getLinks(CELL0.getEcgi()).size());
        assertEquals("wrong link", 2, store.getLinks(ECI0).size());
        assertEquals("wrong link", 2, store.getLinks(UEID0).size());
        assertEquals("wrong link", 2, store.getLinks(CELL1.getEcgi()).size());
        assertEquals("wrong link", 2, store.getLinks(ECI1).size());
        assertEquals("wrong link", 2, store.getLinks(UEID1).size());
    }

    @Test
    public void getNodes() throws Exception {
        List<RnibCell> cellNodes = store.getCellNodes();

        assertEquals("wrong nodes", true, cellNodes.contains(CELL0));
        assertEquals("wrong nodes", true, cellNodes.contains(CELL1));

        List<RnibUe> ueNodes = store.getUeNodes();

        assertEquals("wrong nodes", true, ueNodes.contains(UE0));
        assertEquals("wrong nodes", true, ueNodes.contains(UE1));
    }

    @Test
    public void crntiMap() throws Exception {
        store.getCrnti().forEach(
                (ecgiCrntiPair, aLong) -> assertEquals("wrong primary",
                        store.getCell(ecgiCrntiPair.getKey()).get(),
                        store.getPrimaryCell(store.getUe(aLong).orElseThrow(exception)).get()
                )
        );
    }
}