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

package org.onosproject.xran.impl.providers;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.xran.XranService;
import org.onosproject.xran.XranDeviceListener;
import org.onosproject.xran.impl.controller.XranManager;
import org.onosproject.xran.impl.entities.RnibCell;
import org.slf4j.Logger;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.xran.impl.entities.RnibCell.uri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cell device provider.
 */
@Component(immediate = true)
public class CellDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger log = getLogger(CellDeviceProvider.class);
    private final InternalDeviceListener listener = new InternalDeviceListener();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected XranService controller;
    private DeviceProviderService providerService;

    public CellDeviceProvider() {
        super(new ProviderId("xran", "org.onosproject.providers.cell"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);

        log.info("XRAN Device Provider Started");
    }

    @Deactivate
    public void deactivate() {
        controller.removeListener(listener);
        providerRegistry.unregister(this);

        providerService = null;
        log.info("XRAN Device Provider Stopped");
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {

    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {

    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {

    }

    /**
     * Internal device listener.
     */
    private class InternalDeviceListener implements XranDeviceListener {
        XranManager xranConfig = new XranManager();

        @Override
        public void deviceAdded(RnibCell cell) {
            if (providerService == null) {
                return;
            }

            // use ECGI as device ID URI
            DeviceId id = deviceId(uri(cell.getEcgi()));

            ChassisId cId = new ChassisId(id.hashCode());
            String xy[] = XranManager.provideXY().get(cell.getEcgi()).split(",");

            Device.Type type = Device.Type.OTHER;
            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.NAME, "eNodeB " + cell.getEcgi().getEUTRANcellIdentifier())
                    .set(AnnotationKeys.PROTOCOL, "SCTP")
                    .set(AnnotationKeys.CHANNEL_ID, "xxx")
                    .set(AnnotationKeys.MANAGEMENT_ADDRESS, "127.0.0.1")
                    .set(AnnotationKeys.UI_TYPE, "m_base_station")
                    .set(AnnotationKeys.GRID_X, xy[1])
                    .set(AnnotationKeys.GRID_Y, xy[0])
                    .build();

            log.info("node cordinate & ECGI {} {} {}",xy[0],xy[1], cell.getEcgi().getEUTRANcellIdentifier().toString() );

            DeviceDescription descBase =
                    new DefaultDeviceDescription(id.uri(), type,
                            "xran", "0.1", "0.1", id.uri().toString(),
                            cId);
            DeviceDescription desc = new DefaultDeviceDescription(descBase, annotations);

            providerService.deviceConnected(id, desc);
        }

        @Override
        public void deviceRemoved(DeviceId id) {
            providerService.deviceDisconnected(id);
        }
    }
}
