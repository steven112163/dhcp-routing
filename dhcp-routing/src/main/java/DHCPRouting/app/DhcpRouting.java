/*
 * Copyright 2018-present Open Networking Foundation
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
package DHCPRouting.app;

import com.google.common.collect.ImmutableSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.Ethernet;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;

import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.InboundPacket;

import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;

import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;

import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;

import org.onosproject.net.host.HostService;

import org.onosproject.net.topology.TopologyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashMap;

import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class DhcpRouting {

	private static final int DEFAULT_TIMEOUT = 10;
	private static final int DEFAULT_PRIORITY = 50000;
    
	private final Logger log = LoggerFactory.getLogger(getClass());
    
	private final InternalConfigListener cfgListener = new InternalConfigListener();

	private final Set<ConfigFactory> factories = ImmutableSet.of(
		new ConfigFactory<ApplicationId, DhcpRoutingConfig>(APP_SUBJECT_FACTORY,
				DhcpRoutingConfig.class,
				"dhcpRouting") {
			@Override
			public DhcpRoutingConfig createConfig() {
				return new DhcpRoutingConfig();
			}
		}
	);
    

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected CoreService coreService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected NetworkConfigRegistry cfgService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected PacketService packetService;
    
	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected HostService hostService;
    
	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected TopologyService topologyService;

	private DHCPRoutingProcessor processor = new DHCPRoutingProcessor();
    
	@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
	protected FlowObjectiveService flowObjectiveService;
	protected FlowObjectiveService flowObjectiveService2;

	private ApplicationId appId;

	private static ConnectPoint deviceConnectPoint = null;


	@Activate
	protected void activate() {
		appId = coreService.registerApplication("DHCPRouting.app");
		cfgService.addListener(cfgListener);
		factories.forEach(cfgService::registerConfigFactory);
		packetService.addProcessor(processor, PacketProcessor.director(2));
		requestsPackets();

		log.info("Started");
	}

	@Deactivate
	protected void deactivate() {
		cfgService.removeListener(cfgListener);
		factories.forEach(cfgService::unregisterConfigFactory);
		flowRuleService.removeFlowRulesById(appId);
		packetService.removeProcessor(processor);
		processor = null;
		cancelPackets();

		log.info("Stopped");
	}

	/**
	 * Request packet in via PacketService
	 */
	private void requestsPackets() {
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
			.matchEthType(Ethernet.TYPE_IPV4)
			.matchIPProtocol(IPv4.PROTOCOL_UDP)
			.matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
			.matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
		packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);

		selector = DefaultTrafficSelector.builder()
			.matchEthType(Ethernet.TYPE_IPV4)
			.matchIPProtocol(IPv4.PROTOCOL_UDP)
			.matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
			.matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
		packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
	}

	/**
	 * Cancel requested packets in via packet service
	 */
	private void cancelPackets() {
		TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
			.matchEthType(Ethernet.TYPE_IPV4)
			.matchIPProtocol(IPv4.PROTOCOL_UDP)
			.matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
			.matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
		packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);

		selector = DefaultTrafficSelector.builder()
			.matchEthType(Ethernet.TYPE_IPV4)
			.matchIPProtocol(IPv4.PROTOCOL_UDP)
			.matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
			.matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
		packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
	}
    

	private class DHCPRoutingProcessor implements PacketProcessor {

		/**
		 * Process the packet
		 *
		 * @param context content of the incoming message
		 */
		@Override
		public void process(PacketContext context) {
			// Stop processing if the packet has been handled, since we
			// can't do any more to it.
			if (context.isHandled()) {
				return;
			}

			// Don't process the packet if the device connect point
			// isn't configured.
			if (deviceConnectPoint == null) {
				log.warn("Device connect point isn't configured!!!");
				return;
			}

			InboundPacket pkt = context.inPacket();
			Ethernet ethPkt = pkt.parsed();
			
			// Don't process null packet
			if (ethPkt == null) {
				return;
			}

			HostId dstId = HostId.hostId(ethPkt.getDestinationMAC()); 
			Host dst = hostService.getHost(dstId);   

			if (ethPkt.isBroadcast()) { // DHCP discover and request packets (L2)
				log.info("DHCP broadcast!!!\n");
				findPathAndInstallRule(context, pkt, ethPkt, dst, true);      
			} else { // DHCP offer and ack packets (L2)
				log.info("DHCP unicast!!!\n");
				findPathAndInstallRule(context, pkt, ethPkt, dst, false);
			}
		}
	}
    
	private class InternalConfigListener implements NetworkConfigListener {
	
		/**
		 * Reconfigures the DHCP Server according to the configuration parameters passed.
		 *
		 * @param cfg configuration object
		 */
		private void reconfigureNetwork(DhcpRoutingConfig cfg) {
			if (cfg == null) {
				return;
			}
			if (cfg.devicePoint() != null) {
				deviceConnectPoint = cfg.devicePoint();
			}
		}
		
		@Override
		public void event(NetworkConfigEvent event) {
			if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
				 event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
				 event.configClass().equals(DhcpRoutingConfig.class)) {

				DhcpRoutingConfig cfg = cfgService.getConfig(appId, DhcpRoutingConfig.class);
				reconfigureNetwork(cfg);
				log.info("Reconfigured!!!\n");
			}
		}
		
	}

	/**
	 * Find the way to the destination and install rule on a switch
	 *
	 * @param context content of the incoming message
	 * @param pkt the original packet
	 * @param ethPkt the Ethernet payload
	 * @param dst destination host
	 * @param broadcast true if the packet is broadcast
	 */
	private void findPathAndInstallRule(PacketContext context, InboundPacket pkt, Ethernet ethPkt, Host dst, boolean broadcast) {
		
		// Is the packet on an edge switch that the destination is on? If so,
		// simply forward out to the destination and bail.
		if (broadcast) {
			if (pkt.receivedFrom().deviceId().equals(deviceConnectPoint.deviceId())) {
				log.info("Broadcast is on destination switch!!!\n");
				installRule(context, deviceConnectPoint.port());
				return;
			}
		} else { 
			if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
				if (!context.inPacket().receivedFrom().port().equals(dst.location().port())) {
					log.info("Unicast is on edge switch!!!\n");
					installRule(context, dst.location().port());
				}
				return;
			}
		}
		
		// The packet is not on edge switch, find a path to the destination.
		Set<Path> paths;
		if (broadcast) {
			paths = topologyService.getPaths(topologyService.currentTopology(),
					pkt.receivedFrom().deviceId(),
					deviceConnectPoint.deviceId());
		} else {
			paths = topologyService.getPaths(topologyService.currentTopology(),
					pkt.receivedFrom().deviceId(),
					dst.location().deviceId());
		}
		
		// If there are no paths, flood and bail.
		if (paths.isEmpty()) {
			log.warn("Packet floods!!!\n");
			flood(context);
			return;
		}
		
		// Otherwise, pick a path that does not lead back to where the packet
		// came from; if no such path, flood and bail.
		Path path = pickForwardPathIfPossible(paths, pkt.receivedFrom().port());
		if (path == null) {
			log.warn("Packet doesn't know where to go from here {} for {} -> {}\n",
			pkt.receivedFrom(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC());
			flood(context);
			return;
		}

		// Otherwise forward and be done with it.
		installRule(context, path.src().port());
	}

	/**
	 * Send the packet out from the port
	 *
	 * @param context content of the incoming message
	 * @param portNumber output port of the packet
	 */
	private void packetOut(PacketContext context, PortNumber portNumber) {
		context.treatmentBuilder().setOutput(portNumber);
		context.send();
	}

	/**
	 * flood the packet
	 *
	 * @param context content of the incoming message
	 */
	private void flood(PacketContext context) {
		if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
											 context.inPacket().receivedFrom())) {
			packetOut(context, PortNumber.FLOOD);
		} else {
			context.block();
		}
	}

	/**
	 * Pick a path that does not lead back to where the packet
	 * came from
	 *
	 * @param paths all paths that can lead to the destination
	 * @param notToPort the port that the packet came from
	 */
	private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
		for (Path path : paths) {
			if (!path.src().port().equals(notToPort)) {
				return path;
			}
		}
		return null;
	}

	/**
	 * Install flow rules on a switch
	 *
	 * @param context content of the incoming message
	 * @param portNumber output port defined in the flow rule
	 */
	private void installRule(PacketContext context, PortNumber portNumber){
		TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
		InboundPacket pkt = context.inPacket(); 
		Ethernet ethPkt = pkt.parsed();

		if (ethPkt.isBroadcast()){
			selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
						.matchIPProtocol(IPv4.PROTOCOL_UDP)
						.matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
						.matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));

		} else {
			selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
						.matchEthSrc(ethPkt.getSourceMAC())
						.matchEthDst(ethPkt.getDestinationMAC()); 

		}

		TrafficTreatment treatment = DefaultTrafficTreatment.builder()
				.setOutput(portNumber)
				.build();

		ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
				.withSelector(selectorBuilder.build())
				.withTreatment(treatment)
				.withPriority(DEFAULT_PRIORITY)
				.withFlag(ForwardingObjective.Flag.VERSATILE)
				.fromApp(appId)
				.makeTemporary(DEFAULT_TIMEOUT)
				.add();

		flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(), forwardingObjective);          
		packetOut(context, portNumber);
	}
}
