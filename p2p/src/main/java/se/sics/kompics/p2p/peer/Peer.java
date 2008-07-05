package se.sics.kompics.p2p.peer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapClient;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCacheReset;
import se.sics.kompics.p2p.bootstrap.events.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.events.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.events.BootstrapResponse;
import se.sics.kompics.p2p.fd.events.StartProbingPeer;
import se.sics.kompics.p2p.fd.events.StatusRequest;
import se.sics.kompics.p2p.fd.events.StopProbingPeer;
import se.sics.kompics.p2p.monitor.PeerMonitorClient;
import se.sics.kompics.p2p.monitor.events.StartPeerMonitor;
import se.sics.kompics.p2p.monitor.events.StopPeerMonitor;
import se.sics.kompics.p2p.network.events.LossyNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.LossyNetworkSendEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.p2p.peer.events.FailPeer;
import se.sics.kompics.p2p.peer.events.JoinPeer;
import se.sics.kompics.p2p.peer.events.LeavePeer;
import se.sics.kompics.p2p.son.ps.events.CreateRing;
import se.sics.kompics.p2p.son.ps.events.GetRingNeighborsRequest;
import se.sics.kompics.p2p.son.ps.events.GetRingNeighborsResponse;
import se.sics.kompics.p2p.son.ps.events.JoinRing;
import se.sics.kompics.p2p.son.ps.events.JoinRingCompleted;
import se.sics.kompics.p2p.son.ps.events.NewPredecessor;
import se.sics.kompics.p2p.son.ps.events.NewSuccessor;

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

	private Channel peerChannel;

	private Address peerAddress;

	public Peer(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel peerChannel) {
		this.peerChannel = peerChannel;

		component.subscribe(peerChannel, "handleJoinPeer");
		component.subscribe(peerChannel, "handleLeavePeer");
		component.subscribe(peerChannel, "handleFailPeer");
	}

	@ComponentInitializeMethod
	public void init(Address localAddress) throws IOException {
		peerAddress = localAddress;

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
		pnComponent.initialize(peerAddress);
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
		lnComponent.initialize(peerAddress);
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
		bootstrapClient.share("se.sics.kompics.p2p.bootstrap.BootstrapClient");

		// create channel for the FailureDetector component
		Channel fdRequestChannel = component.createChannel(
				StartProbingPeer.class, StopProbingPeer.class,
				StatusRequest.class);

		// create and share the FailureDetector component
		Component fdComponent = component.createComponent(
				"se.sics.kompics.p2p.fd.FailureDetector", component
						.getFaultChannel(), fdRequestChannel);
		fdComponent.initialize(peerAddress);
		fdComponent.share("se.sics.kompics.p2p.fd.FailureDetector");

		// create channels for the ChordRing component
		Channel chordRingRequestChannel = component
				.createChannel(CreateRing.class, JoinRing.class,
						GetRingNeighborsRequest.class);
		Channel chordRingNotificationChannel = component.createChannel(
				NewPredecessor.class, NewSuccessor.class,
				JoinRingCompleted.class, GetRingNeighborsResponse.class);

		// create the ChordRing component
		Component chordRing = component.createComponent(
				"se.sics.kompics.p2p.son.ps.ChordRing", component
						.getFaultChannel(), chordRingRequestChannel,
				chordRingNotificationChannel);
		chordRing.initialize(peerAddress);
		chordRing.share("se.sics.kompics.p2p.son.ps.ChordRing");

		// create the WebHandler component
		Component webHandler = component.createComponent(
				"se.sics.kompics.p2p.web.WebHandler", component
						.getFaultChannel());

		Properties properties = new Properties();
		properties.load(BootstrapClient.class
				.getResourceAsStream("bootstrap.properties"));
		Address bootstrapWebAddress = new Address(InetAddress
				.getByName(properties.getProperty("bootstrap.web.ip")), Integer
				.parseInt(properties.getProperty("bootstrap.web.port")),
				BigInteger.ZERO);
		properties = new Properties();
		properties.load(PeerMonitorClient.class
				.getResourceAsStream("monitor.properties"));
		Address monitorWebAddress = new Address(InetAddress
				.getByName(properties.getProperty("monitor.web.ip")), Integer
				.parseInt(properties.getProperty("monitor.web.port")),
				BigInteger.ZERO);

		webHandler.initialize(peerAddress, monitorWebAddress,
				bootstrapWebAddress);

		// create channel for the PeerMonitorClient component
		Channel pmcRequestChannel = component.createChannel(
				StartPeerMonitor.class, StopPeerMonitor.class);

		// create the PeerMonitorClient component
		Component peerMonitorClient = component.createComponent(
				"se.sics.kompics.p2p.monitor.PeerMonitorClient", component
						.getFaultChannel(), pmcRequestChannel);
		peerMonitorClient.initialize(peerAddress);

		// create the PeerApplication component
		Component peerApplication = component.createComponent(
				"se.sics.kompics.p2p.application.PeerApplication", component
						.getFaultChannel(), peerChannel);
		peerApplication.initialize(peerAddress);

		// starting PeerMonitor
		component.triggerEvent(new StartPeerMonitor(), pmcRequestChannel);

		logger.debug("Init");
	}

	@EventHandlerMethod
	public void handleJoinPeer(JoinPeer event) {
		logger.debug("Joined");
	}

	@EventHandlerMethod
	public void handleLeavePeer(LeavePeer event) {
		logger.debug("Leaved");
	}

	@EventHandlerMethod
	public void handleFailPeer(FailPeer event) {
		logger.debug("Failed");
	}
}
