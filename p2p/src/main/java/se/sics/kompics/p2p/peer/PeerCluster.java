package se.sics.kompics.p2p.peer;

import java.math.BigInteger;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.p2p.network.topology.NeighbourLinks;
import se.sics.kompics.p2p.peer.events.FailPeer;
import se.sics.kompics.p2p.peer.events.JoinPeer;
import se.sics.kompics.p2p.peer.events.LeavePeer;

/**
 * The <code>PeerCluster</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerCluster {

	private static final Logger logger = LoggerFactory
			.getLogger(PeerCluster.class);

	private final Component component;

	private final HashMap<BigInteger, ComponentMembrane> peers;

	private NeighbourLinks neighbourLinks;

	public PeerCluster(Component component) {
		this.component = component;
		this.peers = new HashMap<BigInteger, ComponentMembrane>();
	}

	@ComponentCreateMethod
	public void create(Channel peerClusterCommandChannel) {
		logger.debug("Create");

		component.subscribe(peerClusterCommandChannel, "handleJoinPeer");
		component.subscribe(peerClusterCommandChannel, "handleLeavePeer");
		component.subscribe(peerClusterCommandChannel, "handleFailPeer");
	}

	@ComponentInitializeMethod
	public void init(NeighbourLinks neighbourLinks) {
		logger.debug("Init");
		this.neighbourLinks = neighbourLinks;
	}

	@EventHandlerMethod
	public void handleJoinPeer(JoinPeer event) {
		ComponentMembrane membrane = createPeer(event.getPeerId());
		Channel peerChannel = membrane.getChannel(JoinPeer.class);

		component.triggerEvent(event, peerChannel);
	}

	@EventHandlerMethod
	public void handleLeavePeer(LeavePeer event) {
		ComponentMembrane membrane = peers.get(event.getPeerId());
		Channel peerChannel = membrane.getChannel(LeavePeer.class);

		component.triggerEvent(event, peerChannel);
	}

	@EventHandlerMethod
	public void handleFailPeer(FailPeer event) {
		ComponentMembrane membrane = peers.get(event.getPeerId());
		Channel peerChannel = membrane.getChannel(FailPeer.class);

		component.triggerEvent(event, peerChannel);
	}

	private ComponentMembrane createPeer(BigInteger peerId) {
		// create channels for the port instance component
		Channel peerChannel = component.createChannel(JoinPeer.class,
				LeavePeer.class, FailPeer.class);

		// create a Peer component
		Component peer = component.createComponent(
				"se.sics.kompics.p2p.peer.Peer", component.getFaultChannel(),
				peerChannel);
		peer.initialize(neighbourLinks);

		// create a membrane for the port instance component
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(JoinPeer.class, peerChannel);
		map.put(LeavePeer.class, peerChannel);
		map.put(FailPeer.class, peerChannel);
		ComponentMembrane membrane = new ComponentMembrane(peer, map);
		peers.put(peerId, membrane);

		logger.debug("Created Peer {}", peerId);

		return membrane;
	}
}
