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

import org.onosproject.xran.asn1lib.pdu.HOComplete;
import org.onosproject.xran.asn1lib.pdu.UEAdmissionStatus;
import org.onosproject.xran.asn1lib.pdu.UEContextUpdate;

/**
 * Class to handle UE Context Update packet.
 */
public class ContextUpdateHandler {
    private UEContextUpdate contextUpdate;
    private UEAdmissionStatus admissionStatus;
    private HOComplete hoComplete;

    /**
     * Get Context Update.
     * @return UEContextUpdate
     */
    public UEContextUpdate getContextUpdate() {
        return contextUpdate;
    }

    /**
     * Set Context Update.
     * @param contextUpdate UEContextUpdate
     * @return boolean to check context update was for admissionStatus packet or HOComplete packet
     */
    public boolean setContextUpdate(UEContextUpdate contextUpdate) {
        synchronized (this) {
            this.contextUpdate = contextUpdate;

            return admissionStatus != null || hoComplete != null;
        }
    }

    /**
     * Get UEAdmissionStatus.
     * @return UEAdmissionStatus
     */
    public UEAdmissionStatus getAdmissionStatus() {
        return admissionStatus;
    }

    /**
     * Set UEAdmissionStatus.
     * @param admissionStatus UEAdmissionStatus
     * @return boolean contextUpdate exists or not
     */
    public boolean setAdmissionStatus(UEAdmissionStatus admissionStatus) {
        synchronized (this) {
            this.admissionStatus = admissionStatus;

            return contextUpdate != null;
        }
    }

    /**
     * Get HOComplete.
     * @return HOComplete
     */
    public HOComplete getHoComplete() {
        return hoComplete;
    }

    /**
     * Set HOComplete.
     * @param hoComplete HOComplete
     * @return boolean contextUpdate exists or not
     */
    public boolean setHoComplete(HOComplete hoComplete) {
        synchronized (this) {
            this.hoComplete = hoComplete;

            return contextUpdate != null;
        }
    }

    /**
     * Reset the values of the variables.
     *
     */
    public void reset() {
        synchronized (this) {
            this.hoComplete = null;
            this.admissionStatus = null;
            this.contextUpdate = null;
        }
    }

    @Override
    public String toString() {
        return "ContextUpdateHandler{" +
                "contextUpdate=" + (contextUpdate != null) +
                ", admissionStatus=" + (admissionStatus != null) +
                ", hoComplete=" + (hoComplete != null) +
                '}';
    }
}
