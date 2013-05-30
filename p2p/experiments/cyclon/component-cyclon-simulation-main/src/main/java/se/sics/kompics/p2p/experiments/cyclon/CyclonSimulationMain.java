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
package se.sics.kompics.p2p.experiments.cyclon;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkConfiguration;
import se.sics.kompics.network.model.king.KingLatencyMap;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServer;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServerInit;
import se.sics.kompics.p2p.experiment.cyclon.CyclonExperiment;
import se.sics.kompics.p2p.experiment.cyclon.CyclonSimulator;
import se.sics.kompics.p2p.experiment.cyclon.CyclonSimulatorInit;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorConfiguration;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorServer;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorServerInit;
import se.sics.kompics.p2p.overlay.cyclon.CyclonConfiguration;
import se.sics.kompics.p2p.simulator.P2pSimulator;
import se.sics.kompics.p2p.simulator.P2pSimulatorInit;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>CyclonSimulationMain</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonSimulationMain extends ComponentDefinition {

    static {
        PropertyConfigurator.configureAndWatch("log4j.properties");
    }
    private static SimulatorScheduler simulatorScheduler = new SimulatorScheduler();
    private static SimulationScenario scenario = SimulationScenario.load(System
            .getProperty("scenario"));

    public static void main(String[] args) {
        Kompics.setScheduler(simulatorScheduler);
        Kompics.createAndStart(CyclonSimulationMain.class, 1);
    }

    public CyclonSimulationMain() throws IOException {
        P2pSimulator.setSimulationPortType(CyclonExperiment.class);

        // loading component configurations
        final BootstrapConfiguration bootConfiguration = BootstrapConfiguration
                .load(System.getProperty("bootstrap.configuration"));
        final CyclonMonitorConfiguration monitorConfiguration = CyclonMonitorConfiguration
                .load(System.getProperty("cyclon.monitor.configuration"));
        final CyclonConfiguration cyclonConfiguration = CyclonConfiguration
                .load(System.getProperty("cyclon.configuration"));
        final NetworkConfiguration networkConfiguration = NetworkConfiguration
                .load(System.getProperty("network.configuration"));
        
        // create
        Component p2pSimulator = create(P2pSimulator.class, new P2pSimulatorInit(simulatorScheduler, scenario,
                new KingLatencyMap()));
        Component bootstrapServer = create(BootstrapServer.class, new BootstrapServerInit(bootConfiguration));
        Component monitorServer = create(CyclonMonitorServer.class, new CyclonMonitorServerInit(monitorConfiguration));
        Component cyclonSimulator = create(CyclonSimulator.class, new CyclonSimulatorInit(bootConfiguration,
                monitorConfiguration, cyclonConfiguration, networkConfiguration
                .getAddress()));

        



        final class MessageDestinationFilter extends ChannelFilter<Message, Address> {

            public MessageDestinationFilter(Address address) {
                super(Message.class, address, true);
            }

            public Address getValue(Message event) {
                return event.getDestination();
            }
        }

        // connect
        connect(bootstrapServer.getNegative(Network.class), p2pSimulator
                .getPositive(Network.class), new MessageDestinationFilter(
                bootConfiguration.getBootstrapServerAddress()));
        connect(bootstrapServer.getNegative(Timer.class), p2pSimulator
                .getPositive(Timer.class));

        connect(monitorServer.getNegative(Network.class), p2pSimulator
                .getPositive(Network.class), new MessageDestinationFilter(
                monitorConfiguration.getMonitorServerAddress()));
        connect(monitorServer.getNegative(Timer.class), p2pSimulator
                .getPositive(Timer.class));

        connect(cyclonSimulator.getNegative(Network.class), p2pSimulator
                .getPositive(Network.class));
        connect(cyclonSimulator.getNegative(Timer.class), p2pSimulator
                .getPositive(Timer.class));
        connect(cyclonSimulator.getNegative(CyclonExperiment.class),
                p2pSimulator.getPositive(CyclonExperiment.class));
    }
}
