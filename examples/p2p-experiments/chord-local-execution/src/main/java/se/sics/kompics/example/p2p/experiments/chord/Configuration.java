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
package se.sics.kompics.example.p2p.experiments.chord;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
 * @version $Id$
 */
public class Configuration {
	public InetAddress ip = null;
	{
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
		}
	}
	int networkPort = 8081;
	int webPort = 8080;
	int bootId = Integer.MAX_VALUE;
	int monitorId = Integer.MAX_VALUE - 1;

	Address bootServerAddress = new Address(ip, networkPort, bootId);
	Address monitorServerAddress = new Address(ip, networkPort, monitorId);
	Address peer0Address = new Address(ip, networkPort, 0);

	int webRequestTimeout = 5000;
	int webThreads = 2;
	String webAddress = "http://" + ip.getHostAddress() + ":" + webPort + "/";
	String homePage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>"
			+ "<a href=\"" + webAddress + bootId + "/"
			+ "\">Bootstrap Server</a><br>" + "<a href=\"" + webAddress
			+ monitorId + "/" + "\">Monitor Server</a>";

	JettyWebServerConfiguration jettyWebServerConfiguration = new JettyWebServerConfiguration(
			ip, webPort, webRequestTimeout, webThreads, homePage);

	BootstrapConfiguration bootConfiguration = new BootstrapConfiguration(
			bootServerAddress, 60000, 4000, 3, 30000, webPort, webPort);

	ChordMonitorConfiguration monitorConfiguration = new ChordMonitorConfiguration(
			monitorServerAddress, 10000, 2000, webPort, webPort, Transport.TCP);

	PingFailureDetectorConfiguration fdConfiguration = new PingFailureDetectorConfiguration(
			1000, 5000, 1000, 0, Transport.TCP);

	ChordConfiguration chordConfiguration = new ChordConfiguration(13, 13,
			1000, 1000, 3000, 20);

	NetworkConfiguration networkConfiguration = new NetworkConfiguration(ip,
			networkPort, 0);

	public void set() throws IOException {
		String c = File.createTempFile("jetty.web.", ".conf").getAbsolutePath();
		jettyWebServerConfiguration.store(c);
		System.setProperty("jetty.web.configuration", c);

		c = File.createTempFile("bootstrap.", ".conf").getAbsolutePath();
		bootConfiguration.store(c);
		System.setProperty("bootstrap.configuration", c);

		c = File.createTempFile("chord.monitor.", ".conf").getAbsolutePath();
		monitorConfiguration.store(c);
		System.setProperty("chord.monitor.configuration", c);

		c = File.createTempFile("ping.fd.", ".conf").getAbsolutePath();
		fdConfiguration.store(c);
		System.setProperty("ping.fd.configuration", c);

		c = File.createTempFile("chord.", ".conf").getAbsolutePath();
		chordConfiguration.store(c);
		System.setProperty("chord.configuration", c);

		c = File.createTempFile("network.", ".conf").getAbsolutePath();
		networkConfiguration.store(c);
		System.setProperty("network.configuration", c);
	}
}
