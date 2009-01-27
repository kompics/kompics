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

/**
 * The <code>PeerProber</code> class
 * 
 * @author Cosmin Arad
 * @author Roberto Roverso
 * @version $Id: PeerProber.java 294 2006-05-05 17:14:14Z roberto $
 */
public class PeerProberOld {

	private enum State {
		TRUST, AWAITING_PONG, SUSPECT;
	}

	private Logger logger;

	private FailureDetector fd;

	private State state;

	private Address probedPeer;

	private Set<Component> clientComponents;
	private HashMap<Component, Channel> clientChannels;

	private long intervalPingTimerId;
	private long pongTimeoutId;
	private long pingTimestamp;

	private PeerResponseTime times;

	PeerProberOld(Address probedPeer, FailureDetector fd) {
		this.probedPeer = probedPeer;
		this.fd = fd;
		this.times = new PeerResponseTime(fd.rtoMin);
		this.clientComponents = new HashSet<Component>();
		clientChannels = new HashMap<Component, Channel>();

		logger = LoggerFactory.getLogger(fd.logger.getName() + "->"
				+ probedPeer.getId());
	}

	void start() {
		state = State.TRUST;
		setTrustPingTimer();
		logger.debug("Started");
	}

	void ping() {
		if (state == State.TRUST) {
			// Setting timer for the receiving the Pong packet
			pongTimeoutId = fd.timerHandler.setTimer(new PongTimedOut(
					probedPeer), fd.alarmChannel, times.getRTO()
					+ fd.pongTimeoutAdd);

			sendPing(pongTimeoutId);
			pingTimestamp = System.currentTimeMillis();

			logger.debug("State change {}->{}", State.TRUST,
					State.AWAITING_PONG);
			state = State.AWAITING_PONG;
		} else if (state == State.SUSPECT) {
			/*
			 * When suspecting, the FD continues sending pings until a pong is
			 * received. When that happens, the RTO will be updated considering
			 * the time passed from the first ping sent and the first pong
			 * received.
			 */
			logger.debug("Received no Pong, sending another ping...");

			sendPing(pongTimeoutId);
			setSuspectPingTimer();
		} else {
			logger.error("Wrong state PING");
		}
	}

	void pong(long pongId) {
		long RTT = System.currentTimeMillis() - pingTimestamp;
		logger.debug("RTT is {}", RTT);

		if (state == State.AWAITING_PONG) {
			logger.debug("State change {}->{}", State.AWAITING_PONG,
					State.TRUST);

			fd.timerHandler.cancelTimer(pongId);
			fd.timerHandler.cancelTimer(pongTimeoutId);
			times.updateRTO(RTT);
			setTrustPingTimer();
			state = State.TRUST;
		} else if (state == State.SUSPECT) {
			logger.debug("State change {}->{}", State.SUSPECT, State.TRUST);

			times.updateRTO(RTT);
			reviseSuspicion();
			setTrustPingTimer();
			state = State.TRUST;
		} else if (state == State.TRUST) {
			logger.debug("Ignoring ping timed out event");
		}
		logger.debug("Pong handler, exiting state {}", state);
	}

	void pongTimedOut() {
		if (state == State.AWAITING_PONG) {
			logger.debug("State change {}->{}", State.AWAITING_PONG,
					State.SUSPECT);
			suspect();
			state = State.SUSPECT;
			/*
			 * Continue sending pings until an answer is received
			 */
			setSuspectPingTimer();
		} else {
			logger.error("Wrong State PTO");
		}
	}

	private void setTrustPingTimer() {
		intervalPingTimerId = fd.timerHandler.setTimer(
				new SendPing(probedPeer), fd.alarmChannel, fd.livePingInterval);
	}

	private void setSuspectPingTimer() {
		intervalPingTimerId = fd.timerHandler.setTimer(
				new SendPing(probedPeer), fd.alarmChannel, fd.deadPingInterval);
	}

	private void sendPing(long id) {
		fd.component.triggerEvent(new Ping(id, System.currentTimeMillis(),
				fd.localAddress, probedPeer), fd.pnSendChannel);
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
