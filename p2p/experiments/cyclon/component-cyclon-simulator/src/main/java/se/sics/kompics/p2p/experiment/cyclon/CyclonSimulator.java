/**
 * This file is part of the Kompics P2P Framework.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.p2p.experiment.cyclon;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.TreeMap;

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
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorConfiguration;
import se.sics.kompics.p2p.overlay.OverlayAddress;
import se.sics.kompics.p2p.overlay.cyclon.CyclonAddress;
import se.sics.kompics.p2p.overlay.cyclon.CyclonConfiguration;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighbors;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsRequest;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsResponse;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeer;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeerInit;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeerPort;
import se.sics.kompics.p2p.peer.cyclon.JoinCyclon;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;

/**
 * The <code>CyclonSimulator</code> class represents the CyclonSimulator
 * component. It manages the peers of a Cyclon overlay and collects statistics.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonSimulator extends ComponentDefinition {

	Positive<CyclonExperiment> simulator = positive(CyclonExperiment.class);
	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Negative<Web> web = negative(Web.class);

	private static final Logger logger = LoggerFactory
			.getLogger(CyclonSimulator.class);
	private final HashMap<BigInteger, Component> peers;

	// peer initialization state
	private Address peer0Address;
	private BootstrapConfiguration bootstrapConfiguration;
	private CyclonMonitorConfiguration monitorConfiguration;
	private CyclonConfiguration cyclonConfiguration;

	private int peerIdSequence;

	private BigInteger cyclonIdentifierSpaceSize;
	private ConsistentHashtable<BigInteger> cyclonView;

	public CyclonSimulator() {
		peers = new HashMap<BigInteger, Component>();
		cyclonView = new ConsistentHashtable<BigInteger>();

		subscribe(handleInit, control);

		subscribe(handleJoin, simulator);
		subscribe(handleFail, simulator);
		subscribe(handleCollectData, simulator);
	}

	Handler<CyclonSimulatorInit> handleInit = new Handler<CyclonSimulatorInit>() {
		public void handle(CyclonSimulatorInit init) {
			peers.clear();
			peerIdSequence = 0;

			peer0Address = init.getPeer0Address();
			bootstrapConfiguration = init.getBootstrapConfiguration();
			monitorConfiguration = init.getMonitorConfiguration();
			cyclonConfiguration = init.getCyclonConfiguration();

			cyclonIdentifierSpaceSize = cyclonConfiguration
					.getIdentifierSpaceSize();
		}
	};

	Handler<CyclonPeerJoin> handleJoin = new Handler<CyclonPeerJoin>() {
		public void handle(CyclonPeerJoin event) {
			BigInteger id = event.getCyclonId();

			// join with the next id if this id is taken
			BigInteger successor = cyclonView.getNode(id);
			while (successor != null && successor.equals(id)) {
				id = id.add(BigInteger.ONE).mod(cyclonIdentifierSpaceSize);
				successor = cyclonView.getNode(id);
			}
			logger.debug("JOIN@{}", id);

			Component newPeer = createAndStartNewPeer(id);
			cyclonView.addNode(id);

			trigger(new JoinCyclon(id), newPeer
					.getPositive(CyclonPeerPort.class));
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
			stopAndDestroyPeer(id);
		}
	};

	private int snapshotPeerCount;
	private TreeMap<OverlayAddress, CyclonNeighbors> snapshotPeers;
	private int dataPointIndex = 0;

	Handler<CollectCyclonData> handleCollectData = new Handler<CollectCyclonData>() {
		public void handle(CollectCyclonData event) {
			logger.info("Data collection...");

			snapshotPeers = new TreeMap<OverlayAddress, CyclonNeighbors>();

			snapshotPeerCount = 0;
			for (Component peer : peers.values()) {
				trigger(new CyclonNeighborsRequest(), peer
						.getPositive(CyclonPeerPort.class));
				snapshotPeerCount++;
			}
		}
	};

	Handler<CyclonNeighborsResponse> handleNeighbors = new Handler<CyclonNeighborsResponse>() {
		public void handle(CyclonNeighborsResponse event) {
			CyclonAddress node = event.getNeighbors().getSelf();
			snapshotPeerCount--;

			if (node != null) {
				snapshotPeers.put(node, event.getNeighbors());
			}

			if (snapshotPeerCount == 0) {
				// dump view
				CyclonDataPoint dataPoint = new CyclonDataPoint(
						++dataPointIndex, snapshotPeers);
				logger.info("DataPoint: \n{}", dataPoint);

				/**
				 * TODO here you can change the code so that instead of printing
				 * out the data points, it collects them for aggregation. E.g.
				 * collect 10 consecutive data points and take their average.
				 */
			}
		}
	};

	/**
	 * The <code>MessageDestinationFilter</code> class.
	 * 
	 * @author Cosmin Arad <cosmin@sics.se>
	 * @version $Id: MessageDestinationFilter.java 750 2009-04-02 09:55:01Z
	 *          Cosmin $
	 */
	private final static class MessageDestinationFilter extends
			ChannelFilter<Message, Address> {
		public MessageDestinationFilter(Address address) {
			super(Message.class, address, true);
		}

		public Address getValue(Message event) {
			return event.getDestination();
		}
	}

	/**
	 * The <code>WebRequestDestinationFilter</code> class.
	 * 
	 * @author Cosmin Arad <cosmin@sics.se>
	 * @version $Id: WebRequestDestinationFilter.java 750 2009-04-02 09:55:01Z
	 *          Cosmin $
	 */
	private final static class WebRequestDestinationFilter extends
			ChannelFilter<WebRequest, Integer> {
		public WebRequestDestinationFilter(Integer destination) {
			super(WebRequest.class, destination, false);
		}

		public Integer getValue(WebRequest event) {
			return event.getDestination();
		}
	}

	/**
	 * TODO After implementing the <code>TChordPeer</code> component replace the
	 * "create" line with the commented line.
	 * 
	 * @param id
	 * @return
	 */
	private final Component createAndStartNewPeer(BigInteger id) {
		Component peer = create(CyclonPeer.class);
		// Component peer = create(TChordPeer.class);
		int peerId = ++peerIdSequence;
		Address peerAddress = new Address(peer0Address.getIp(), peer0Address
				.getPort(), peerId);

		connect(network, peer.getNegative(Network.class),
				new MessageDestinationFilter(peerAddress));
		connect(timer, peer.getNegative(Timer.class));
		connect(web, peer.getPositive(Web.class),
				new WebRequestDestinationFilter(peerId));

		subscribe(handleNeighbors, peer.getPositive(CyclonPeerPort.class));

		trigger(new CyclonPeerInit(peerAddress, bootstrapConfiguration,
				monitorConfiguration, cyclonConfiguration), peer.getControl());

		trigger(new Start(), peer.getControl());
		peers.put(id, peer);

		return peer;
	}

	private final void stopAndDestroyPeer(BigInteger id) {
		Component peer = peers.get(id);

		trigger(new Stop(), peer.getControl());

		unsubscribe(handleNeighbors, peer.getPositive(CyclonPeerPort.class));

		disconnect(network, peer.getNegative(Network.class));
		disconnect(timer, peer.getNegative(Timer.class));
		disconnect(web, peer.getPositive(Web.class));

		peers.remove(id);
		destroy(peer);
	}
}
