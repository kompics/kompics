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
package se.sics.kompics.p2p.systems.cyclon;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkConfiguration;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorConfiguration;
import se.sics.kompics.p2p.overlay.cyclon.CyclonConfiguration;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeer;
import se.sics.kompics.p2p.peer.cyclon.CyclonPeerInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.jetty.JettyWebServer;
import se.sics.kompics.web.jetty.JettyWebServerConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

/**
 * The <code>CyclonPeerMain</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonPeerMain extends ComponentDefinition {
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	private static final Logger logger = LoggerFactory
			.getLogger(CyclonPeerMain.class);

	public static void main(String[] args) {
		Kompics.createAndStart(CyclonPeerMain.class, 1);
	}

	public CyclonPeerMain() throws IOException {
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

		// creating components
		Component cyclonPeer = create(CyclonPeer.class);
		Component timer = create(JavaTimer.class);
		Component network = create(MinaNetwork.class);
		Component web = create(JettyWebServer.class);

		// initializing components
		trigger(new CyclonPeerInit(networkConfiguration.getAddress(),
				bootConfiguration, monitorConfiguration, cyclonConfiguration),
				cyclonPeer.getControl());
		trigger(new MinaNetworkInit(networkConfiguration.getAddress(), 5),
				network.getControl());
		trigger(new JettyWebServerInit(webConfiguration), web.getControl());

		// connecting components
		connect(cyclonPeer.getNegative(Timer.class), timer
				.getPositive(Timer.class));
		connect(cyclonPeer.getNegative(Network.class), network
				.getPositive(Network.class));
		connect(cyclonPeer.getPositive(Web.class), web.getNegative(Web.class));

		logger.info("Started. Network={} Web={}", networkConfiguration
				.getAddress(), webConfiguration.getIp().getHostAddress() + ":"
				+ webConfiguration.getPort());
	}
}
