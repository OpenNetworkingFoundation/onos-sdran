/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.host.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.*;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostStore;
import org.onosproject.net.host.HostStoreDelegate;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProviderService;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.packet.IPv6.getLinkLocalAddress;
import static org.onlab.util.Tools.get;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.HOST_EVENT;
import static org.onosproject.security.AppPermission.Type.HOST_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides basic implementation of the host SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class HostManager
        extends AbstractListenerProviderRegistry<HostEvent, HostListener, HostProvider, HostProviderService>
        implements HostService, HostAdminService, HostProviderRegistry {

    private final Logger log = getLogger(getClass());

    public static final String HOST_ID_NULL = "Host ID cannot be null";

    private final NetworkConfigListener networkConfigListener = new InternalNetworkConfigListener();

    private HostStoreDelegate delegate = new InternalStoreDelegate();
    public List<String> coordinates = new ArrayList<>();
    public static int countLines =0;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EdgePortService edgePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "allowDuplicateIps", boolValue = true,
            label = "Enable removal of duplicate ip address")
    private boolean allowDuplicateIps = true;

    @Property(name = "monitorHosts", boolValue = false,
            label = "Enable/Disable monitoring of hosts")
    private boolean monitorHosts = false;

    @Property(name = "probeRate", longValue = 30000,
            label = "Set the probe Rate in milli seconds")
    private long probeRate = 30000;

    @Property(name = "greedyLearningIpv6", boolValue = false,
            label = "Enable/Disable greedy learning of IPv6 link local address")
    private boolean greedyLearningIpv6 = false;

    @Property(name = "hostMoveTrackerEnabled", boolValue = false,
            label = "Enable tracking of rogue host moves")
    private boolean hostMoveTrackerEnabled = false;

    private static final int HOST_MOVED_THRESHOLD_IN_MILLIS = 200000;
    // If the host move happening within given threshold then increment the host move counter
    private static final int HOST_MOVE_COUNTER = 3;
    // Max value of the counter after which the host will not be considered as offending host
    private static final long OFFENDING_HOST_EXPIRY_IN_MINS = 1;
    // Default pool size of offending host clear executor thread
    private static final int DEFAULT_OFFENDING_HOST_THREADS_POOL_SIZE = 10;
    private Map<HostId, HostMoveTracker> hostMoveTracker = new ConcurrentHashMap<>();

    @Property(name = "hostMoveThresholdInMillis", intValue = HOST_MOVED_THRESHOLD_IN_MILLIS,
            label = "Host move threshold in milliseconds")
    private int hostMoveThresholdInMillis = HOST_MOVED_THRESHOLD_IN_MILLIS;

    @Property(name = "hostMoveCounter", intValue = HOST_MOVE_COUNTER,
            label = "Host move counter before host is marked as rogue host")
    private int hostMoveCounter = HOST_MOVE_COUNTER;

    @Property(name = "offendingHostExpiryInMins", longValue = OFFENDING_HOST_EXPIRY_IN_MINS,
            label = "Expiry time after which the host is cleared from being rogue host")
    private long offendingHostExpiryInMins = OFFENDING_HOST_EXPIRY_IN_MINS;

    @Property(name = "offendingHostClearThreadPool", intValue = DEFAULT_OFFENDING_HOST_THREADS_POOL_SIZE,
            label = "Thread pool capacity")
    protected int offendingHostClearThreadPool = DEFAULT_OFFENDING_HOST_THREADS_POOL_SIZE;

    private HostMonitor monitor;
    private ScheduledExecutorService offendingHostUnblockExecutor = null;



    @Activate
    public void activate(ComponentContext context) {
        store.setDelegate(delegate);
        eventDispatcher.addSink(HostEvent.class, listenerRegistry);
        cfgService.registerProperties(getClass());
        networkConfigService.addListener(networkConfigListener);
        monitor = new HostMonitor(packetService, this, interfaceService, edgePortService);
        monitor.setProbeRate(probeRate);
        monitor.start();
        cfgService.registerProperties(getClass());
        modified(context);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(HostEvent.class);
        networkConfigService.removeListener(networkConfigListener);
        cfgService.unregisterProperties(getClass(), false);
        monitor.shutdown();
        if (offendingHostUnblockExecutor != null) {
            offendingHostUnblockExecutor.shutdown();
        }
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        boolean oldValue = monitorHosts;
        readComponentConfiguration(context);
        if (probeRate > 0) {
            monitor.setProbeRate(probeRate);
        } else {
            log.warn("probeRate cannot be lessthan 0");
        }

        if (oldValue != monitorHosts) {
            if (monitorHosts) {
                startMonitoring();
            } else {
                stopMonitoring();
            }
        }
    }

    public  int readCSV(){
//log.info("\n$$$$$$$$$$$$ File => "+ System.getProperty("user.home")+"/file_test13.csv");
        int countLine = 0;
        FileReader csvFile = null;
        String charArr[];
        String line;

        try {
            BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.home")+"/file_test21.csv"));
            if (countLines ==0) {
                while ((line = br.readLine()) != null) {
                    charArr = line.split(",");
                    coordinates.add(charArr[1]);
                    countLine++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        countLines = countLine;
        return countLines;
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;
        int newHostMoveThresholdInMillis;
        int newHostMoveCounter;
        int newOffendinghostPoolSize;
        long newOffendingHostExpiryInMins;

        flag = Tools.isPropertyEnabled(properties, "monitorHosts");
        if (flag == null) {
            log.info("monitorHosts is not enabled " +
                    "using current value of {}", monitorHosts);
        } else {
            monitorHosts = flag;
            log.info("Configured. monitorHosts {}",
                    monitorHosts ? "enabled" : "disabled");
        }

        Long longValue = Tools.getLongProperty(properties, "probeRate");
        if (longValue == null || longValue == 0) {
            log.info("probeRate is not set sing default value of {}", probeRate);
        } else {
            probeRate = longValue;
            log.info("Configured. probeRate {}", probeRate);
        }

        flag = Tools.isPropertyEnabled(properties, "allowDuplicateIps");
        if (flag == null) {
            log.info("Removal of duplicate ip address is not configured");
        } else {
            allowDuplicateIps = flag;
            log.info("Removal of duplicate ip address is {}",
                    allowDuplicateIps ? "disabled" : "enabled");
        }

        flag = Tools.isPropertyEnabled(properties, "greedyLearningIpv6");
        if (flag == null) {
            log.info("greedy learning is not enabled " +
                    "using current value of {}", greedyLearningIpv6);
        } else {
            greedyLearningIpv6 = flag;
            log.info("Configured. greedyLearningIpv6 {}",
                    greedyLearningIpv6 ? "enabled" : "disabled");
        }

        flag = Tools.isPropertyEnabled(properties, "hostMoveTrackerEnabled");
        if (flag == null) {
            log.info("Host move tracker is not configured " +
                    "using current value of {}", hostMoveTrackerEnabled);
        } else {
            hostMoveTrackerEnabled = flag;
            log.info("Configured. hostMoveTrackerEnabled {}",
                    hostMoveTrackerEnabled ? "enabled" : "disabled");

            //On enable cfg ,sets default configuration vales added , else use the default values
            properties = context.getProperties();
            try {
                String s = get(properties, "hostMoveThresholdInMillis");
                newHostMoveThresholdInMillis = isNullOrEmpty(s) ?
                        hostMoveThresholdInMillis : Integer.parseInt(s.trim());

                s = get(properties, "hostMoveCounter");
                newHostMoveCounter = isNullOrEmpty(s) ? hostMoveCounter : Integer.parseInt(s.trim());

                s = get(properties, "offendingHostExpiryInMins");
                newOffendingHostExpiryInMins = isNullOrEmpty(s) ?
                        offendingHostExpiryInMins : Integer.parseInt(s.trim());

                s = get(properties, "offendingHostClearThreadPool");
                newOffendinghostPoolSize = isNullOrEmpty(s) ?
                        offendingHostClearThreadPool : Integer.parseInt(s.trim());
            } catch (NumberFormatException | ClassCastException e) {
                newHostMoveThresholdInMillis = HOST_MOVED_THRESHOLD_IN_MILLIS;
                newHostMoveCounter = HOST_MOVE_COUNTER;
                newOffendingHostExpiryInMins = OFFENDING_HOST_EXPIRY_IN_MINS;
                newOffendinghostPoolSize = DEFAULT_OFFENDING_HOST_THREADS_POOL_SIZE;
            }
            if (newHostMoveThresholdInMillis != hostMoveThresholdInMillis) {
                hostMoveThresholdInMillis = newHostMoveThresholdInMillis;
            }
            if (newHostMoveCounter != hostMoveCounter) {
                hostMoveCounter = newHostMoveCounter;
            }
            if (newOffendingHostExpiryInMins != offendingHostExpiryInMins) {
                offendingHostExpiryInMins = newOffendingHostExpiryInMins;
            }
            if (hostMoveTrackerEnabled && offendingHostUnblockExecutor == null) {
                setupThreadPool();
            } else if (newOffendinghostPoolSize != offendingHostClearThreadPool
                    && offendingHostUnblockExecutor != null) {
                offendingHostClearThreadPool = newOffendinghostPoolSize;
                offendingHostUnblockExecutor.shutdown();
                offendingHostUnblockExecutor = null;
                setupThreadPool();
            } else if (!hostMoveTrackerEnabled && offendingHostUnblockExecutor != null) {
                offendingHostUnblockExecutor.shutdown();
                offendingHostUnblockExecutor = null;
            }
            if (newOffendinghostPoolSize != offendingHostClearThreadPool) {
                offendingHostClearThreadPool = newOffendinghostPoolSize;
            }

            log.debug("modified hostMoveThresholdInMillis: {}, hostMoveCounter: {}, " +
                            "offendingHostExpiryInMins: {} ", hostMoveThresholdInMillis,
                    hostMoveCounter, offendingHostExpiryInMins);
        }

    }

    private synchronized void setupThreadPool() {
        offendingHostUnblockExecutor = Executors.newScheduledThreadPool(offendingHostClearThreadPool);
    }

    /**
     * Starts monitoring the hosts by IP Address.
     */
    private void startMonitoring() {
        store.getHosts().forEach(host -> {
            host.ipAddresses().forEach(ip -> {
                monitor.addMonitoringFor(ip);
            });
        });
    }

    /**
     * Stops monitoring the hosts by IP Address.
     */
    private void stopMonitoring() {
        store.getHosts().forEach(host -> {
            host.ipAddresses().forEach(ip -> {
                monitor.stopMonitoring(ip);
            });
        });
    }

    @Override
    protected HostProviderService createProviderService(HostProvider provider) {
        monitor.registerHostProvider(provider);
        return new InternalHostProviderService(provider);
    }

    @Override
    public int getHostCount() {
        checkPermission(HOST_READ);
        return store.getHostCount();
    }

    @Override
    public Iterable<Host> getHosts() {
        checkPermission(HOST_READ);
        return store.getHosts();
    }

    @Override
    public Host getHost(HostId hostId) {
        checkPermission(HOST_READ);
        checkNotNull(hostId, HOST_ID_NULL);
        return store.getHost(hostId);
    }

    @Override
    public Set<Host> getHostsByVlan(VlanId vlanId) {
        checkPermission(HOST_READ);
        return store.getHosts(vlanId);
    }

    @Override
    public Set<Host> getHostsByMac(MacAddress mac) {
        checkPermission(HOST_READ);
        checkNotNull(mac, "MAC address cannot be null");
        return store.getHosts(mac);
    }

    @Override
    public Set<Host> getHostsByIp(IpAddress ip) {
        checkPermission(HOST_READ);
        checkNotNull(ip, "IP address cannot be null");
        return store.getHosts(ip);
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        checkPermission(HOST_READ);
        checkNotNull(connectPoint, "Connection point cannot be null");
        return store.getConnectedHosts(connectPoint);
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        checkPermission(HOST_READ);
        checkNotNull(deviceId, "Device ID cannot be null");
        return store.getConnectedHosts(deviceId);
    }

    @Override
    public void startMonitoringIp(IpAddress ip) {
        checkPermission(HOST_EVENT);
        monitor.addMonitoringFor(ip);
    }

    @Override
    public void stopMonitoringIp(IpAddress ip) {
        checkPermission(HOST_EVENT);
        monitor.stopMonitoring(ip);
    }

    @Override
    public void requestMac(IpAddress ip) {
        // FIXME!!!! Auto-generated method stub
    }

    @Override
    public void removeHost(HostId hostId) {
        checkNotNull(hostId, HOST_ID_NULL);
        store.removeHost(hostId);
    }

    // Personalized host provider service issued to the supplied provider.
    private class InternalHostProviderService
            extends AbstractProviderService<HostProvider>
            implements HostProviderService {
        InternalHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription, boolean replaceIps) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            hostDescription = validateHost(hostDescription, hostId);

            if (!allowDuplicateIps) {
                removeDuplicates(hostId, hostDescription);
            }

            if (!hostMoveTrackerEnabled) {
                store.createOrUpdateHost(provider().id(), hostId,
                        hostDescription, replaceIps);
            } else if (!shouldBlock(hostId, hostDescription.locations())) {
                log.debug("Host move is allowed for host with Id: {} ", hostId);
                store.createOrUpdateHost(provider().id(), hostId,
                        hostDescription, replaceIps);
            } else {
                log.info("Host move is NOT allowed for host with Id: {} , removing from host store ", hostId);
            }

            if (monitorHosts) {
                hostDescription.ipAddress().forEach(ip -> {
                    monitor.addMonitoringFor(ip);
                });
            }

            // Greedy learning of IPv6 host. We have to disable the greedy
            // learning of configured hosts. Validate hosts each time will
            // overwrite the learnt information with the configured information.
            if (greedyLearningIpv6) {
                // Auto-generation of the IPv6 link local address
                // using the mac address
                Ip6Address targetIp6Address = Ip6Address.valueOf(
                        getLinkLocalAddress(hostId.mac().toBytes())
                );
                // If we already know this guy we don't need to do other
                if (!hostDescription.ipAddress().contains(targetIp6Address)) {
                    Host host = store.getHost(hostId);
                    // Configured host, skip it.
                    if (host != null && host.configured()) {
                        return;
                    }
                    // Host does not exist in the store or the target is not known
                    if ((host == null || !host.ipAddresses().contains(targetIp6Address))) {
                        // Use DAD to probe if interface MAC is not specified
                        MacAddress probeMac = interfaceService.getInterfacesByPort(hostDescription.location())
                                .stream().map(Interface::mac).findFirst().orElse(MacAddress.ONOS);
                        Ip6Address probeIp = !probeMac.equals(MacAddress.ONOS) ?
                                Ip6Address.valueOf(getLinkLocalAddress(probeMac.toBytes())) :
                                Ip6Address.ZERO;
                        // We send a probe using the monitoring service
                        monitor.sendProbe(
                                hostDescription.location(),
                                targetIp6Address,
                                probeIp,
                                probeMac,
                                hostId.vlanId()
                        );
                    }
                }
            }

            if (store.getHost(hostId).mac().toString().equals(MacAddress.valueOf("00:00:00:00:00:00").toString()) ) {
		final int[] flag = {1};
                try {
                    if (countLines==0) {

                        readCSV();
                    }
                    BasicHostConfig cfg = networkConfigService.getConfig(hostId, BasicHostConfig.class);
                    if (cfg == null) {
                        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
                        jsonNode.put(BasicHostConfig.GRID_X, -400);
                        jsonNode.put(BasicHostConfig.GRID_Y, 450);
                        jsonNode.put(BasicHostConfig.NAME, hostDescription.annotations().value("name"));
                        jsonNode.put(BasicHostConfig.UI_TYPE, hostDescription.annotations().value("uiType"));
                        networkConfigService.applyConfig(hostId, BasicHostConfig.class, jsonNode);
                        cfg = networkConfigService.getConfig(hostId, BasicHostConfig.class);
                    }
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    BasicHostConfig finalCfg = cfg;
                    executorService.submit(()->{
//int max=300;
//                        for (int i = 0; i <= 300; i++) {
//                            finalCfg.gridX(finalCfg.gridX() + 25);
//                            log.info("GRID_X   "+ finalCfg.gridX() );
//                            //cfg.gridY(new Double(+10));
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            HostDescription description = BasicHostOperator
//                                    .combine(finalCfg, BasicHostOperator.descriptionOf(store.getHost(hostId)));
//                            store.createOrUpdateHost(provider().id(), hostId,
//                                    description, replaceIps);
//                            if (i==max) {
//                                i=0;
//                                finalCfg.gridX(new Double("-500"));
//                                log.info("GRID_X MAX  "+ finalCfg.gridX() );
//                            }
//
//                        }
//                    });
//                        Iterator<String> iterator = coordinates.listIterator();
//                        while (iterator.hasNext()) {
//                            finalCfg.gridX(finalCfg.gridX() + 1);
//                            //cfg.gridY(new Double(i+10));
//                            HostDescription description = BasicHostOperator
//                                    .combine(finalCfg, BasicHostOperator.descriptionOf(store.getHost(hostId)));
//                            store.createOrUpdateHost(provider().id(), hostId,
//                                    description, replaceIps);
//                            try {
//                                Thread.sleep(200);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        try {
//                            Thread.sleep(15000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
			boolean fTravel = true;
			try {
                                Thread.sleep(20000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
			synchronized(this){
			if(flag[0] == 1){

                        for (int i = 0; i < coordinates.size(); i++) {
			    flag[0] = 0;
                            finalCfg.gridX(new Double(coordinates.get(i)));
			//cfg.gridY(new Double(i+10));
			    log.info("------ UE ----->"+i);		
       			    log.info("------ UE coordinates ----->"+coordinates.get(i));		
			    if(Integer.valueOf(coordinates.get(i)) > 1650 && fTravel){
				    if(Integer.valueOf(coordinates.get(i)) == 1750){
					    fTravel = false;
				    }
				    i=i+9;
				    continue;
			    }
			    if(Integer.valueOf(coordinates.get(i)) < -275 && !fTravel){

				    if(Integer.valueOf(coordinates.get(i)) == -375){
					    fTravel = true;
				    }
				    i=i+9;
				    continue;
			    }
			    HostDescription description = BasicHostOperator
				    .combine(finalCfg, BasicHostOperator.descriptionOf(store.getHost(hostId)));
			    store.createOrUpdateHost(provider().id(), hostId,
					    description, replaceIps);
			    i = i+9;
			    try {
				    Thread.sleep(1050);
			    } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
		    }
		}
                    });



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // When a new IP is detected, remove that IP on other hosts if it exists
        public void removeDuplicates(HostId hostId, HostDescription desc) {
            desc.ipAddress().forEach(ip -> {
                Set<Host> allHosts = store.getHosts(ip);
                allHosts.forEach(eachHost -> {
                    if (!(eachHost.id().equals(hostId))) {
                        log.info("Duplicate ip {} found on host {} and {}", ip,
                                hostId.toString(), eachHost.id().toString());
                        store.removeIp(eachHost.id(), ip);
                    }
                });
            });
        }

        // returns a HostDescription made from the union of the BasicHostConfig
        // annotations if it exists
        private HostDescription validateHost(HostDescription hostDescription, HostId hostId) {
            BasicHostConfig cfg = networkConfigService.getConfig(hostId, BasicHostConfig.class);
            checkState(cfg == null || cfg.isAllowed(), "Host {} is not allowed", hostId);

            return BasicHostOperator.combine(cfg, hostDescription);
        }

        @Override
        public void hostVanished(HostId hostId) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();
            Host host = store.getHost(hostId);

            if (!allowedToChange(hostId)) {
                log.info("Request to remove {} is ignored due to provider mismatch", hostId);
                return;
            }

            if (monitorHosts) {
                host.ipAddresses().forEach(ip -> {
                    monitor.stopMonitoring(ip);
                });
            }
            store.removeHost(hostId);
        }

        @Override
        public void removeIpFromHost(HostId hostId, IpAddress ipAddress) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();

            if (!allowedToChange(hostId)) {
                log.info("Request to remove {} from {} is ignored due to provider mismatch",
                        ipAddress, hostId);
                return;
            }

            store.removeIp(hostId, ipAddress);
        }

        @Override
        public void addLocationToHost(HostId hostId, HostLocation location) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();

            if (!allowedToChange(hostId)) {
                log.info("Request to add {} to {} is ignored due to provider mismatch",
                        location, hostId);
                return;
            }

            store.appendLocation(hostId, location);
        }

        @Override
        public void removeLocationFromHost(HostId hostId, HostLocation location) {
            checkNotNull(hostId, HOST_ID_NULL);
            checkValidity();

            if (!allowedToChange(hostId)) {
                log.info("Request to remove {} from {} is ignored due to provider mismatch",
                        location, hostId);
                return;
            }

            store.removeLocation(hostId, location);
        }

        /**
         * Providers should only be able to remove a host that is provided by itself,
         * or a host that is not configured.
         */
        private boolean allowedToChange(HostId hostId) {
            Host host = store.getHost(hostId);
            return host == null || !host.configured() || host.providerId().equals(provider().id());
        }


        /**
         * Deny host move if happening within the threshold time,
         * track moved host to identify offending hosts.
         *
         * @param hostId    host identifier
         * @param locations host locations
         */
        private boolean shouldBlock(HostId hostId, Set<HostLocation> locations) {
            Host host = store.getHost(hostId);
            // If host is not present in host store means host added for hte first time.
            if (host != null) {
                if (host.suspended()) {
                    // Checks host is marked as offending in other onos cluster instance/local instance
                    log.debug("Host id {} is moving frequently hence host moving " +
                            "processing is ignored", hostId);
                    return true;
                }
            } else {
                //host added for the first time.
                return false;
            }
            HostMoveTracker hostMove = hostMoveTracker.computeIfAbsent(hostId, id -> new HostMoveTracker(locations));
            if (Sets.difference(hostMove.getLocations(), locations).isEmpty() &&
                    Sets.difference(locations, hostMove.getLocations()).isEmpty()) {
                log.debug("Not hostmove scenario: Host id: {}, Old Host Location: {}, New host Location: {}",
                        hostId, hostMove.getLocations(), locations);
                return false; // It is not a host move scenario
            } else if (hostMove.getCounter() >= hostMoveCounter && System.currentTimeMillis() - hostMove.getTimeStamp()
                    < hostMoveThresholdInMillis) {
                //Check host move is crossed the threshold, then to mark as offending Host
                log.debug("Host id {} is identified as offending host and entry is added in cache", hostId);
                hostMove.resetHostMoveTracker(locations);
                store.suspend(hostId);
                //Set host suspended flag to false after given offendingHostExpiryInMins
                offendingHostUnblockExecutor.schedule(new UnblockOffendingHost(hostId),
                        offendingHostExpiryInMins,
                        TimeUnit.MINUTES);
                return true;
            } else if (System.currentTimeMillis() - hostMove.getTimeStamp()
                    < hostMoveThresholdInMillis) {
                //Increment the host move count as hostmove occured within the hostMoveThresholdInMillis time
                hostMove.updateHostMoveTracker(locations);
                log.debug("Updated the tracker with the host move registered for host: {}", hostId);
            } else if (System.currentTimeMillis() - hostMove.getTimeStamp()
                    > hostMoveThresholdInMillis) {
                //Hostmove is happened after hostMoveThresholdInMillis time so remove from host tracker.
                hostMove.resetHostMoveTracker(locations);
                store.unsuspend(hostId);
                log.debug("Reset the tracker with the host move registered for host: {}", hostId);
            }
            return false;
        }

        // Set host suspended flag to false after given offendingHostExpiryInMins.
        private final class UnblockOffendingHost implements Runnable {
            private HostId hostId;

            UnblockOffendingHost(HostId hostId) {
                this.hostId = hostId;
            }

            @Override
            public void run() {
                // Set the host suspended flag to false
                try {
                    store.unsuspend(hostId);
                    log.debug("Host {}: Marked host as unsuspended", hostId);
                } catch (Exception ex) {
                    log.debug("Host {}: not present in host list", hostId);
                }
            }
        }
    }


    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements HostStoreDelegate {
        @Override
        public void notify(HostEvent event) {
            post(event);
        }
    }

    // listens for NetworkConfigEvents of type BasicHostConfig and removes
    // links that the config does not allow
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED
                    || event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED)
                    && (event.configClass().equals(BasicHostConfig.class));
        }

        @Override
        public void event(NetworkConfigEvent event) {
            log.debug("Detected host network config event {}", event.type());
            HostEvent he = null;

            HostId hostId = (HostId) event.subject();
            BasicHostConfig cfg =
                    networkConfigService.getConfig(hostId, BasicHostConfig.class);

            if (!isAllowed(cfg)) {
                kickOutBadHost(hostId);
            } else {
                Host host = getHost(hostId);
                HostDescription desc =
                        (host == null) ? null : BasicHostOperator.descriptionOf(host);
                desc = BasicHostOperator.combine(cfg, desc);
                if (desc != null) {
                    he = store.createOrUpdateHost(host.providerId(), hostId, desc, false);
                }
            }

            if (he != null) {
                post(he);
            }
        }
    }

    // by default allowed, otherwise check flag
    private boolean isAllowed(BasicHostConfig cfg) {
        return (cfg == null || cfg.isAllowed());
    }

    // removes the specified host, if it exists
    private void kickOutBadHost(HostId hostId) {
        Host badHost = getHost(hostId);
        if (badHost != null) {
            removeHost(hostId);
        }
    }
}
