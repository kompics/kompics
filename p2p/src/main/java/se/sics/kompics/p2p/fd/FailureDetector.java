package se.sics.kompics.p2p.fd;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.fd.events.Ping;
import se.sics.kompics.p2p.fd.events.Pong;
import se.sics.kompics.p2p.fd.events.PongTimedOut;
import se.sics.kompics.p2p.fd.events.SendPing;
import se.sics.kompics.p2p.fd.events.StartProbingPeer;
import se.sics.kompics.p2p.fd.events.StatusRequest;
import se.sics.kompics.p2p.fd.events.StatusResponse;
import se.sics.kompics.p2p.fd.events.StopProbingPeer;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>FailureDetector</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: FailureDetector.java 492 2007-12-11 16:44:23Z roberto $
 */
@ComponentSpecification
public final class FailureDetector {

	Logger logger;

	Component component;

	Channel timerSignalChannel;

	Channel pnSendChannel;

	private Channel requestChannel;

	private HashMap<Address, PeerProber> peerProbers;

	TimerHandler timerHandler;

	long rtoMin, pingInterval, pongTimeoutAdd;

	public FailureDetector(Component component) {
		this.component = component;
		peerProbers = new HashMap<Address, PeerProber>();
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel) {
		this.requestChannel = requestChannel;

		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane.getChannel(SetTimerEvent.class);
		timerSignalChannel = timerMembrane.getChannel(TimerSignalEvent.class);

		timerHandler = new TimerHandler(component, timerSetChannel);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannel(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = pnMembrane
				.getChannel(PerfectNetworkDeliverEvent.class);

		component.subscribe(timerSignalChannel, "handleSendPing");
		component.subscribe(timerSignalChannel, "handlePongTimedOut");

		component.subscribe(pnDeliverChannel, "handlePing");
		component.subscribe(pnDeliverChannel, "handlePong");

		component.subscribe(requestChannel, "handleStartProbingPeer");
		component.subscribe(requestChannel, "handleStopProbingPeer");
		component.subscribe(requestChannel, "handleStatusRequest");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(StartProbingPeer.class, requestChannel);
		map.put(StopProbingPeer.class, requestChannel);
		map.put(StatusRequest.class, requestChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod("fd.properties")
	public void init(Properties properties, Address localAddress) {
		logger = LoggerFactory.getLogger(FailureDetector.class.getName() + "@"
				+ localAddress.getId());

		rtoMin = Long.parseLong(properties.getProperty("rto.min", "100"));
		pingInterval = Long.parseLong(properties.getProperty("ping.interval",
				"3000"));
		pongTimeoutAdd = Long.parseLong(properties.getProperty(
				"pong.timeout.add", "10"));
	}

	@EventHandlerMethod
	public void handleStartProbingPeer(StartProbingPeer event) {
		Address peerAddress = event.getPeerAddress();
		if (!peerProbers.containsKey(peerAddress)) {
			PeerProber peerProber = new PeerProber(peerAddress, this);

			peerProber.addClientComponent(event.getComponent(), event
					.getChannel());
			peerProbers.put(peerAddress, peerProber);
			peerProber.start();
			logger.debug("Started probing peer {}", peerAddress);
		} else {
			peerProbers.get(peerAddress).addClientComponent(
					event.getComponent(), event.getChannel());
			logger.debug("Peer {} is already being probed", peerAddress);
		}
	}

	@EventHandlerMethod
	public void handleStopProbingPeer(StopProbingPeer event) {
		Address peerAddress = event.getPeerAddress();
		if (peerProbers.containsKey(peerAddress)) {
			PeerProber prober = peerProbers.get(peerAddress);
			Component requestingComponent = event.getComponent();
			if (prober.isClientComponent(requestingComponent)) {
				boolean last = prober
						.removeClientComponent(requestingComponent);
				if (last) {
					peerProbers.remove(peerAddress);
					prober.stop();
					logger.debug("Stoped probing peer {}", peerAddress);
				}
			} else {
				logger.debug(
						"Component {} didn't request the probing of peer {}",
						requestingComponent.getName(), peerAddress);
			}
		} else {
			logger.debug("Peer {} is not currently being probed", peerAddress);
		}
	}

	@EventHandlerMethod
	public void handleSendPing(SendPing event) {
		Address peer = event.getPeer();
		PeerProber prober = peerProbers.get(peer);
		if (prober != null) {
			prober.ping();
		} else {
			logger.debug("Peer {} is not currently being probed", peer);
		}
	}

	@EventHandlerMethod
	public void handlePongTimedOut(PongTimedOut event) {
		if (timerHandler.isOustandingTimer(event.getTimerId())) {
			Address peer = event.getPeer();
			if (peerProbers.containsKey(peer)) {
				peerProbers.get(peer).pingTimedOut();
			} else {
				logger.debug("Peer {} is not currently being probed", peer);
			}
		}
	}

	@EventHandlerMethod
	public void handlePing(Ping event) {
		logger.debug("Received Ping from {}. Sending Pong.", event.getSource());

		PerfectNetworkSendEvent sendEvent = new PerfectNetworkSendEvent(
				new Pong(), event.getSource());
		component.triggerEvent(sendEvent, pnSendChannel);
	}

	@EventHandlerMethod
	public void handlePong(Pong event) {
		Address peer = event.getSource();
		if (peerProbers.containsKey(peer)) {
			peerProbers.get(peer).pong();
		} else {
			logger.debug("Peer {} is not currently being probed", peer);
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(StatusResponse.class)
	public void handleStatusRequest(StatusRequest request) {
		LinkedList<Address> peers = new LinkedList<Address>();

		for (Address address : peerProbers.keySet()) {
			peers.add(address);
		}

		StatusResponse response = new StatusResponse(peers);

		component.triggerEvent(response, request.getChannel());
	}
}
