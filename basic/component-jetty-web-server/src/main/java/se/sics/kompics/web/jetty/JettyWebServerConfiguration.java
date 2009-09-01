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
package se.sics.kompics.web.jetty;

import java.net.InetAddress;

import se.sics.kompics.Init;

/**
 * The <code>JettyWebServerConfiguration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class JettyWebServerConfiguration extends Init {

	private final InetAddress ip;
	private final int port;
	private final long requestTimeout;
	private final int maxThreads;
	private final String homePage;

	public JettyWebServerConfiguration(InetAddress ip, int port, long requestTimeout,
			int maxThreads, String homePage) {
		super();
		this.ip = ip;
		this.port = port;
		this.requestTimeout = requestTimeout;
		this.maxThreads = maxThreads;
		this.homePage = homePage;
	}

	public final InetAddress getIp() {
		return ip;
	}

	public final int getPort() {
		return port;
	}

	public final long getRequestTimeout() {
		return requestTimeout;
	}

	public int getMaxThreads() {
		return maxThreads;
	}
	
	public String getHomePage() {
		return homePage;
	}
}
