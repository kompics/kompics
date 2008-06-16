package se.sics.kompics.p2p.application;

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
import se.sics.kompics.p2p.bootstrap.events.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.events.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.events.BootstrapResponse;
import se.sics.kompics.p2p.peer.events.FailPeer;
import se.sics.kompics.p2p.peer.events.JoinPeer;
import se.sics.kompics.p2p.peer.events.LeavePeer;

/**
 * The <code>PeerApplication</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerApplication {

	private Logger logger;

	private final Component component;

	// bootstrap client channels
	Channel bootstrapRequestChannel, bootstrapResponseChannel;

	private Address localAddress;

	public PeerApplication(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel commandChannel) {
		component.subscribe(commandChannel, "handleJoinPeer");
		component.subscribe(commandChannel, "handleLeavePeer");
		component.subscribe(commandChannel, "handleFailPeer");
	}

	@ComponentInitializeMethod
	public void create(Address localAddress) {
		this.localAddress = localAddress;

		logger = LoggerFactory.getLogger(PeerApplication.class.getName() + "@"
				+ this.localAddress.getId());

		// use shared BootstrapClient component
		ComponentMembrane bootMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.bootstrap.BootstrapClient");
		bootstrapRequestChannel = bootMembrane
				.getChannel(BootstrapRequest.class);
		bootstrapResponseChannel = bootMembrane
				.getChannel(BootstrapResponse.class);

		component
				.subscribe(bootstrapResponseChannel, "handleBootstrapResponse");
	}

	@EventHandlerMethod
	public void handleJoinPeer(JoinPeer event) {
		logger.debug("Started, trying to bootstrap");

		BootstrapRequest request = new BootstrapRequest(10);
		component.triggerEvent(request, bootstrapRequestChannel);
	}

	@EventHandlerMethod
	public void handleLeavePeer(LeavePeer event) {
		logger.debug("Leave not implemented");
	}

	@EventHandlerMethod
	public void handleFailPeer(FailPeer event) {
		logger.debug("Fail not implemented");
	}

	@EventHandlerMethod
	public void handleBootstrapResponse(BootstrapResponse event) {
		logger.debug("Got BoostrapResponse {}, bootstrap complete", event
				.getPeers().size());

		// bootstrap completed
		component.triggerEvent(new BootstrapCompleted(),
				bootstrapRequestChannel);
	}
}
