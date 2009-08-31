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
package se.sics.kompics.p2p.peer.chord;

import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.bootstrap.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.BootstrapResponse;
import se.sics.kompics.p2p.bootstrap.P2pBootstrap;
import se.sics.kompics.p2p.bootstrap.PeerEntry;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClient;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClientInit;
import se.sics.kompics.p2p.fd.FailureDetector;
import se.sics.kompics.p2p.fd.ping.PingFailureDetector;
import se.sics.kompics.p2p.fd.ping.PingFailureDetectorInit;
import se.sics.kompics.p2p.fdstatus.FailureDetectorStatus;
import se.sics.kompics.p2p.monitor.chord.client.ChordMonitorClient;
import se.sics.kompics.p2p.monitor.chord.client.ChordMonitorClientInit;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorConfiguration;
import se.sics.kompics.p2p.overlay.chord.Chord;
import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.ChordInit;
import se.sics.kompics.p2p.overlay.chord.ChordLookupRequest;
import se.sics.kompics.p2p.overlay.chord.ChordLookupResponse;
import se.sics.kompics.p2p.overlay.chord.ChordNeighborsRequest;
import se.sics.kompics.p2p.overlay.chord.ChordNeighborsResponse;
import se.sics.kompics.p2p.overlay.chord.ChordStatus;
import se.sics.kompics.p2p.overlay.chord.ChordStructuredOverlay;
import se.sics.kompics.p2p.overlay.chord.CreateRing;
import se.sics.kompics.p2p.overlay.chord.JoinRing;
import se.sics.kompics.p2p.overlay.chord.JoinRingCompleted;
import se.sics.kompics.p2p.overlay.chord.LeaveRing;
import se.sics.kompics.p2p.overlay.chord.LeaveRingCompleted;
import se.sics.kompics.p2p.web.chord.ChordWebApplication;
import se.sics.kompics.p2p.web.chord.ChordWebApplicationInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;

