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
package se.sics.kompics.p2p.overlay.chord.router;

import se.sics.kompics.p2p.overlay.chord.ChordAddress;
import se.sics.kompics.p2p.overlay.chord.ChordLookupRequest;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * The <code>RpcTimeout</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class RpcTimeout extends Timeout {

	private final ChordLookupRequest rpcRequest;

	private final ChordAddress peer;
	
	private final boolean maintenance;

	public RpcTimeout(ScheduleTimeout st, ChordLookupRequest rpcRequest,
			ChordAddress peer, boolean maintenance) {
		super(st);
		this.rpcRequest = rpcRequest;
		this.peer = peer;
		this.maintenance = maintenance;
	}

	public ChordLookupRequest getRpcRequest() {
		return rpcRequest;
	}

	public ChordAddress getPeer() {
		return peer;
	}
	
	public boolean isMaintenance() {
		return maintenance;
	}
}
