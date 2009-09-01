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
package se.sics.kompics.p2p.systems.chord.monitor.server;

import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorConfiguration;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorServer;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorServerInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.web.Web;
import se.sics.kompics.web.jetty.JettyWebServer;
import se.sics.kompics.web.jetty.JettyWebServerConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

/**
 * The <code>BootstrapServerMain</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ChordMonitorServerMain extends ComponentDefinition {
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	private static final Logger logger = LoggerFactory
			.getLogger(ChordMonitorServerMain.class);

	public static void main(String[] args) {
		Kompics.createAndStart(ChordMonitorServerMain.class, 1);
	}

	/**
	 * Instantiates a new chord monitor server main.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public ChordMonitorServerMain() throws IOException {
		// loading component configurations
		final ChordMonitorConfiguration chordMonitorConfiguration = ChordMonitorConfiguration
				.load(System.getProperty("chord.monitor.configuration"));
		final JettyWebServerConfiguration webConfiguration = JettyWebServerConfiguration
				.load(System.getProperty("jetty.web.configuration"));

		// creating components
		Component bootstrapServer = create(ChordMonitorServer.class);
		Component timer = create(JavaTimer.class);
		Component network = create(MinaNetwork.class);
		Component web = create(JettyWebServer.class);

		// initializing components
		trigger(new ChordMonitorServerInit(chordMonitorConfiguration),
				bootstrapServer.getControl());
		trigger(new MinaNetworkInit(chordMonitorConfiguration
				.getMonitorServerAddress(), 5), network.getControl());
		trigger(new JettyWebServerInit(webConfiguration), web.getControl());

		// connecting components
		connect(bootstrapServer.getNegative(Timer.class), timer
				.getPositive(Timer.class));
		connect(bootstrapServer.getNegative(Network.class), network
				.getPositive(Network.class));
		connect(bootstrapServer.getPositive(Web.class), web
				.getNegative(Web.class));

		logger.info("Started. Network={} Web={}", chordMonitorConfiguration
				.getMonitorServerAddress(), webConfiguration.getIp()
				.getHostAddress()
				+ ":" + webConfiguration.getPort());
	}
}
