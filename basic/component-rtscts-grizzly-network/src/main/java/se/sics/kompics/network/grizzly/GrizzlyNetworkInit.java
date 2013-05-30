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
 * @author Lars Kroll <lkroll@sics.se>
 * @version $Id: GrizzlyNetworkInit.java 4050 2012-03-30 14:32:18Z Cosmin $
 */
public class GrizzlyNetworkInit extends Init<GrizzlyNetwork> {

	/** The self. */
	private final Address self;

	/** The connect retries. */
	private final int connectRetries;

	private final int compressionLevel;

	private final int initialBufferCapacity;
	private final int maxBufferCapacity;
	private final int workerCount;
	private final int selectorCount;
        
        private final int numberOfChannels;
        private final QuotaAllocator allocator;

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 *            the self
	 */
	public GrizzlyNetworkInit(Address self, int numberOfChannels) {
		this(self, numberOfChannels, 0);
	}

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 *            the self
	 * @param connectRetries
	 *            the connect retries
	 */
	public GrizzlyNetworkInit(Address self, int numberOfChannels, int connectRetries) {
		this(self, numberOfChannels, connectRetries, 0);
	}

	/**
	 * Instantiates a new Netty network init.
	 * 
	 * @param self
	 * @param connectRetries
	 * @param compressionLevel
	 */
	public GrizzlyNetworkInit(Address self, int numberOfChannels, int connectRetries,
			int compressionLevel) {
		this(self, numberOfChannels, connectRetries, compressionLevel, 2 * 1024, 16 * 1024);
	}

	public GrizzlyNetworkInit(Address self, int numberOfChannels, int connectRetries,
			int compressionLevel, int initialBufferCapacity,
			int maxBufferCapacity) {
		this(self, numberOfChannels, connectRetries, compressionLevel, initialBufferCapacity,
				maxBufferCapacity, Runtime.getRuntime().availableProcessors(),
				Runtime.getRuntime().availableProcessors());
	}
        
        public GrizzlyNetworkInit(Address self, int numberOfChannels, int connectRetries,
			int compressionLevel, int initialBufferCapacity,
			int maxBufferCapacity, int workerCount, int selectorCount) {
            this(self, numberOfChannels, connectRetries, compressionLevel, initialBufferCapacity,
				maxBufferCapacity, workerCount, selectorCount, new ConstantQuotaAllocator(1));
        }

	public GrizzlyNetworkInit(Address self, int numberOfChannels, int connectRetries,
			int compressionLevel, int initialBufferCapacity,
			int maxBufferCapacity, int workerCount, int selectorCount, QuotaAllocator allocator) {
		super();
		this.self = self;
		this.connectRetries = connectRetries;
		this.compressionLevel = compressionLevel;
		this.initialBufferCapacity = initialBufferCapacity;
		this.maxBufferCapacity = maxBufferCapacity;
		this.workerCount = workerCount;
		this.selectorCount = selectorCount;
                this.numberOfChannels = numberOfChannels;
                this.allocator = allocator;
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

	public int getWorkerCount() {
		return workerCount;
	}

	public int getSelectorCount() {
		return selectorCount;
	}
        
        public int getNumberOfChannels() {
            return this.numberOfChannels;
        }
        
        public QuotaAllocator getAllocator() {
            return this.allocator;
        }
}
