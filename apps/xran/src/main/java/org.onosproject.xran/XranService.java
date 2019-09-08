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

import org.onosproject.xran.asn1lib.pdu.RRMConfig;
import org.onosproject.xran.impl.entities.RnibLink;

import java.util.Optional;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by dimitris on 7/27/17.
 */
public interface XranService {

    /**
     * Send a HandOff request from one link to another.
     *
     * @param newLink target LINK entity
     * @param oldLink source LINK entity
     * @return blocking queue for RESPONSE
     * @throws InterruptedException interrupted exception
     */
    Optional<SynchronousQueue<String>> sendHoRequest(RnibLink newLink, RnibLink oldLink);

    /**
     * Add a device listener for CELL connections.
     *
     * @param listener listener
     */
    void addListener(XranDeviceListener listener);

    /**
     * Add a host listener for UE connections.
     *
     * @param listener listener
     */
    void addListener(XranHostListener listener);

    /**
     * Remove a CELL device listener.
     *
     * @param listener listener
     */
    void removeListener(XranDeviceListener listener);

    /**
     * Remove a UE host listener.
     *
     * @param listener listener
     */
    void removeListener(XranHostListener listener);

    /**
     * Send modified RRM configuration.
     *
     * @param rrmConfig configuration fields to send
     * @return blocking queue for RESPONSE
     */
    Optional<SynchronousQueue<String>> sendModifiedRrm(RRMConfig rrmConfig);

    /**
     * Send scell add packet for specified LINK.
     *
     * @param link LINK entity
     * @return blocking queue for RESPONSE
     */
    Optional<SynchronousQueue<String>> sendScellAdd(RnibLink link);

    /**
     * Send scell delete for specified LINK.
     *
     * @param link LINK entity
     * @return true if sent correctly
     */
    boolean sendScellDelete(RnibLink link);

    /**
     * Get northbound timeout.
     *
     * @return interval in milliseconds
     */
    int getNorthboundTimeout();
}
