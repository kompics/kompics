/**
 * This file is part of the Kompics component model runtime.
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
package se.sics.kompics.network;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Properties;
import se.sics.kompics.address.Address;

/**
 * The <code>NetworkConfiguration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: NetworkConfiguration.java 2836 2010-05-27 11:01:18Z Cosmin $
 */
public final class NetworkConfiguration {

	private final int multicastPort;

	private final Address address;

	public NetworkConfiguration(InetAddress ip, int port) {
		super();
		this.multicastPort = port + 1;
		this.address = new Address(ip, port, null);
	}

	public NetworkConfiguration(InetAddress ip, int port,
			int multicastPort) {
		super();
		this.multicastPort = multicastPort;
		this.address = new Address(ip, port, null);
	}

	public InetAddress getIp() {
		return address.getIp();
	}

	public int getPort() {
		return address.getPort();
	}

	public int getMulticastPort() {
		return multicastPort;
	}

	public Address getAddress() {
		return address;
	}

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("ip", "" + address.getIp().getHostAddress());
		p.setProperty("port", "" + address.getPort());
		p.setProperty("multicast.port", "" + multicastPort);

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.network");
	}

	public static NetworkConfiguration load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		InetAddress ip = InetAddress.getByName(p.getProperty("ip"));
		int port = Integer.parseInt(p.getProperty("port"));
		int multicastPort = Integer.parseInt(p.getProperty("multicast.port"));

		return new NetworkConfiguration(ip, port, multicastPort);
	}
}
