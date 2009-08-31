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
package se.sics.kompics.p2p.monitor.chord.server;

import java.util.UUID;

import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * The <code>OverlayViewEntry</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class OverlayViewEntry {

	private final OverlayAddress address;

	private long refreshedAt;

	private UUID evictionTimerId;

	private final long addedAt;

	public OverlayViewEntry(OverlayAddress address, long now, long addedAt) {
		this.address = address;
		this.refreshedAt = now;
		this.addedAt = addedAt;
	}

	public OverlayAddress getOverlayAddress() {
		return address;
	}

	public long getRefreshedAt() {
		return refreshedAt;
	}

	public void setRefreshedAt(long refreshedAt) {
		this.refreshedAt = refreshedAt;
	}

	public long getAddedAt() {
		return addedAt;
	}

	public UUID getEvictionTimerId() {
		return evictionTimerId;
	}

	public void setEvictionTimerId(UUID evictionTimerId) {
		this.evictionTimerId = evictionTimerId;
	}
}
