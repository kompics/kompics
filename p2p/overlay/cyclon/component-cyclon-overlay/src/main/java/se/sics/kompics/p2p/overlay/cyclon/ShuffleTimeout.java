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
package se.sics.kompics.p2p.overlay.cyclon;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * The <code>ShuffleTimeout</code> class represents a shuffle timeout. This
 * event is handled by Cyclon if a peer does not reply to a shuffle request in
 * due time.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ShuffleTimeout extends Timeout {

	private final CyclonAddress peer;

	public ShuffleTimeout(ScheduleTimeout request, CyclonAddress peer) {
		super(request);
		this.peer = peer;
	}

	/**
	 * @return the peer who did not reply within the shuffle timeout period.
	 */
	public CyclonAddress getPeer() {
		return peer;
	}
}
