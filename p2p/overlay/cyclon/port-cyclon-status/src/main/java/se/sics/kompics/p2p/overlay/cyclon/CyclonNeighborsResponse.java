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

import se.sics.kompics.Response;

/**
 * The <code>CyclonNeighborsResponse</code> class represents a response to a
 * <code>CyclonNeighborsRequest</code>, asking the Cyclon component for the
 * current set of Cyclon neighbors.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonNeighborsResponse extends Response {

	/**
	 * the current set of Cyclon neighbors, time-stamped (to show freshness).
	 */
	private final CyclonNeighbors neighbors;

	public CyclonNeighborsResponse(CyclonNeighborsRequest request,
			CyclonNeighbors neighbors) {
		super(request);
		this.neighbors = neighbors;
	}

	public CyclonNeighbors getNeighbors() {
		return neighbors;
	}
}
