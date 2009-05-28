package se.sics.kompics.wan.cyclon;

import java.math.BigInteger;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.p2p.overlay.random.cyclon.CyclonConfiguration;
import se.sics.kompics.p2p.overlay.random.cyclon.CyclonGetPeersRequest;
import se.sics.kompics.p2p.overlay.random.cyclon.CyclonGetPeersResponse;
import se.sics.kompics.p2p.overlay.random.cyclon.CyclonNeighborsResponse;
import se.sics.kompics.p2p.overlay.structured.ring.NumericRingKey;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeer;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeerInit;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeerPort;
import se.sics.kompics.p2p.peer.cyclon.JoinCyclon;
import se.sics.kompics.p2p.simulator.P2pSimulator;
import se.sics.kompics.p2p.simulator.cyclon.CollectCyclonData;
import se.sics.kompics.p2p.simulator.cyclon.ConsistentHashtable;
import se.sics.kompics.p2p.simulator.cyclon.CyclonDataPoint;
import se.sics.kompics.p2p.simulator.cyclon.CyclonPeerFail;
import se.sics.kompics.p2p.simulator.cyclon.CyclonPeerGetPeer;
import se.sics.kompics.p2p.simulator.cyclon.CyclonPeerJoin;
import se.sics.kompics.p2p.simulator.cyclon.CyclonSimulatorPort;
import se.sics.kompics.p2p.simulator.cyclon.ReceivedMessage;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.SimulationScenarioLoadException;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.daemon.Daemon;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;

/**
 * 
 * All the slaves must be seeded with the same random number - otherwise join
 * events will be missed by slaves!
 * 
 * 
 * @author Jim Dowling
 * 
 */
public final class CyclonSimulatorWan extends ComponentDefinition {

	private static SimulationScenario scenario;

	private BigInteger numSlaves;
	private int slaveId;
	// private BigInteger localIdSpaceStart, localIdSpaceEnd;

	Positive<CyclonSimulatorPort> simulator = positive(CyclonSimulatorPort.class);
	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Negative<Web> web = negative(Web.class);

	private static final Logger logger = LoggerFactory.getLogger(CyclonSimulatorWan.class);
	private final HashMap<BigInteger, Component> peers;

	// peer initialization state
	private Address peer0Address;
	private BootstrapConfiguration bootstrapConfiguration;
	private P2pMonitorConfiguration monitorConfiguration;
	private CyclonConfiguration cyclonConfiguration;

	private int peerIdSequence;

	private BigInteger cyclonIdentifierSpaceSize;

	private ConsistentHashtable<BigInteger> cyclonView;

	// statistics
	private long totalPeerLifetime = 0;
	private long currentPeriodStartedAt = 0;
	private int currentPeriodPeerCount = 0;
	private CyclonDataPoint dataSet;

