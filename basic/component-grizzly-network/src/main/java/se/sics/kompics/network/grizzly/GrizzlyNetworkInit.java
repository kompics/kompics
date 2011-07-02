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
package se.sics.kompics.network.grizzly;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

/**
 * The <code>GrizzlyNetworkInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class GrizzlyNetworkInit extends Init {

	/** The self. */
	private final Address self;

	/** The connect retries. */
	private final int connectRetries;

	private final int compressionLevel;

	private final int initialBufferCapacity;
	private final int maxBufferCapacity;

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 *            the self
	 */
	public GrizzlyNetworkInit(Address self) {
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
	public GrizzlyNetworkInit(Address self, int connectRetries) {
		this(self, connectRetries, 0);
	}

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 * @param connectRetries
	 * @param compressionLevel
	 */
	public GrizzlyNetworkInit(Address self, int connectRetries,
			int compressionLevel) {
		this(self, connectRetries, compressionLevel, 2 * 1024, 16 * 1024);
	}

	public GrizzlyNetworkInit(Address self, int connectRetries,
			int compressionLevel, int initialBufferCapacity,
			int maxBufferCapacity) {
		super();
		this.self = self;
		this.connectRetries = connectRetries;
		this.compressionLevel = compressionLevel;
		this.initialBufferCapacity = initialBufferCapacity;
		this.maxBufferCapacity = maxBufferCapacity;
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

	public int getInitialBufferCapacity() {
		return initialBufferCapacity;
	}

	public int getMaxBufferCapacity() {
		return maxBufferCapacity;
	}
}
