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

package org.onosproject.xran.impl.identifiers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.impl.entities.RnibCell;
import org.onosproject.xran.impl.entities.RnibUe;

/**
 * Class for LinkId.
 */
@JsonPropertyOrder({
        "ECGI",
        "UEID"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LinkId {
    @JsonIgnore
    private RnibCell cell;
    @JsonIgnore
    private RnibUe ue;

    private LinkId(RnibCell cell, RnibUe ue) {
        this.cell = cell;
        this.ue = ue;
    }

    /**
     * Create new LinkId.
     * @param cell Cell
     * @param ue UE
     * @return new LinkId
     */
    public static LinkId valueOf(RnibCell cell, RnibUe ue) {
        return new LinkId(cell, ue);
    }

    /**
     * Create new LinkID with ECGI and UE ID given.
     * @param ecgi ECGI of new cell
     * @param ueId UE ID of new UE
     * @return LinkId
     */
    public static LinkId valueOf(ECGI ecgi, Long ueId) {
        RnibCell cell = new RnibCell();
        RnibUe ue = new RnibUe();

        cell.setEcgi(ecgi);
        ue.setId(ueId);
        return new LinkId(cell, ue);
    }

    /**
     * Get ECGI.
     * @return ECGI
     */
    @JsonProperty("ECGI")
    public ECGI getEcgi() {
        return cell.getEcgi();
    }

    /**
     * Set ECGI.
     * @param sourceId ECGI
     */
    @JsonProperty("ECGI")
    public void setEcgi(ECGI sourceId) {
        cell.setEcgi(sourceId);
    }

    /**
     * Get UE ID.
     * @return long UE ID
     */
    @JsonProperty("UEID")
    public Long getUeId() {
        return ue.getId();
    }

    /**
     * Set UE ID.
     * @param destinationId long UE ID
     */
    @JsonProperty("UEID")
    public void setUeId(Long destinationId) {
        ue.setId(destinationId);
    }

    /**
     * Get Cell.
     * @return Cell
     */
    @JsonIgnore
    public RnibCell getCell() {
        return cell;
    }

    /**
     * Set Cell.
     * @param cell Cell
     */
    @JsonIgnore
    public void setCell(RnibCell cell) {
        this.cell = cell;
    }

    /**
     * Get UE.
     * @return UE
     */
    @JsonIgnore
    public RnibUe getUe() {
        return ue;
    }

    /**
     * Set UE.
     * @param ue UE
     */
    @JsonIgnore
    public void setUe(RnibUe ue) {
        this.ue = ue;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals()
     */
    @Override
    public boolean equals(Object o) {
        return this == o ||
                o != null &&
                        o instanceof LinkId &&
                        cell.getEcgi().equals(((LinkId) o).cell.getEcgi()) &&
                        ue.getId().equals(((LinkId) o).ue.getId());

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = cell.getEcgi().hashCode();
        result = 31 * result + ue.getId().hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
                .append(cell != null ? "\"cell\":" + cell : "")
                .append(ue != null ? ",\n\"ue\":" + ue : "")
                .append("\n}\n");
        return sb.toString();
    }
}
