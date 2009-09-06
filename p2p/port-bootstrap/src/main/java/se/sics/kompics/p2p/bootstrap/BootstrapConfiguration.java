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
package se.sics.kompics.p2p.bootstrap;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Properties;

import se.sics.kompics.address.Address;

/**
 * The <code>BootstrapConfiguration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class BootstrapConfiguration {

	private final Address bootstrapServerAddress;

	private final long cacheEvictAfter;

	private final long clientRetryPeriod;

	private final int clientRetryCount;

	private final long clientKeepAlivePeriod;

	private final int clientWebPort;

	private final int serverWebPort;

	public BootstrapConfiguration(Address bootstrapServerAddress,
			long cacheEvictAfter, long clientRetryPeriod, int clientRetryCount,
			long clientKeepAlivePeriod, int clientWebPort, int serverWebPort) {
		this.bootstrapServerAddress = bootstrapServerAddress;
		this.cacheEvictAfter = cacheEvictAfter;
		this.clientRetryPeriod = clientRetryPeriod;
		this.clientRetryCount = clientRetryCount;
		this.clientKeepAlivePeriod = clientKeepAlivePeriod;
		this.clientWebPort = clientWebPort;
		this.serverWebPort = serverWebPort;
	}

	public Address getBootstrapServerAddress() {
		return bootstrapServerAddress;
	}

	public long getCacheEvictAfter() {
		return cacheEvictAfter;
	}

	public long getClientRetryPeriod() {
		return clientRetryPeriod;
	}

	public int getClientRetryCount() {
		return clientRetryCount;
	}

	public long getClientKeepAlivePeriod() {
		return clientKeepAlivePeriod;
	}

	public int getClientWebPort() {
		return clientWebPort;
	}

	public int getServerWebPort() {
		return serverWebPort;
	}

	public void store(String file) throws IOException {
		Properties p = new Properties();
		p.setProperty("cache.evict.after", "" + cacheEvictAfter);
		p.setProperty("client.retry.period", "" + clientRetryPeriod);
		p.setProperty("client.retry.count", "" + clientRetryCount);
		p.setProperty("client.keepalive.period", "" + clientKeepAlivePeriod);
		p.setProperty("client.web.port", "" + clientWebPort);
		p.setProperty("server.web.port", "" + serverWebPort);
		p.setProperty("server.ip", ""
				+ bootstrapServerAddress.getIp().getHostAddress());
		p.setProperty("server.port", "" + bootstrapServerAddress.getPort());
		p.setProperty("server.id", "" + bootstrapServerAddress.getId());

		Writer writer = new FileWriter(file);
		p.store(writer, "se.sics.kompics.p2p.bootstrap");
	}

	public static BootstrapConfiguration load(String file) throws IOException {
		Properties p = new Properties();
		Reader reader = new FileReader(file);
		p.load(reader);

		InetAddress ip = InetAddress.getByName(p.getProperty("server.ip"));
		int port = Integer.parseInt(p.getProperty("server.port"));
		int id = Integer.parseInt(p.getProperty("server.id"));

		Address bootstrapServerAddress = new Address(ip, port, id);
		long cacheEvictAfter = Long.parseLong(p
				.getProperty("cache.evict.after"));
		long clientRetryPeriod = Long.parseLong(p
				.getProperty("client.retry.period"));
		int clientRetryCount = Integer.parseInt(p
				.getProperty("client.retry.count"));
		long clientKeepAlivePeriod = Long.parseLong(p
				.getProperty("client.keepalive.period"));
		int clientWebPort = Integer.parseInt(p.getProperty("client.web.port"));
		int serverWebPort = Integer.parseInt(p.getProperty("server.web.port"));

		return new BootstrapConfiguration(bootstrapServerAddress,
				cacheEvictAfter, clientRetryPeriod, clientRetryCount,
				clientKeepAlivePeriod, clientWebPort, serverWebPort);
	}
}
