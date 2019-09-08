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

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.VlanId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.impl.HostManager;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.xran.XranService;
import org.onosproject.xran.XranHostListener;
import org.onosproject.xran.asn1lib.api.ECGI;
import org.onosproject.xran.impl.entities.RnibUe;
import org.slf4j.Logger;

import java.util.Set;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.xran.impl.entities.RnibCell.uri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * UE Provider.
 */
/*
    in general provider is: provider of information about network environment, UE environment in this case
 */
@Component(immediate = true)
public class UeProvider extends AbstractProvider implements HostProvider {

    private static final Logger log = getLogger(UeProvider.class);
    private final InternalHostListener listener = new InternalHostListener();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostProviderRegistry providerRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private XranService controller;
    private HostProviderService providerService;

    public UeProvider() {
        super(new ProviderId("xran", "org.onosproject.providers.ue"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);

        log.info("XRAN Host Provider Started");
    }

    @Deactivate
    public void deactivate() {
        controller.removeListener(listener);
        providerRegistry.unregister(this);

        providerService = null;
        log.info("XRAN Host Provider Stopped");
    }

    @Override
    public void triggerProbe(Host host) {

    }

    /**
     * Internal host listener.
     */
    class InternalHostListener implements XranHostListener {

        @Override
        public void hostAdded(RnibUe ue, Set<ECGI> ecgiSet) {
            if (providerService == null) {
                return;
            }

            if (ue == null) {
                log.error("UE is not found");
                return;
            }

            try {
                Set<HostLocation> hostLocations = Sets.newConcurrentHashSet();

                ecgiSet.forEach(ecgi -> hostLocations
                        .add(new HostLocation(deviceId(uri(ecgi)),
                                PortNumber.portNumber(0), 0)));

                SparseAnnotations annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.NAME, "UE " + ue.getId())
                        .set(AnnotationKeys.UI_TYPE, "m_mobile")
                        .build();

                // Host ID is calculated from UE ID with some hacky function to represent a MAC address.
                DefaultHostDescription desc = new DefaultHostDescription(
                        ue.getHostId().mac(),
                        VlanId.vlanId(VlanId.UNTAGGED),
                        hostLocations,
                        Sets.newConcurrentHashSet(),
                        true,
                        annotations
                );

                providerService.hostDetected(ue.getHostId(), desc, false);
            } catch (Exception e) {
                log.warn(e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void hostRemoved(HostId id) {
            providerService.hostVanished(id);
        }
    }

    //ProviderId providerId, HostId hostId,
    //                           HostDescription hostDescription,
    //                           boolean replaceIps
//    public void updateHost(HostId id) {
//        try {
//            BasicHostConfig cfg = .getConfig(device.id(), BasicHostConfig.class);
//            log.info("Before Device id: {}   X:{} Y:{}",device.id(),cfg.gridX(),cfg.gridY());
//            cfg.gridX(cfg.gridX()+10);
//            log.info("After Device id: {}   X:{} Y:{}",device.id(),cfg.gridX(),cfg.gridY());
//            DeviceDescription description=BasicDeviceOperator.combine(cfg,BasicDeviceOperator.descriptionOf(device));
//            store.createOrUpdateDevice(device.providerId(),device.id(),description);
//            Thread.sleep(2000);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//hostManager.updateHost(providerService.provider().id(), id, );
//    }
}
