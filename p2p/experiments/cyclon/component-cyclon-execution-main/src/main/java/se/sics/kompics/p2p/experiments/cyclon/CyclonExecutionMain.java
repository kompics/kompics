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
import se.sics.kompics.p2p.orchestrator.P2pOrchestrator;
import se.sics.kompics.p2p.orchestrator.P2pOrchestratorInit;
import se.sics.kompics.p2p.overlay.cyclon.CyclonConfiguration;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.WebRequest;
import se.sics.kompics.web.jetty.JettyWebServer;
import se.sics.kompics.web.jetty.JettyWebServerConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

/**
 * The <code>CyclonExecutionMain</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonExecutionMain extends ComponentDefinition {
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	private static SimulationScenario scenario = SimulationScenario.load(System
			.getProperty("scenario"));

	public static void main(String[] args) {
		Kompics.createAndStart(CyclonExecutionMain.class, 1);
	}

	public CyclonExecutionMain() throws InterruptedException, IOException {
		P2pOrchestrator.setSimulationPortType(CyclonExperiment.class);
		// create
		Component p2pOrchestrator = create(P2pOrchestrator.class);
		Component jettyWebServer = create(JettyWebServer.class);
		Component bootstrapServer = create(BootstrapServer.class);
		Component monitorServer = create(CyclonMonitorServer.class);
		Component cyclonSimulator = create(CyclonSimulator.class);

		// loading component configurations
		final BootstrapConfiguration bootConfiguration = BootstrapConfiguration
				.load(System.getProperty("bootstrap.configuration"));
		final CyclonMonitorConfiguration monitorConfiguration = CyclonMonitorConfiguration
				.load(System.getProperty("cyclon.monitor.configuration"));
		final CyclonConfiguration cyclonConfiguration = CyclonConfiguration
				.load(System.getProperty("cyclon.configuration"));
		final JettyWebServerConfiguration webConfiguration = JettyWebServerConfiguration
				.load(System.getProperty("jetty.web.configuration"));
		final NetworkConfiguration networkConfiguration = NetworkConfiguration
				.load(System.getProperty("network.configuration"));

		System.out.println("For web access please go to " + "http://"
				+ webConfiguration.getIp().getHostAddress() + ":"
				+ webConfiguration.getPort() + "/");
		Thread.sleep(2000);

		trigger(new P2pOrchestratorInit(scenario, new KingLatencyMap()),
				p2pOrchestrator.getControl());
		trigger(new JettyWebServerInit(webConfiguration), jettyWebServer
				.getControl());
		trigger(new BootstrapServerInit(bootConfiguration), bootstrapServer
				.getControl());
		trigger(new CyclonMonitorServerInit(monitorConfiguration),
				monitorServer.getControl());
		trigger(new CyclonSimulatorInit(bootConfiguration,
				monitorConfiguration, cyclonConfiguration, networkConfiguration
						.getAddress()), cyclonSimulator.getControl());

		final class MessageDestinationFilter extends
				ChannelFilter<Message, Address> {
			public MessageDestinationFilter(Address address) {
				super(Message.class, address, true);
			}

			public Address getValue(Message event) {
				return event.getDestination();
			}
		}
		final class WebRequestDestinationFilter extends
				ChannelFilter<WebRequest, Integer> {
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

		connect(cyclonSimulator.getNegative(Network.class), p2pOrchestrator
				.getPositive(Network.class));
		connect(cyclonSimulator.getNegative(Timer.class), p2pOrchestrator
				.getPositive(Timer.class));
		connect(cyclonSimulator.getPositive(Web.class), jettyWebServer
				.getNegative(Web.class));
		connect(cyclonSimulator.getNegative(CyclonExperiment.class),
				p2pOrchestrator.getPositive(CyclonExperiment.class));
	}
}
