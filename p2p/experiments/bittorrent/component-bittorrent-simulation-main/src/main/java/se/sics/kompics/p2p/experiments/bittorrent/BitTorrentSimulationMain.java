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
package se.sics.kompics.p2p.experiments.bittorrent;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.model.king.KingLatencyMap;
import se.sics.kompics.p2p.cdn.bittorrent.BitTorrentConfiguration;
import se.sics.kompics.p2p.cdn.bittorrent.TorrentMetadata;
import se.sics.kompics.p2p.cdn.bittorrent.tracker.BitTorrentTracker;
import se.sics.kompics.p2p.cdn.bittorrent.tracker.BitTorrentTrackerInit;
import se.sics.kompics.p2p.experiment.bittorrent.BitTorrentSimulator;
import se.sics.kompics.p2p.experiment.bittorrent.BitTorrentSimulatorInit;
import se.sics.kompics.p2p.experiment.bittorrent.BitTorrentSimulatorPort;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.simulator.P2pSimulator;
import se.sics.kompics.p2p.simulator.P2pSimulatorInit;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>BitTorrentSimulationMain</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: BitTorrentSimulationMain.java 1176 2009-09-02 15:10:34Z Cosmin
 * $
 */
public final class BitTorrentSimulationMain extends ComponentDefinition {

    static {
        PropertyConfigurator.configureAndWatch("log4j.properties");
    }
    private static SimulatorScheduler simulatorScheduler = new SimulatorScheduler();
    private static SimulationScenario scenario = SimulationScenario.load(System
            .getProperty("scenario"));

    public static void main(String[] args) {
        Kompics.setScheduler(simulatorScheduler);
        Kompics.createAndStart(BitTorrentSimulationMain.class, 1);
    }

    public BitTorrentSimulationMain() throws IOException {
        P2pSimulator.setSimulationPortType(BitTorrentSimulatorPort.class);

        // loading component configurations
        final BitTorrentConfiguration btConfiguration = BitTorrentConfiguration
                .load(System.getProperty("bittorrent.configuration"));
        final TorrentMetadata torrentMetadata = TorrentMetadata.load(System
                .getProperty("torrent.metadata"));

        // create
        Component p2pSimulator = create(P2pSimulator.class, new P2pSimulatorInit(simulatorScheduler, scenario,
                new KingLatencyMap()));
        Component btTracker = create(BitTorrentTracker.class, new BitTorrentTrackerInit(torrentMetadata.getTracker()));
        Component btSimulator = create(BitTorrentSimulator.class, new BitTorrentSimulatorInit(btConfiguration, torrentMetadata,
                btConfiguration.isSelfishPeers()));




        final class MessageDestinationFilter extends ChannelFilter<Message, Address> {

            public MessageDestinationFilter(Address address) {
                super(Message.class, address, true);
            }

            public Address getValue(Message event) {
                return event.getDestination();
            }
        }

        // connect
        connect(btTracker.getNegative(Network.class), p2pSimulator
                .getPositive(Network.class), new MessageDestinationFilter(
                torrentMetadata.getTracker().getPeerAddress()));
        connect(btTracker.getNegative(Timer.class), p2pSimulator
                .getPositive(Timer.class));

        connect(btSimulator.getNegative(Network.class), p2pSimulator
                .getPositive(Network.class));
        connect(btSimulator.getNegative(Timer.class), p2pSimulator
                .getPositive(Timer.class));
        connect(btSimulator.getNegative(BitTorrentSimulatorPort.class),
                p2pSimulator.getPositive(BitTorrentSimulatorPort.class));
    }
}
