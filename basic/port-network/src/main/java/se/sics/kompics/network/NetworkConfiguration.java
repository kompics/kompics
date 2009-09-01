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
 * @version $Id$
 */
public final class NetworkConfiguration {

	private final InetAddress ip;

	private final int port;

	private final int id;

	private final Address address;

	public NetworkConfiguration(InetAddress ip, int port, int id) {
		super();
		this.ip = ip;
		this.port = port;
		this.id = id;
		this.address = new Address(ip, port, id);
	}

	public InetAddress getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public int getId() {
		return id;
	}

	public Address getAddress() {
		return address;
	}

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("ip", "" + ip.getHostAddress());
		p.setProperty("port", "" + port);
		p.setProperty("id", "" + id);

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.network");
	}

	public static NetworkConfiguration load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		InetAddress ip = InetAddress.getByName(p.getProperty("ip"));
		int port = Integer.parseInt(p.getProperty("port"));
		int id = Integer.parseInt(p.getProperty("id"));

		return new NetworkConfiguration(ip, port, id);
	}
}
