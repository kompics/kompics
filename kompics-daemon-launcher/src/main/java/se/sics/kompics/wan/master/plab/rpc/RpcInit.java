/**
 * This file is part of the ID2210 course assignments kit.
 * 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.kompics.wan.master.plab.rpc;

import java.net.InetAddress;

import se.sics.kompics.Init;



/**
 * The <code>BootstrapServerInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 */
public final class RpcInit extends Init {

	private final InetAddress ip;
	private final int port;
	private final int requestTimeout;
	private final int maxThreads;
	private final String homepage;
	
	public RpcInit(InetAddress ip, int port, int requestTimeout,
			int maxThreads, String homepage) {
		super();
		this.ip = ip;
		this.port = port;
		this.requestTimeout = requestTimeout;
		this.maxThreads = maxThreads;
		this.homepage = homepage;
	}

	/**
	 * @return the ip
	 */
	public InetAddress getIp() {
		return ip;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return the requestTimeout
	 */
	public int getRequestTimeout() {
		return requestTimeout;
	}

	/**
	 * @return the maxThreads
	 */
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * @return the homepage
	 */
	public String getHomepage() {
		return homepage;
	}

}