/**
 * The <code>ChordPeer</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordPeer extends ComponentDefinition {

	Negative<ChordPeerPort> chordPeer = negative(ChordPeerPort.class);

	Positive<Network> network = positive(Network.class);
	Positive<Timer> timer = positive(Timer.class);
	Negative<Web> web = negative(Web.class);

	private Component chord, fd, bootstrap, monitor, webapp;
	private Address self;
	private ChordAddress chordSelf;

	private int bootstrapRequestPeerCount;
	private boolean bootstrapped;
	private Logger logger;

	private BootstrapConfiguration bootstrapConfiguration;
	private ChordMonitorConfiguration monitorConfiguration;

	public ChordPeer() {
		chord = create(Chord.class);
		fd = create(PingFailureDetector.class);
		bootstrap = create(BootstrapClient.class);
		monitor = create(ChordMonitorClient.class);
		webapp = create(ChordWebApplication.class);

		connect(network, chord.getNegative(Network.class));
		connect(network, fd.getNegative(Network.class));
		connect(network, bootstrap.getNegative(Network.class));
		connect(network, monitor.getNegative(Network.class));

		connect(timer, chord.getNegative(Timer.class));
		connect(timer, fd.getNegative(Timer.class));
		connect(timer, bootstrap.getNegative(Timer.class));
		connect(timer, monitor.getNegative(Timer.class));

		connect(web, webapp.getPositive(Web.class));

		connect(chord.getNegative(FailureDetector.class), fd
				.getPositive(FailureDetector.class));

		connect(chord.getPositive(ChordStatus.class), webapp
				.getNegative(ChordStatus.class));
		connect(chord.getPositive(ChordStructuredOverlay.class), webapp
				.getNegative(ChordStructuredOverlay.class));

		connect(fd.getPositive(FailureDetectorStatus.class), webapp
				.getNegative(FailureDetectorStatus.class));

		connect(chord.getPositive(ChordStatus.class), monitor
				.getNegative(ChordStatus.class));

		subscribe(handleInit, control);

		subscribe(handleJoin, chordPeer);
		subscribe(handleJoinRingCompleted, chord
				.getPositive(ChordStructuredOverlay.class));
		subscribe(handleBootstrapResponse, bootstrap
				.getPositive(P2pBootstrap.class));
		subscribe(handleLeave, chordPeer);
		subscribe(handleLeaveDone, chord
				.getPositive(ChordStructuredOverlay.class));
		subscribe(handleLookupRequest, chordPeer);
		subscribe(handleLookupResponse, chord
				.getPositive(ChordStructuredOverlay.class));
		subscribe(handleNeighborsRequest, chordPeer);
		subscribe(handleNeighborsResponse, chord.getPositive(ChordStatus.class));
	}

	Handler<ChordPeerInit> handleInit = new Handler<ChordPeerInit>() {
		public void handle(ChordPeerInit init) {
			self = init.getSelf();

			logger = LoggerFactory.getLogger(getClass().getName() + "@"
					+ self.getId());

			bootstrapRequestPeerCount = init.getChordConfiguration()
					.getBootstrapRequestPeerCount();
			bootstrapConfiguration = init.getBootstrapConfiguration();
			monitorConfiguration = init.getMonitorConfiguration();

			trigger(new ChordInit(init.getChordConfiguration()
					.getLog2RingSize(), init.getChordConfiguration()
					.getSuccessorListLength(), init.getChordConfiguration()
					.getSuccessorStabilizationPeriod(), init
					.getChordConfiguration().getFingerStabilizationPeriod(),
					init.getChordConfiguration().getRpcTimeout()), chord
					.getControl());
			trigger(new PingFailureDetectorInit(self, init.getFdConfiguration()),
					fd.getControl());
			trigger(new BootstrapClientInit(self, init
					.getBootstrapConfiguration()), bootstrap.getControl());
			trigger(new ChordMonitorClientInit(init.getMonitorConfiguration(),
					self), monitor.getControl());
		}
	};

	Handler<JoinChordRing> handleJoin = new Handler<JoinChordRing>() {
		public void handle(JoinChordRing event) {
			// logger.debug("JOIN CHORD RING");

			chordSelf = new ChordAddress(self, event.getNodeKey());

			trigger(new ChordWebApplicationInit(chordSelf, monitorConfiguration
					.getMonitorServerAddress(), bootstrapConfiguration
					.getBootstrapServerAddress(), monitorConfiguration
					.getClientWebPort()), webapp.getControl());

			BootstrapRequest request = new BootstrapRequest("Chord",
					bootstrapRequestPeerCount);
			trigger(request, bootstrap.getPositive(P2pBootstrap.class));

			// Join or create are triggered on BootstrapResponse
		}
	};

	Handler<BootstrapResponse> handleBootstrapResponse = new Handler<BootstrapResponse>() {
		public void handle(BootstrapResponse event) {
			if (!bootstrapped) {
				logger.debug("Got BoostrapResponse {}, Bootstrap complete",
						event.getPeers().size());

				Set<PeerEntry> somePeers = event.getPeers();

				if (somePeers.size() > 0) {
					LinkedList<ChordAddress> chordInsiders = new LinkedList<ChordAddress>();
					for (PeerEntry peerEntry : somePeers) {
						chordInsiders.add((ChordAddress) peerEntry
								.getOverlayAddress());
					}
					trigger(new JoinRing(chordSelf, chordInsiders), chord
							.getPositive(ChordStructuredOverlay.class));
				} else {
					// we create a new ring
					trigger(new CreateRing(chordSelf), chord
							.getPositive(ChordStructuredOverlay.class));
				}
				bootstrapped = true;
			}
		}
	};

	Handler<JoinRingCompleted> handleJoinRingCompleted = new Handler<JoinRingCompleted>() {
		public void handle(JoinRingCompleted event) {
			logger.debug("JoinRing completed");

			// bootstrap completed
			trigger(new BootstrapCompleted("Chord", chordSelf), bootstrap
					.getPositive(P2pBootstrap.class));
		}
	};

	Handler<LeaveChordRing> handleLeave = new Handler<LeaveChordRing>() {
		public void handle(LeaveChordRing event) {
			// System.err.println("LEAVE CHORD RING @ " + self);
			trigger(new LeaveRing(), chord
					.getPositive(ChordStructuredOverlay.class));
		}
	};
	Handler<LeaveRingCompleted> handleLeaveDone = new Handler<LeaveRingCompleted>() {
		public void handle(LeaveRingCompleted event) {
			// System.err.println("CHORD LEAVE DONE @ " + self);
			// trigger(new LeaveRing(),
			// chord.getPositive(ChordStructuredOverlay.class));
		}
	};

	Handler<ChordLookupRequest> handleLookupRequest = new Handler<ChordLookupRequest>() {
		public void handle(ChordLookupRequest event) {
			// System.err.println("LOOKUP_REQUEST");
			trigger(event, chord.getPositive(ChordStructuredOverlay.class));
		}
	};

	Handler<ChordLookupResponse> handleLookupResponse = new Handler<ChordLookupResponse>() {
		public void handle(ChordLookupResponse event) {
			// System.err.println("LOOKUP_RESPONSE");
			trigger(event, chordPeer);
		}
	};

	Handler<ChordNeighborsRequest> handleNeighborsRequest = new Handler<ChordNeighborsRequest>() {
		public void handle(ChordNeighborsRequest event) {
			// System.err.println("NEIGHBORS_REQUEST");
			trigger(event, chord.getPositive(ChordStatus.class));
		}
	};

	Handler<ChordNeighborsResponse> handleNeighborsResponse = new Handler<ChordNeighborsResponse>() {
		public void handle(ChordNeighborsResponse event) {
			// System.err.println("LOOKUP_RESPONSE");
			trigger(event, chordPeer);
		}
	};
}
