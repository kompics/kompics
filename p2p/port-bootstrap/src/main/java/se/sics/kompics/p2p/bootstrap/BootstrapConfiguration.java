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

	public BootstrapConfiguration(Address bootstrapServerAddress,
			long cacheEvictAfter, long clientRetryPeriod, int clientRetryCount,
			long clientKeepAlivePeriod, int clientWebPort) {
		this.bootstrapServerAddress = bootstrapServerAddress;
		this.cacheEvictAfter = cacheEvictAfter;
		this.clientRetryPeriod = clientRetryPeriod;
		this.clientRetryCount = clientRetryCount;
		this.clientKeepAlivePeriod = clientKeepAlivePeriod;
		this.clientWebPort = clientWebPort;
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
}
