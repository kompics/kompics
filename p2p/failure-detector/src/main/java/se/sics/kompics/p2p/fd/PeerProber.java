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
public class PeerProber {

	private enum State {
		INIT, HSENT, HSUSPECT, STOPPED
	}

	private Logger logger;

	private FailureDetector fd;

	private boolean started = false;

	private State state;

	private Address probedPeer;

	private Set<Component> clientComponents;
	private HashMap<Component, Channel> clientChannels;

	private long intervalPingTimerId;
	private long pingTimerId;
	private long pingTimestamp;

	private FailureDetectorStatistics stats;

	PeerProber(Address probedPeer, FailureDetector fd) {
		this.probedPeer = probedPeer;
		this.fd = fd;
		this.stats = new FailureDetectorStatistics(fd.rtoMin);
		this.clientComponents = new HashSet<Component>();
		clientChannels = new HashMap<Component, Channel>();

		logger = LoggerFactory.getLogger(fd.logger.getName() + "->"
				+ probedPeer.getId());
	}

	void start() {
		if (!started) {
			setPingTimer();
			state = State.INIT;
			logger.debug("Started");
			started = true;
		}
	}

	void ping() {
		if (!started) {
			return;
		}
		switch (state) {
		case INIT:
			// Setting timer for the receiving the Pong packet
			pingTimerId = fd.timerHandler.setTimer(
					new PongTimedOut(probedPeer), fd.timerSignalChannel, stats
							.getRTO()
							+ fd.pongTimeoutAdd);

			sendPing();
			pingTimestamp = System.currentTimeMillis();

			logger.debug("State change {}->{}", State.INIT, State.HSENT);
			state = State.HSENT;
			break;

		case HSUSPECT:

			/*
			 * When suspecting, the FD continues sending pings until a pong is
			 * received. When that happens, the RTO will be updated considering
			 * the time passed from the first ping sent and the first pong
			 * received.
			 */
			logger.debug("Received no Pong, sending another ping...");

			sendPing();
			setPingTimer();
			break;

		default:
			logger.error("Wrong state");
			break;

		}
	}

	void pong() {
		if (!started) {
			return;
		}

		long RTT = System.currentTimeMillis() - pingTimestamp;
		logger.debug("RTT is {}", RTT);

		switch (state) {
		case HSENT:
			logger.debug("State change {}->{}", State.HSENT, State.INIT);

			fd.timerHandler.cancelTimer(pingTimerId);
			stats.updateRTO(RTT);
			setPingTimer();
			state = State.INIT;
			break;

		case HSUSPECT:
			logger.debug("State change {}->{}", State.HSUSPECT, State.INIT);

			stats.updateRTO(RTT);
			reviseSuspicion();
			setPingTimer();
			state = State.INIT;
			break;

		default:
			break;
		}
		logger.debug("Pong handler, exiting state {}", state);
	}

	void pingTimedOut() {
		if (!started) {
			return;
		}
		switch (state) {
		case INIT:
			logger.debug("Ignoring ping timed out event");
			break;

		case HSENT:
			logger.debug("State change {}->{}", State.HSENT, State.HSUSPECT);
			suspect();
			state = State.HSUSPECT;
			/*
			 * Continue sending pings until an answer is received
			 */
			setPingTimer();
			break;

		case HSUSPECT:
			logger.debug("Received no pong, sending again ping");
			setPingTimer();

			break;
		default:
			logger.debug("Wrong State");
			break;
		}
	}

	private void setPingTimer() {
		intervalPingTimerId = fd.timerHandler.setTimer(
				new SendPing(probedPeer), fd.timerSignalChannel,
				fd.pingInterval);
	}

	private void sendPing() {
		fd.component.triggerEvent(new Ping(fd.localAddress, probedPeer),
				fd.pnSendChannel);
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
		fd.timerHandler.cancelTimer(pingTimerId);

		state = State.STOPPED;
		started = false;
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
}
