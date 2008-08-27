package se.sics.kompics.p2p.application;

import java.util.Set;

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
import se.sics.kompics.p2p.bootstrap.events.PeerEntry;
import se.sics.kompics.p2p.chord.events.CreateRing;
import se.sics.kompics.p2p.chord.events.JoinRing;
import se.sics.kompics.p2p.chord.ring.events.JoinRingCompleted;
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

	// Chord channels
	Channel chordRequestChannel, chordResponseChannel;

	private Address localAddress;

	private boolean bootstraped;

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
		this.bootstraped = false;

		logger = LoggerFactory.getLogger(PeerApplication.class.getName() + "@"
				+ this.localAddress.getId());

		// use shared BootstrapClient component
		ComponentMembrane bootMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.bootstrap.BootstrapClient");
		bootstrapRequestChannel = bootMembrane
				.getChannelIn(BootstrapRequest.class);
		bootstrapResponseChannel = bootMembrane
				.getChannelOut(BootstrapResponse.class);

		// use shared ChordRing component
		ComponentMembrane ringMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.chord.Chord");
		chordRequestChannel = ringMembrane.getChannelIn(JoinRing.class);
		chordResponseChannel = ringMembrane
				.getChannelOut(JoinRingCompleted.class);

		component
				.subscribe(bootstrapResponseChannel, "handleBootstrapResponse");

		component.subscribe(chordResponseChannel, "handleJoinRingCompleted");
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
		if (!bootstraped) {
			logger.debug("Got BoostrapResponse {}, Bootstrap complete", event
					.getPeers().size());

			Set<PeerEntry> somePeers = event.getPeers();

			if (somePeers.size() > 0) {
				// we join though the first peer;
				PeerEntry peerEntry = somePeers.iterator().next();
				JoinRing request = new JoinRing(peerEntry.getAddress());
				component.triggerEvent(request, chordRequestChannel);
			} else {
				// we create a new ring
				CreateRing request = new CreateRing();
				component.triggerEvent(request, chordRequestChannel);
			}
			bootstraped = true;
		}
	}

	@EventHandlerMethod
	public void handleJoinRingCompleted(JoinRingCompleted event) {
		logger.debug("JoinRing completed");

		// bootstrap completed
		component.triggerEvent(new BootstrapCompleted(),
				bootstrapRequestChannel);
	}
}
