package se.sics.kompics.p2p.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCacheReset;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.events.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.events.BootstrapResponse;
import se.sics.kompics.p2p.network.events.LossyNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.LossyNetworkSendEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.p2p.network.topology.NeighbourLinks;

/**
 * The <code>Peer</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class Peer {

	private Logger logger;

	private final Component component;

	private Address peerAddress;

	public Peer(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create() {
	}

	@ComponentInitializeMethod
	public void init(NeighbourLinks neighbourLinks) {
		peerAddress = neighbourLinks.getLocalAddress();

		logger = LoggerFactory.getLogger(Peer.class.getName() + "@"
				+ peerAddress.getId());

		// create channels for the PerfectNetwork component
		Channel pnSendChannel = component
				.createChannel(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = component
				.createChannel(PerfectNetworkDeliverEvent.class);

		// create and share the PerfectNetwork component
		Component pnComponent = component.createComponent(
				"se.sics.kompics.p2p.network.PerfectNetwork", component
						.getFaultChannel(), pnSendChannel, pnDeliverChannel);
		pnComponent.initialize(neighbourLinks);
		pnComponent.share("se.sics.kompics.p2p.network.PerfectNetwork");

		// create channels for the LossyNetwork component
		Channel lnSendChannel = component
				.createChannel(LossyNetworkSendEvent.class);
		Channel lnDeliverChannel = component
				.createChannel(LossyNetworkDeliverEvent.class);

		// create and share the LossyNetwork component
		Component lnComponent = component.createComponent(
				"se.sics.kompics.p2p.network.LossyNetwork", component
						.getFaultChannel(), lnSendChannel, lnDeliverChannel);
		lnComponent.initialize(neighbourLinks);
		lnComponent.share("se.sics.kompics.p2p.network.LossyNetwork");

		// create channels for the BootstrapClient component
		Channel bootRequestChannel = component.createChannel(
				BootstrapRequest.class, BootstrapCompleted.class,
				BootstrapCacheReset.class);
		Channel bootResponseChannel = component
				.createChannel(BootstrapResponse.class);

		// create the BootstrapClient component
		Component bootstrapClient = component.createComponent(
				"se.sics.kompics.p2p.bootstrap.BootstrapClient", component
						.getFaultChannel(), bootRequestChannel,
				bootResponseChannel);
		bootstrapClient.initialize(peerAddress);

		// create the WebHandler component
		Component webHandler = component.createComponent(
				"se.sics.kompics.p2p.web.WebHandler", component
						.getFaultChannel());
		webHandler.initialize(peerAddress);

		logger.debug("Init");
	}

	@EventHandlerMethod
	public void handle(Event event) {
	}
}
