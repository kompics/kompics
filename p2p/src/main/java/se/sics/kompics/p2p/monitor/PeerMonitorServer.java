package se.sics.kompics.p2p.monitor;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.monitor.events.PeerViewNotification;
import se.sics.kompics.p2p.network.events.LossyNetworkDeliverEvent;

/**
 * The <code>PeerMonitorServer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerMonitorServer {

	private static final Logger logger = LoggerFactory
			.getLogger(PeerMonitorServer.class);

	private final Component component;

	// private Channel lnSendChannel;
	//
	// private long updatePeriod;

	private HashMap<Address, Map<String, Object>> p2pNetworkData;

	public PeerMonitorServer(Component component) {
		super();
		this.component = component;
		this.p2pNetworkData = new HashMap<Address, Map<String, Object>>();
	}

	@ComponentCreateMethod
	public void create() {
		// use shared LossyNetwork component
		ComponentMembrane lnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.LossyNetwork");
		// lnSendChannel = lnMembrane.getChannel(LossyNetworkSendEvent.class);
		Channel lnDeliverChannel = lnMembrane
				.getChannel(LossyNetworkDeliverEvent.class);

		component.subscribe(lnDeliverChannel, "handlePeerNotification");
	}

	@ComponentInitializeMethod()
	public void init(long updatePeriod) {
		// this.updatePeriod = updatePeriod;
	}

	@EventHandlerMethod
	public void handlePeerNotification(PeerViewNotification event) {
		Address peerAddress = event.getPeerAddress();
		Map<String, Object> peerData = event.getPeerData();

		p2pNetworkData.put(peerAddress, peerData);

		logger.debug("Got notification from peer {}", peerAddress);
	}
}
