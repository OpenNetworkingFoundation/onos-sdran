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

import org.onosproject.net.HostId;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.impl.entities.RnibUe;

import java.util.Set;

/**
 * Xran Host Listener.
 */
public interface XranHostListener {

    /**
     * Add UE as a host with location the primary link.
     *
     * @param ue      UE entity
     * @param ecgiSet All connected CELLs to this UE
     */
    void hostAdded(RnibUe ue, Set<ECGI> ecgiSet);

    /**
     * Remove UE based on Host Id.
     *
     * @param id Host Id generated from UE id
     */
    void hostRemoved(HostId id);
}