	public CyclonSimulatorWan() {
		peers = new HashMap<BigInteger, Component>();
		cyclonView = new ConsistentHashtable<BigInteger>();

		try {
			scenario = SimulationScenario.load(System.getProperty(Daemon.SCENARIO_FILENAME));
		} catch (SimulationScenarioLoadException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		subscribe(handleInit, control);

		subscribe(handleJoin, simulator);
		subscribe(handleFail, simulator);
		subscribe(handleGetPeer, simulator);
		subscribe(handleCollectData, simulator);
		subscribe(handleMessage, network);
	}

	Handler<CyclonSimulatorWanInit> handleInit = new Handler<CyclonSimulatorWanInit>() {
		public void handle(CyclonSimulatorWanInit init) {
			peers.clear();
			peerIdSequence = 0;
			dataSet = null;

			slaveId = init.getSlaveId();
			if (slaveId < 0) {
				throw new IllegalStateException("SlaveId must be >= 0 ");
			}
			int n = init.getNumSlaves();
			numSlaves = new BigInteger(Integer.toString(n));

			peer0Address = init.getPeer0Address();
			bootstrapConfiguration = init.getBootstrapConfiguration();
			monitorConfiguration = init.getMonitorConfiguration();
			cyclonConfiguration = init.getCyclonConfiguration();

			cyclonIdentifierSpaceSize = cyclonConfiguration.getIdentifierSpaceSize();

		}
	};

	final private boolean isEventForThisPeer(BigInteger val) {
		if (val.mod(numSlaves).equals(BigInteger.ZERO)) {
			return true;
		} else {
			return false;
		}
	}

	Handler<CyclonPeerJoin> handleJoin = new Handler<CyclonPeerJoin>() {
		public void handle(CyclonPeerJoin event) {
			BigInteger id = event.getCyclonId();

//			String host = event.getHost();
			
			if (isEventForThisPeer(id) == false) {
				return;
			}

			// join with the next id if this id is taken
			BigInteger successor = cyclonView.getNode(id);
			while (successor != null && successor.equals(id)) {
				id = id.add(BigInteger.ONE).mod(cyclonIdentifierSpaceSize);
				successor = cyclonView.getNode(id);
			}

			logger.debug("JOIN@{}", id);

			Component newPeer = createAndStartNewPeer(id);

			cyclonView.addNode(id);

			trigger(new JoinCyclon(id), newPeer.getPositive(CyclonPeerPort.class));

			addedPeer();
		}
	};

	Handler<CyclonPeerFail> handleFail = new Handler<CyclonPeerFail>() {
		public void handle(CyclonPeerFail event) {
			BigInteger id = cyclonView.getNode(event.getCyclonId());

			logger.debug("FAIL@" + id);

			if (cyclonView.size() == 0) {
				System.err.println("Empty network");
				return;
			}

			cyclonView.removeNode(id);
			removedPeer();

			stopAndDestroyPeer(id);
		}
	};

	Handler<CyclonPeerGetPeer> handleGetPeer = new Handler<CyclonPeerGetPeer>() {
		private int lcnt = 0;

		public void handle(CyclonPeerGetPeer event) {
			BigInteger id = cyclonView.getNode(event.getCyclonId());

			if (isEventForThisPeer(id) == false) {
				return;
			}
			if (peers.get(id) == null) {
				return;
			}

			logger.debug("{} GET_PEER@{}", ++lcnt, id);

			CyclonGetPeersRequest getPeerRequest = new CyclonGetPeersRequest(
					se.sics.kompics.wan.config.CyclonConfiguration.getCacheSize());
			trigger(getPeerRequest, peers.get(id).getPositive(CyclonPeerPort.class));
		}
	};

	Handler<CyclonGetPeersResponse> handleGetPeerResponse = new Handler<CyclonGetPeersResponse>() {
		public void handle(CyclonGetPeersResponse event) {
			// if (dataSet == null) {
			// return;
			// }
			// boolean success = event.getStatus().equals(
			// ChordLookupStatus.SUCCESS);
			// boolean correct = chordRingView.getNode(event.getKey()).equals(
			// event.getResponsible().getKey());
			//
			// int hopCount = 0;
			// long latency = 0;
			// if (success) {
			// hopCount = event.getLookupInfo().getHopCount();
			// latency = event.getLookupInfo().getDuration();
			// }
			//
			// ChordLookupStat stat = new ChordLookupStat(success, correct,
			// (NumericRingKey) event.getAttachment(), event.getKey(),
			// event.getResponsible().getKey(), latency, hopCount);
			//
			// dataSet.lookups.add(stat);
			// dataSet.totalLookups++;
			// if (success) {
			// dataSet.successLookups++;
			// } else {
			// dataSet.failedLookups++;
			// }
			// if (correct) {
			// dataSet.correctLookups++;
			// }
		}
	};

	private HashMap<Class<? extends Message>, ReceivedMessage> messageHistogram = new HashMap<Class<? extends Message>, ReceivedMessage>();
	private HashMap<NumericRingKey, Integer> loadHistogram = new HashMap<NumericRingKey, Integer>();

	Handler<Message> handleMessage = new Handler<Message>() {
		public void handle(Message event) {
			ReceivedMessage rm = messageHistogram.get(event.getClass());
			if (rm == null) {
				rm = new ReceivedMessage(event.getClass(), 0);
				messageHistogram.put(event.getClass(), rm);
			}
			rm.incrementCount();

			// if (dataSet != null) {
			// rm = dataSet.messageHistogram.get(event.getClass());
			// if (rm == null) {
			// rm = new ReceivedMessage(event.getClass(), 0);
			// dataSet.messageHistogram.put(event.getClass(), rm);
			// }
			// rm.incrementCount();
			// }

			// lookup load
			// if (event instanceof FindSuccessorRequest) {
			// FindSuccessorRequest req = (FindSuccessorRequest) event;
			// if (!req.isMaintenance()) {
			// NumericRingKey node = req.getChordDestination().getKey();
			// Integer count = loadHistogram.get(node);
			// if (count == null) {
			// loadHistogram.put(node, 1);
			// } else {
			// loadHistogram.put(node, count + 1);
			// }
			// if (dataSet != null) {
			// count = dataSet.loadHistogram.get(node);
			// if (count == null) {
			// dataSet.loadHistogram.put(node, 1);
			// } else {
			// dataSet.loadHistogram.put(node, count + 1);
			// }
			// }
			// }
			// }
		}
	};

	// private int snapshotPeerCount;

	Handler<CollectCyclonData> handleCollectData = new Handler<CollectCyclonData>() {
		public void handle(CollectCyclonData event) {
			// if (dataSet == null) {
			// dataSet = new CyclonDataSet();
			// dataSet.beganAt = System.currentTimeMillis();
			// logger.info("Started data collection...");
			// } else {
			// dataSet.endedAt = System.currentTimeMillis();
			// logger.info("Stopped data collection...");

			// updatedPeerCount(0);
			// CyclonDataPoint dataPoint = new CyclonDataPoint(
			// currentPeriodPeerCount, dataSet);
			// logger.info("DataPoint: \n{}", dataPoint);
			// }

			// updatedPeerCount(0);

			// snapshotPeerCount = 0;
			// snapshotPeers = new TreeMap<NumericRingKey,
			// ChordNeighborsResponse>();

			// LinkedList<ReceivedMessage> receivedMessages = new
			// LinkedList<ReceivedMessage>(
			// messageHistogram.values());
			// Collections.sort(receivedMessages);
			//
			// logger.info("Received Messages");
			// logger.info("-----------------");
			// for (ReceivedMessage receivedMessage : receivedMessages) {
			// logger.info(receivedMessage.toString());
			// }
			// logger.info("-----------------");

			// for (Class<? extends Message> messageType : messageHistogram
			// .keySet()) {
			// logger.info("{} = {}", messageType, messageHistogram
			// .get(messageType));
			// }

			// for (Component peer : peers.values()) {
			// trigger(new ChordNeighborsRequest(), peer
			// .getPositive(ChordPeerPort.class));
			// snapshotPeerCount++;
			// }
		}
	};

	// TreeMap<NumericRingKey, ChordNeighborsResponse> snapshotPeers;

	Handler<CyclonNeighborsResponse> handleNeighbors = new Handler<CyclonNeighborsResponse>() {
		public void handle(CyclonNeighborsResponse event) {
			// ChordAddress node = event.getNeighbors().getLocalPeer();
			// snapshotPeerCount--;
			// snapshotPeers.put(node.getKey(), event);
			//
			// if (snapshotPeerCount == 0) {
			// // dump view
			// for (NumericRingKey key : snapshotPeers.keySet()) {
			// ChordNeighborsResponse r = snapshotPeers.get(key);
			// logger.info("Node={} P={} S={}", new Object[] { key,
			// r.getNeighbors().getPredecessorPeer(),
			// r.getNeighbors().getSuccessorPeer() });
			// }
			// }
		}
	};

	private final void addedPeer() {
		updatedPeerCount(1);
	}

	private final void removedPeer() {
		updatedPeerCount(-1);
	}

	private final void updatedPeerCount(int increment) {
		long now = System.currentTimeMillis();
		long period = now - currentPeriodStartedAt;
		totalPeerLifetime += period * currentPeriodPeerCount;
		currentPeriodStartedAt = now;
		currentPeriodPeerCount += increment;

		if (totalPeerLifetime < 0)
			throw new RuntimeException("Total peer lifetime overflow");

		if (increment == 0) {
			logger.info("Total peer lifetime: {} Current peer count: {}", P2pSimulator
					.durationToString(totalPeerLifetime), currentPeriodPeerCount);
		} else {
			logger.debug("Period: {} Total lifetime: {} Current count: {}", new Object[] {
					P2pSimulator.durationToString(period),
					P2pSimulator.durationToString(totalPeerLifetime), currentPeriodPeerCount });
		}
	}

	private final static class MessageDestinationFilter extends ChannelFilter<Message, Address> {
		public MessageDestinationFilter(Address address) {
			super(Message.class, address, true);
		}

		public Address getValue(Message event) {
			return event.getDestination();
		}
	}

	private final static class WebRequestDestinationFilter extends
			ChannelFilter<WebRequest, Integer> {
		public WebRequestDestinationFilter(Integer destination) {
			super(WebRequest.class, destination, false);
		}

		public Integer getValue(WebRequest event) {
			return event.getDestination();
		}
	}

	private final Component createAndStartNewPeer(BigInteger id) {
		Component peer = create(CyclonPeer.class);
		int peerId = ++peerIdSequence;
		Address peerAddress = new Address(peer0Address.getIp(), peer0Address.getPort(), peerId);

		connect(network, peer.getNegative(Network.class), new MessageDestinationFilter(peerAddress));
		connect(timer, peer.getNegative(Timer.class));
		connect(web, peer.getPositive(Web.class), new WebRequestDestinationFilter(peerId));

		subscribe(handleGetPeerResponse, peer.getPositive(CyclonPeerPort.class));
		subscribe(handleNeighbors, peer.getPositive(CyclonPeerPort.class));

		trigger(new CyclonPeerInit(peerAddress, bootstrapConfiguration, monitorConfiguration,
				cyclonConfiguration), peer.getControl());

		trigger(new Start(), peer.getControl());
		peers.put(id, peer);

		return peer;
	}

	private final void stopAndDestroyPeer(BigInteger id) {
		Component peer = peers.get(id);

		trigger(new Stop(), peer.getControl());

		unsubscribe(handleGetPeerResponse, peer.getPositive(CyclonPeerPort.class));
		unsubscribe(handleNeighbors, peer.getPositive(CyclonPeerPort.class));

		disconnect(network, peer.getNegative(Network.class));
		disconnect(timer, peer.getNegative(Timer.class));
		disconnect(web, peer.getPositive(Web.class));

		peers.remove(id);
		destroy(peer);
	}
}
