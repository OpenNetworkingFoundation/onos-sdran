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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Set;

/**
 * Created by dimitris on 7/22/17.
 */
@JsonPropertyOrder({
        "Identifier",
        "Description",
        "UE-IDs",
        "Links",
        "ValidityPeriod",
        "DesiredKPIs",
        "DeliveredKPIs",
        "RRMConfiguration"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class RnibSlice {
    @JsonProperty("Identifier")
    private long sliceId;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("UE-IDs")
    private Set<Long> ueID;
    @JsonProperty("Links")
    private Set<RnibLink> links;
    @JsonProperty("ValidityPeriod")
    private long validityPeriod;
    @JsonProperty("DesiredKPIs")
    private Object desiredKpis;
    @JsonProperty("DeliveredKPIs")
    private Object deliveredKpis;
    @JsonProperty("RRMConfiguration")
    private Object rrmConfiguration;

    public long getSliceId() {
        return sliceId;
    }

    public void setSliceId(long sliceId) {
        this.sliceId = sliceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Long> getUeID() {
        return ueID;
    }

    public void setUeID(Set<Long> ueID) {
        this.ueID = ueID;
    }

    public Set<RnibLink> getLinks() {
        return links;
    }

    public void setLinks(Set<RnibLink> links) {
        this.links = links;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public Object getDesiredKpis() {
        return desiredKpis;
    }

    public void setDesiredKpis(Object desiredKpis) {
        this.desiredKpis = desiredKpis;
    }

    public Object getDeliveredKpis() {
        return deliveredKpis;
    }

    public void setDeliveredKpis(Object deliveredKpis) {
        this.deliveredKpis = deliveredKpis;
    }

    public Object getRrmConfiguration() {
        return rrmConfiguration;
    }

    public void setRrmConfiguration(Object rrmConfiguration) {
        this.rrmConfiguration = rrmConfiguration;
    }
}
