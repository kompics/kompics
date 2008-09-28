package se.sics.kompics.p2p.fd;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.p2p.fd.events.Ping;
import se.sics.kompics.p2p.fd.events.Pong;
import se.sics.kompics.p2p.fd.events.PongTimedOut;
import se.sics.kompics.p2p.fd.events.SendPing;
import se.sics.kompics.p2p.fd.events.StartProbingPeer;
import se.sics.kompics.p2p.fd.events.StatusRequest;
import se.sics.kompics.p2p.fd.events.StatusResponse;
import se.sics.kompics.p2p.fd.events.StopProbingPeer;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.ScheduleTimeout;
import se.sics.kompics.timer.events.Timeout;

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

	Channel alarmChannel;

	Channel pnSendChannel;

	private Channel requestChannel;

	private HashMap<Address, PeerProber> peerProbers;

	TimerHandler timerHandler;

	long rtoMin, livePingInterval, deadPingInterval, pongTimeoutAdd;

	Address localAddress;

	private final FailureDetector thisFailureDetector;

	public FailureDetector(Component component) {
		this.component = component;
		peerProbers = new HashMap<Address, PeerProber>();
		this.thisFailureDetector = this;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel) {
		this.requestChannel = requestChannel;

		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane
				.getChannelIn(ScheduleTimeout.class);
		alarmChannel = timerMembrane.getChannelOut(Timeout.class);

		timerHandler = new TimerHandler(component, timerSetChannel);

		// use shared PerfectNetwork component
		ComponentMembrane pnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.PerfectNetwork");
		pnSendChannel = pnMembrane.getChannelIn(Message.class);
		Channel pnDeliverChannel = pnMembrane.getChannelOut(Message.class);

		component.subscribe(alarmChannel, handleSendPing);
		component.subscribe(alarmChannel, handlePongTimedOut);

		component.subscribe(pnDeliverChannel, handlePing);
		component.subscribe(pnDeliverChannel, handlePong);

		component.subscribe(requestChannel, handleStartProbingPeer);
		component.subscribe(requestChannel, handleStopProbingPeer);
		component.subscribe(requestChannel, handleStatusRequest);
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		ComponentMembrane membrane = new ComponentMembrane(component);
		membrane.inChannel(StartProbingPeer.class, requestChannel);
		membrane.inChannel(StopProbingPeer.class, requestChannel);
		membrane.inChannel(StatusRequest.class, requestChannel);
		membrane.seal();
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod("fd.properties")
	public void init(Properties properties, Address localAddress) {
		this.localAddress = localAddress;
		logger = LoggerFactory.getLogger(FailureDetector.class.getName() + "@"
				+ localAddress.getId());

		rtoMin = Long.parseLong(properties.getProperty("rto.min"));
		livePingInterval = Long.parseLong(properties
				.getProperty("live.ping.interval"));
		deadPingInterval = Long.parseLong(properties
				.getProperty("dead.ping.interval"));
		pongTimeoutAdd = Long.parseLong(properties
				.getProperty("pong.timeout.add"));
	}

	private EventHandler<StartProbingPeer> handleStartProbingPeer = new EventHandler<StartProbingPeer>() {
		public void handle(StartProbingPeer event) {
			Address peerAddress = event.getPeerAddress();
			if (!peerProbers.containsKey(peerAddress)) {
				PeerProber peerProber = new PeerProber(peerAddress,
						thisFailureDetector);

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
	};

	private EventHandler<StopProbingPeer> handleStopProbingPeer = new EventHandler<StopProbingPeer>() {
		public void handle(StopProbingPeer event) {
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
					logger
							.debug(
									"Component {} didn't request the probing of peer {}",
									requestingComponent.getName(), peerAddress);
				}
			} else {
				logger.debug("Peer {} is not currently being probed",
						peerAddress);
			}
		}
	};

	private EventHandler<SendPing> handleSendPing = new EventHandler<SendPing>() {
		public void handle(SendPing event) {
			Address peer = event.getPeer();
			PeerProber prober = peerProbers.get(peer);
			if (prober != null) {
				prober.ping();
			} else {
				logger.debug("Peer {} is not currently being probed", peer);
			}
		}
	};

	private EventHandler<PongTimedOut> handlePongTimedOut = new EventHandler<PongTimedOut>() {
		public void handle(PongTimedOut event) {
			if (timerHandler.isOustandingTimer(event.getTimerId())) {
				Address peer = event.getPeer();
				if (peerProbers.containsKey(peer)) {
					peerProbers.get(peer).pongTimedOut();
				} else {
					logger.debug("Peer {} is not currently being probed", peer);
				}
			}
		}
	};

	private EventHandler<Ping> handlePing = new EventHandler<Ping>() {
		public void handle(Ping event) {
			logger.debug("Received Ping from {}. Sending Pong.", event
					.getSource());

			component.triggerEvent(new Pong(event.getId(), localAddress, event
					.getSource()), pnSendChannel);
		}
	};

	private EventHandler<Pong> handlePong = new EventHandler<Pong>() {
		public void handle(Pong event) {
			Address peer = event.getSource();
			if (peerProbers.containsKey(peer)) {
				peerProbers.get(peer).pong(event.getId());
			} else {
				logger.debug("Peer {} is not currently being probed", peer);
			}
		}
	};

	@MayTriggerEventTypes(StatusResponse.class)
	private EventHandler<StatusRequest> handleStatusRequest = new EventHandler<StatusRequest>() {
		public void handle(StatusRequest request) {
			Map<Address, ProbedPeerData> probedPeers = new HashMap<Address, ProbedPeerData>();
			for (Map.Entry<Address, PeerProber> entry : peerProbers.entrySet()) {
				probedPeers.put(entry.getKey(), entry.getValue()
						.getProbedPeerData());
			}
			StatusResponse response = new StatusResponse(probedPeers);

			component.triggerEvent(response, request.getChannel());
		}
	};
}
