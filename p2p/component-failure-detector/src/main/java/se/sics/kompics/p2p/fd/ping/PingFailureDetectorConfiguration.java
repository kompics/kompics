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
package se.sics.kompics.p2p.fd.ping;

import se.sics.kompics.Init;

/**
 * The <code>PingFailureDetectorConfiguration</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: PingFailureDetectorConfiguration.java 750 2009-04-02 09:55:01Z
 *          Cosmin $
 */
public final class PingFailureDetectorConfiguration extends Init {

	private final long livePeriod;
	private final long suspectedPeriod;
	private final long minRto;
	private final long timeoutPeriodIncrement;

	public PingFailureDetectorConfiguration(long livePeriod, long suspectedPeriod,
			long minRto, long timeoutPeriodIncrement) {
		this.livePeriod = livePeriod;
		this.suspectedPeriod = suspectedPeriod;
		this.minRto = minRto;
		this.timeoutPeriodIncrement = timeoutPeriodIncrement;
	}

	public final long getLivePeriod() {
		return livePeriod;
	}

	public final long getSuspectedPeriod() {
		return suspectedPeriod;
	}

	public final long getMinRto() {
		return minRto;
	}

	public final long getTimeoutPeriodIncrement() {
		return timeoutPeriodIncrement;
	}
}
