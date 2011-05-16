/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009-2011 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009-2011 Royal Institute of Technology (KTH)
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
package se.sics.kompics.network.netty;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

/**
 * The <code>NettyNetworkInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class NettyNetworkInit extends Init {

	/** The self. */
	private final Address self;

	/** The connect retries. */
	private final int connectRetries;

	private final int compressionLevel;

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 *            the self
	 */
	public NettyNetworkInit(Address self) {
		this(self, 0);
	}

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 *            the self
	 * @param connectRetries
	 *            the connect retries
	 */
	public NettyNetworkInit(Address self, int connectRetries) {
		super();
		this.self = self;
		this.connectRetries = connectRetries;
		this.compressionLevel = 9;
	}

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 *            the self
	 * @param connectRetries
	 *            the connect retries
	 */
	public NettyNetworkInit(Address self, int connectRetries,
			int compressionLevel) {
		super();
		this.self = self;
		this.connectRetries = connectRetries;
		this.compressionLevel = compressionLevel;
	}

	/**
	 * Gets the self.
	 * 
	 * @return the self
	 */
	public final Address getSelf() {
		return self;
	}

	/**
	 * Gets the connect retries.
	 * 
	 * @return the connect retries
	 */
	public int getConnectRetries() {
		return connectRetries;
	}
	
	public int getCompressionLevel() {
		return compressionLevel;
	}
}
