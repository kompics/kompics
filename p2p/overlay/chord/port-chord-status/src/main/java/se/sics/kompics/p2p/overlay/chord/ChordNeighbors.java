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
package se.sics.kompics.p2p.overlay.chord;

import java.io.Serializable;
import java.util.List;

/**
 * The <code>ChordNeighbors</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordNeighbors implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2725293559607263641L;

	private final ChordAddress self;

	private final ChordAddress predecessor;

	private final ChordAddress successor;

	private final List<ChordAddress> successorList;

	private final FingerTableView fingerTable;

	private final long atTime;

	public ChordNeighbors(ChordAddress localPeer, ChordAddress successor,
			ChordAddress predecessor, List<ChordAddress> successorList,
			FingerTableView fingerTable) {
		this.self = localPeer;
		this.predecessor = predecessor;
		this.successor = successor;
		this.successorList = successorList;
		this.fingerTable = fingerTable;
		this.atTime = System.currentTimeMillis();
	}

	public ChordAddress getLocalPeer() {
		return self;
	}

	public ChordAddress getSuccessorPeer() {
		return successor;
	}

	public ChordAddress getPredecessorPeer() {
		return predecessor;
	}

	public List<ChordAddress> getSuccessorList() {
		return successorList;
	}

	public FingerTableView getFingerTable() {
		return fingerTable;
	}

	public long getAtTime() {
		return atTime;
	}
}
