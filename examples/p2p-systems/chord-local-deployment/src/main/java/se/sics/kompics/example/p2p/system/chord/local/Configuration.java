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
package se.sics.kompics.example.p2p.system.chord.local;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.NetworkConfiguration;
import se.sics.kompics.network.Transport;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.fd.ping.PingFailureDetectorConfiguration;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorConfiguration;
import se.sics.kompics.p2p.overlay.chord.ChordConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerConfiguration;

/**
 * The <code>Configuration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Configuration.java 1150 2009-09-02 00:00:40Z Cosmin $
 */
public class Configuration {

	final int networkPort;
	final int webPort;

	final int bootNetPort = 7005;
	final int bootWebPort = 7004;
	final int monitorNetPort = 7007;
	final int monitorWebPort = 7006;

	JettyWebServerConfiguration jettyWebServerConfiguration;
	BootstrapConfiguration bootConfiguration;
	ChordMonitorConfiguration monitorConfiguration;
	PingFailureDetectorConfiguration fdConfiguration;
	ChordConfiguration chordConfiguration;
	NetworkConfiguration networkConfiguration;

	public Configuration(int networkPort) {
		super();
		this.networkPort = networkPort;
		this.webPort = networkPort - 1;
		InetAddress ip = null;
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
		}
		int bootId = Integer.MAX_VALUE;
		int monitorId = Integer.MAX_VALUE - 1;

		Address bootServerAddress = new Address(ip, bootNetPort, bootId);
		Address monitorServerAddress = new Address(ip, monitorNetPort,
				monitorId);

		int webRequestTimeout = 5000;
		int webThreads = 2;

		String bootWebAddress = "http://" + ip.getHostAddress() + ":"
				+ bootWebPort + "/";
		String monitorWebAddress = "http://" + ip.getHostAddress() + ":"
				+ monitorWebPort + "/";

		String homePage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>"
				+ "<a href=\""
				+ bootWebAddress
				+ bootId
				+ "/"
				+ "\">Bootstrap Server</a><br>"
				+ "<a href=\""
				+ monitorWebAddress + monitorId + "/" + "\">Monitor Server</a>";

		jettyWebServerConfiguration = new JettyWebServerConfiguration(ip,
				webPort, webRequestTimeout, webThreads, homePage);

		bootConfiguration = new BootstrapConfiguration(bootServerAddress,
				60000, 4000, 3, 30000, webPort, bootWebPort);

		monitorConfiguration = new ChordMonitorConfiguration(
				monitorServerAddress, 10000, 1000, webPort, monitorWebPort,
				Transport.TCP);

		fdConfiguration = new PingFailureDetectorConfiguration(1000, 5000,
				10000, 1000, Transport.TCP);

		chordConfiguration = new ChordConfiguration(13, 13, 1000, 1000, 3000,
				20);

		networkConfiguration = new NetworkConfiguration(ip, networkPort, 0);
	}

	public Properties set() throws IOException {
		Properties p = new Properties();
		String c = File.createTempFile("jetty.web.", ".conf").getAbsolutePath();
		jettyWebServerConfiguration.store(c);
		System.setProperty("jetty.web.configuration", c);
		p.setProperty("jetty.web.configuration", c);

		c = File.createTempFile("bootstrap.", ".conf").getAbsolutePath();
		bootConfiguration.store(c);
		System.setProperty("bootstrap.configuration", c);
		p.setProperty("bootstrap.configuration", c);

		c = File.createTempFile("chord.monitor.", ".conf").getAbsolutePath();
		monitorConfiguration.store(c);
		System.setProperty("chord.monitor.configuration", c);
		p.setProperty("chord.monitor.configuration", c);

		c = File.createTempFile("ping.fd.", ".conf").getAbsolutePath();
		fdConfiguration.store(c);
		System.setProperty("ping.fd.configuration", c);
		p.setProperty("ping.fd.configuration", c);

		c = File.createTempFile("chord.", ".conf").getAbsolutePath();
		chordConfiguration.store(c);
		System.setProperty("chord.configuration", c);
		p.setProperty("chord.configuration", c);

		c = File.createTempFile("network.", ".conf").getAbsolutePath();
		networkConfiguration.store(c);
		System.setProperty("network.configuration", c);
		p.setProperty("network.configuration", c);

		return p;
	}
}
