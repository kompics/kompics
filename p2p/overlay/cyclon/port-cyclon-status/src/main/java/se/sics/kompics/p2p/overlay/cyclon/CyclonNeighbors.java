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

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The <code>CyclonNeighbors</code> class represents a set of Cyclon neighbors
 * which is time-stamped with its creation time.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonNeighbors implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1524921765310674054L;

	private final CyclonAddress self;

	/**
	 * the current Cyclon node descriptors found in a node's cache.
	 */
	private final ArrayList<CyclonNodeDescriptor> descriptors;

	private final long atTime;

	public CyclonNeighbors(CyclonAddress self,
			ArrayList<CyclonNodeDescriptor> descriptors) {
		super();
		this.self = self;
		this.descriptors = descriptors;
		this.atTime = System.currentTimeMillis();
	}

	/**
	 * @return the node who has these neighbors.
	 */
	public CyclonAddress getSelf() {
		return self;
	}

	/**
	 * @return the list of Cyclon node descriptors found this node's cache.
	 */
	public ArrayList<CyclonNodeDescriptor> getDescriptors() {
		return descriptors;
	}

	/**
	 * @return the creation time of this neighbor set.
	 */
	public long getAtTime() {
		return atTime;
	}
}
