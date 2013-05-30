/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.p2p.peer.cyclon;

import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
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
import se.sics.kompics.p2p.monitor.cyclon.client.CyclonMonitorClient;
import se.sics.kompics.p2p.monitor.cyclon.client.CyclonMonitorClientInit;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorConfiguration;
import se.sics.kompics.p2p.overlay.cyclon.Cyclon;
import se.sics.kompics.p2p.overlay.cyclon.CyclonAddress;
import se.sics.kompics.p2p.overlay.cyclon.CyclonGetPeersRequest;
import se.sics.kompics.p2p.overlay.cyclon.CyclonGetPeersResponse;
import se.sics.kompics.p2p.overlay.cyclon.CyclonInit;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsRequest;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsResponse;
import se.sics.kompics.p2p.overlay.cyclon.CyclonPeerSampling;
import se.sics.kompics.p2p.overlay.cyclon.CyclonStatus;
import se.sics.kompics.p2p.overlay.cyclon.Join;
import se.sics.kompics.p2p.overlay.cyclon.JoinCompleted;
import se.sics.kompics.p2p.web.cyclon.CyclonWebApplication;
import se.sics.kompics.p2p.web.cyclon.CyclonWebApplicationInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;

/**
 * The
 * <code>CyclonPeer</code> class represents the peer component for Cyclon. It
 * contains the Cyclon component, which implements the Cyclon protocol, as well
 * as a bootstrap client component, a monitor client component, and a web
 * application component. It deals with bootstrapping of the peer (getting a set
 * of nodes already in the system) before initiating a Join request to the
 * Cyclon component.
 *
 * TODO You have to create a copy of this component, call it
 * <code>TChordPeer</code> that has the same subcomponent as this component,
 * plus a
 * <code>TChord</code> component, implementing the T-Chord protocol and a
 * <code>Chord</code> component initialized with the set of Chord neighbors
 * computed by T-Chord. The
 * <code>TChordPeer</code> component should implement the following flow of
 * events: (1) bootstrap (like this component), (2) ask the Cyclon component to
 * join a Cyclon overlay, (3) wait 15 Cyclon cycles so that Cyclon gets random
 * view, (4) start TChord (which uses Cyclon for initialization), (5) when
 * TChord signals termination, use the set of Chord neighbors output by TChord
 * to initialize and start the Chord component.
 *
 * Question: is step (3) necessary with joins based on random walks? Reply on
 * the mailing list if not already replied.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonPeer extends ComponentDefinition {

    Negative<CyclonPeerPort> cyclonPeer = negative(CyclonPeerPort.class);
    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    Negative<Web> web = negative(Web.class);
    private Component cyclon, bootstrap, monitor, webapp;
    private Address self;
    private CyclonAddress cyclonSelf;
    private int bootstrapRequestPeerCount;
    private boolean bootstrapped;
    private Logger logger;
    private BootstrapConfiguration bootstrapConfiguration;
    private CyclonMonitorConfiguration monitorConfiguration;

    public CyclonPeer(CyclonPeerInit init) {
        cyclon = create(Cyclon.class, new CyclonInit(init.getConfiguration()));
        bootstrap = create(BootstrapClient.class, new BootstrapClientInit(self, init
                .getBootstrapConfiguration()));
        monitor = create(CyclonMonitorClient.class, new CyclonMonitorClientInit(init.getMonitorConfiguration(),
                self));


        connect(network, cyclon.getNegative(Network.class));
        connect(network, bootstrap.getNegative(Network.class));
        connect(network, monitor.getNegative(Network.class));

        connect(timer, cyclon.getNegative(Timer.class));
        connect(timer, bootstrap.getNegative(Timer.class));
        connect(timer, monitor.getNegative(Timer.class));







        subscribe(handleJoin, cyclonPeer);
        subscribe(handleJoinCompleted, cyclon
                .getPositive(CyclonPeerSampling.class));
        subscribe(handleBootstrapResponse, bootstrap
                .getPositive(P2pBootstrap.class));
        subscribe(handlePeerRequest, cyclonPeer);
        subscribe(handlePeerResponse, cyclon
                .getPositive(CyclonPeerSampling.class));
        subscribe(handleNeighborsRequest, cyclonPeer);
        subscribe(handleNeighborsResponse, cyclon
                .getPositive(CyclonStatus.class));

        // INIT
        self = init.getSelf();

        logger = LoggerFactory.getLogger(getClass().getName() + "@"
                + self.getId());

        bootstrapRequestPeerCount = init.getConfiguration()
                .getBootstrapRequestPeerCount();
        bootstrapConfiguration = init.getBootstrapConfiguration();
        monitorConfiguration = init.getMonitorConfiguration();

    }
    Handler<JoinCyclon> handleJoin = new Handler<JoinCyclon>() {
        public void handle(JoinCyclon event) {
            cyclonSelf = new CyclonAddress(self, event.getCyclonId());

            webapp = create(CyclonWebApplication.class, new CyclonWebApplicationInit(cyclonSelf,
                    monitorConfiguration.getMonitorServerAddress(),
                    bootstrapConfiguration.getBootstrapServerAddress(),
                    monitorConfiguration.getClientWebPort()));
            connect(web, webapp.getPositive(Web.class));
            connect(cyclon.getPositive(CyclonStatus.class), webapp
                    .getNegative(CyclonStatus.class));
            connect(cyclon.getPositive(CyclonPeerSampling.class), webapp
                    .getNegative(CyclonPeerSampling.class));

            connect(cyclon.getPositive(CyclonStatus.class), monitor
                    .getNegative(CyclonStatus.class));

            BootstrapRequest request = new BootstrapRequest("Cyclon",
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

                LinkedList<CyclonAddress> cyclonInsiders = new LinkedList<CyclonAddress>();
                for (PeerEntry peerEntry : somePeers) {
                    cyclonInsiders.add((CyclonAddress) peerEntry
                            .getOverlayAddress());
                }
                trigger(new Join(cyclonSelf, cyclonInsiders), cyclon
                        .getPositive(CyclonPeerSampling.class));
                bootstrapped = true;
            }
        }
    };
    Handler<JoinCompleted> handleJoinCompleted = new Handler<JoinCompleted>() {
        public void handle(JoinCompleted event) {
            logger.debug("Join completed");

            // bootstrap completed
            trigger(new BootstrapCompleted("Cyclon", cyclonSelf), bootstrap
                    .getPositive(P2pBootstrap.class));
        }
    };
    Handler<CyclonGetPeersRequest> handlePeerRequest = new Handler<CyclonGetPeersRequest>() {
        public void handle(CyclonGetPeersRequest event) {
            trigger(event, cyclon.getPositive(CyclonPeerSampling.class));
        }
    };
    Handler<CyclonGetPeersResponse> handlePeerResponse = new Handler<CyclonGetPeersResponse>() {
        public void handle(CyclonGetPeersResponse event) {
            trigger(event, cyclonPeer);
        }
    };
    Handler<CyclonNeighborsRequest> handleNeighborsRequest = new Handler<CyclonNeighborsRequest>() {
        public void handle(CyclonNeighborsRequest event) {
            trigger(event, cyclon.getPositive(CyclonStatus.class));
        }
    };
    Handler<CyclonNeighborsResponse> handleNeighborsResponse = new Handler<CyclonNeighborsResponse>() {
        public void handle(CyclonNeighborsResponse event) {
            trigger(event, cyclonPeer);
        }
    };
}
