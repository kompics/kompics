package se.sics.kompics.p2p.fd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.fd.events.PeerFailureSuspicion;
import se.sics.kompics.p2p.fd.events.Ping;
import se.sics.kompics.p2p.fd.events.PongTimedOut;
import se.sics.kompics.p2p.fd.events.SendPing;
import se.sics.kompics.p2p.fd.events.SuspicionStatus;

public class PeerProber {

	private Logger logger;

	private long intervalPingTimerId;
	private long pongTimeoutId;

	private boolean suspected;

	private FailureDetector fd;

	private PeerResponseTime times;
	private Set<Component> clientComponents;
	private HashMap<Component, Channel> clientChannels;

	private Address probedPeer;

	PeerProber(Address probedPeer, FailureDetector fd) {
		this.probedPeer = probedPeer;
		this.fd = fd;

		suspected = false;

		this.clientComponents = new HashSet<Component>();
		clientChannels = new HashMap<Component, Channel>();

		this.times = new PeerResponseTime(fd.rtoMin);

		logger = LoggerFactory.getLogger(fd.logger.getName() + "->"
				+ probedPeer.getId());
	}

	void start() {
		setPingTimer();
	}

	void ping() {
		logger.debug("PING");
		setPingTimer();

		// Setting timer for the receiving the Pong packet
//		pongTimeoutId = fd.timerHandler.setTimer(new PongTimedOut(probedPeer),
//				fd.alarmChannel, times.getRTO() + fd.pongTimeoutAdd);
		pongTimeoutId = fd.timerHandler.setTimer(new PongTimedOut(probedPeer),
				fd.alarmChannel, fd.pongTimeoutAdd);

		sendPing(pongTimeoutId, System.currentTimeMillis());
	}

	void pong(long pongId, long ts) {
		logger.debug("PoNG {}", pongId);

		if (suspected == true) {
			suspected = false;
			reviseSuspicion();
		}

		long RTT = System.currentTimeMillis() - ts;
		fd.timerHandler.cancelTimer(pongId);
//		fd.timerHandler.cancelTimer(pongTimeoutId);
		times.updateRTO(RTT);
	}

	void pongTimedOut() {
		if (suspected == false) {
			suspected = true;
			suspect();
		}
	}

	private void suspect() {
		PeerFailureSuspicion commPeerSuspectedEvent = new PeerFailureSuspicion(
				probedPeer, SuspicionStatus.SUSPECTED);
		logger.debug("Peer {} is suspected", probedPeer);

		/* we raise the event directly to subscriber components' channels */
		for (Component comp : clientComponents) {
			Channel channel = clientChannels.get(comp);
			if (channel != null) {
				fd.component.triggerEvent(commPeerSuspectedEvent, channel);
			}
		}
	}

	private void reviseSuspicion() {
		// Revising previous suspicion
		PeerFailureSuspicion commRectifyEvent = new PeerFailureSuspicion(
				probedPeer, SuspicionStatus.ALIVE);
		/* we raise the event directly to subscriber components' channels */
		for (Component comp : clientComponents) {
			Channel channel = clientChannels.get(comp);
			if (channel != null) {
				fd.component.triggerEvent(commRectifyEvent, channel);
			}
		}
	}

	void stop() {
		fd.timerHandler.cancelTimer(intervalPingTimerId);
		fd.timerHandler.cancelTimer(pongTimeoutId);
	}

	private void sendPing(long id, long ts) {
		fd.component
				.triggerEvent(new Ping(id, ts, fd.localAddress, probedPeer),
						fd.pnSendChannel);
	}

	private void setPingTimer() {
		long interval = suspected ? fd.deadPingInterval : fd.livePingInterval;
		intervalPingTimerId = fd.timerHandler.setTimer(
				new SendPing(probedPeer), fd.alarmChannel, interval);
	}

	void addClientComponent(Component monitoringComponent, Channel channel) {
		clientComponents.add(monitoringComponent);
		if (channel != null) {
			clientChannels.put(monitoringComponent, channel);
		}
	}

	boolean removeClientComponent(Component monitoringComponent) {
		clientComponents.remove(monitoringComponent);
		if (clientChannels.containsKey(monitoringComponent)) {
			clientChannels.remove(monitoringComponent);
		}
		return clientComponents.isEmpty();
	}

	boolean isClientComponent(Component component) {
		return clientComponents.contains(component);
	}

	ProbedPeerData getProbedPeerData() {
		return times.getProbedPeerData();
	}
}
