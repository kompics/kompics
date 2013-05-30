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
package se.sics.kompics.p2p.experiments.chord;

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
import se.sics.kompics.p2p.experiment.chord.ChordExperiment;
import se.sics.kompics.p2p.experiment.chord.ChordSimulator;
import se.sics.kompics.p2p.experiment.chord.ChordSimulatorInit;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.fd.ping.PingFailureDetectorConfiguration;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorConfiguration;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorServer;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorServerInit;
import se.sics.kompics.p2p.orchestrator.P2pOrchestrator;
import se.sics.kompics.p2p.orchestrator.P2pOrchestratorInit;
import se.sics.kompics.p2p.overlay.chord.ChordConfiguration;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.jetty.JettyWebServer;
import se.sics.kompics.web.jetty.JettyWebServerConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

/**
 * The
 * <code>ChordExecutionMain</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordExecutionMain extends ComponentDefinition {

    static {
        PropertyConfigurator.configureAndWatch("log4j.properties");
    }
    private static SimulationScenario scenario = SimulationScenario.load(System
            .getProperty("scenario"));

    public static void main(String[] args) {
        Kompics.createAndStart(ChordExecutionMain.class, 8);
    }

    public ChordExecutionMain() throws InterruptedException, IOException {
        P2pOrchestrator.setSimulationPortType(ChordExperiment.class);


        // loading component configurations
        final BootstrapConfiguration bootConfiguration = BootstrapConfiguration
                .load(System.getProperty("bootstrap.configuration"));
        final ChordMonitorConfiguration monitorConfiguration = ChordMonitorConfiguration
                .load(System.getProperty("chord.monitor.configuration"));
        final PingFailureDetectorConfiguration fdConfiguration = PingFailureDetectorConfiguration
                .load(System.getProperty("ping.fd.configuration"));
        final ChordConfiguration chordConfiguration = ChordConfiguration
                .load(System.getProperty("chord.configuration"));
        final JettyWebServerConfiguration webConfiguration = JettyWebServerConfiguration
                .load(System.getProperty("jetty.web.configuration"));
        final NetworkConfiguration networkConfiguration = NetworkConfiguration
                .load(System.getProperty("network.configuration"));

        System.out.println("For web access please go to " + "http://"
                + webConfiguration.getIp().getHostAddress() + ":"
                + webConfiguration.getPort() + "/");


        // create
        Component p2pOrchestrator = create(P2pOrchestrator.class, new P2pOrchestratorInit(scenario, new KingLatencyMap()));
        Component jettyWebServer = create(JettyWebServer.class, new JettyWebServerInit(webConfiguration));
        Component bootstrapServer = create(BootstrapServer.class, new BootstrapServerInit(bootConfiguration));
        Component monitorServer = create(ChordMonitorServer.class, new ChordMonitorServerInit(monitorConfiguration));
        Component chordSimulator = create(ChordSimulator.class, new ChordSimulatorInit(bootConfiguration, monitorConfiguration,
                chordConfiguration, fdConfiguration, networkConfiguration
                .getAddress()));


        Thread.sleep(2000);

        final class MessageDestinationFilter extends ChannelFilter<Message, Address> {

            public MessageDestinationFilter(Address address) {
                super(Message.class, address, true);
            }

            public Address getValue(Message event) {
                return event.getDestination();
            }
        }
        final class WebRequestDestinationFilter extends ChannelFilter<WebRequest, Integer> {

            public WebRequestDestinationFilter(Integer destination) {
                super(WebRequest.class, destination, false);
            }

            public Integer getValue(WebRequest event) {
                return event.getDestination();
            }
        }

        // connect
        connect(bootstrapServer.getNegative(Network.class), p2pOrchestrator
                .getPositive(Network.class), new MessageDestinationFilter(
                bootConfiguration.getBootstrapServerAddress()));
        connect(bootstrapServer.getNegative(Timer.class), p2pOrchestrator
                .getPositive(Timer.class));
        connect(bootstrapServer.getPositive(Web.class), jettyWebServer
                .getNegative(Web.class), new WebRequestDestinationFilter(
                bootConfiguration.getBootstrapServerAddress().getId()));

        connect(monitorServer.getNegative(Network.class), p2pOrchestrator
                .getPositive(Network.class), new MessageDestinationFilter(
                monitorConfiguration.getMonitorServerAddress()));
        connect(monitorServer.getNegative(Timer.class), p2pOrchestrator
                .getPositive(Timer.class));
        connect(monitorServer.getPositive(Web.class), jettyWebServer
                .getNegative(Web.class), new WebRequestDestinationFilter(
                monitorConfiguration.getMonitorServerAddress().getId()));

        connect(chordSimulator.getNegative(Network.class), p2pOrchestrator
                .getPositive(Network.class));
        connect(chordSimulator.getNegative(Timer.class), p2pOrchestrator
                .getPositive(Timer.class));
        connect(chordSimulator.getPositive(Web.class), jettyWebServer
                .getNegative(Web.class));
        connect(chordSimulator.getNegative(ChordExperiment.class),
                p2pOrchestrator.getPositive(ChordExperiment.class));
    }
}
