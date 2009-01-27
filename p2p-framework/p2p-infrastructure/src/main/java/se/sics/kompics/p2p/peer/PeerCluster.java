package se.sics.kompics.p2p.peer;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.network.Address;
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

	private InetSocketAddress localSocketAddress;

	public PeerCluster(Component component) {
		this.component = component;
		this.peers = new HashMap<BigInteger, ComponentMembrane>();
	}

	@ComponentCreateMethod
	public void create(Channel peerClusterCommandChannel) {
		logger.debug("Create");

		component.subscribe(peerClusterCommandChannel, handleJoinPeer);
		component.subscribe(peerClusterCommandChannel, handleLeavePeer);
		component.subscribe(peerClusterCommandChannel, handleFailPeer);
	}

	@ComponentInitializeMethod
	public void init(InetSocketAddress localSocketAddress) {
		logger.debug("Init");
		this.localSocketAddress = localSocketAddress;
	}

	private EventHandler<JoinPeer> handleJoinPeer = new EventHandler<JoinPeer>() {
		public void handle(JoinPeer event) {
			ComponentMembrane membrane = createPeer(event.getPeerId());
			Channel peerChannel = membrane.getChannelIn(JoinPeer.class);

			component.triggerEvent(event, peerChannel);
		}
	};

	private EventHandler<LeavePeer> handleLeavePeer = new EventHandler<LeavePeer>() {
		public void handle(LeavePeer event) {
			ComponentMembrane membrane = peers.get(event.getPeerId());
			Channel peerChannel = membrane.getChannelIn(LeavePeer.class);

			component.triggerEvent(event, peerChannel);
			
			membrane.getComponent().stop();
		}
	};

	private EventHandler<FailPeer> handleFailPeer = new EventHandler<FailPeer>() {
		public void handle(FailPeer event) {
			ComponentMembrane membrane = peers.get(event.getPeerId());
			Channel peerChannel = membrane.getChannelIn(FailPeer.class);

			component.triggerEvent(event, peerChannel);

			membrane.getComponent().stop();
		}
	};

	private ComponentMembrane createPeer(BigInteger peerId) {
		// create channels for the port instance component
		Channel peerChannel = component.createChannel(JoinPeer.class,
				LeavePeer.class, FailPeer.class);

		// create a Peer component
		Component peer = component.createComponent(
				"se.sics.kompics.p2p.peer.Peer", component.getFaultChannel(),
				peerChannel);
		peer.initialize(new Address(localSocketAddress.getAddress(),
				localSocketAddress.getPort(), peerId));

		ComponentMembrane membrane = new ComponentMembrane(peer);
		membrane.inChannel(JoinPeer.class, peerChannel);
		membrane.inChannel(LeavePeer.class, peerChannel);
		membrane.inChannel(FailPeer.class, peerChannel);
		membrane.seal();
		peers.put(peerId, membrane);

		logger.debug("Created Peer {}", peerId);

		return membrane;
	}
}
