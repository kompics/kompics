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
package se.sics.kompics.p2p.bootstrap.server;

import java.util.Set;
import java.util.UUID;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.RewriteableMessage;
import se.sics.kompics.p2p.bootstrap.PeerEntry;

/**
 * The <code>CacheGetPeersResponse</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CacheGetPeersResponse extends RewriteableMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3661778191727187359L;

	private final Set<PeerEntry> peers;

	private final UUID requestId;

	public CacheGetPeersResponse(Set<PeerEntry> peers, UUID id, Address source,
			Address destination) {
		super(source, destination);
		this.peers = peers;
		this.requestId = id;
	}

	public Set<PeerEntry> getPeers() {
		return peers;
	}

	public UUID getRequestId() {
		return requestId;
	}

    @Override
    public Message rewriteSourceAddress(Address src)
    {
        Message msg = new CacheGetPeersResponse(this.getPeers(),
                this.getRequestId(),
                src,
                this.getDestination());
        return msg;
    }

    @Override
    public Message rewriteDestinationAddress(Address dest)
    {
        Message msg = new CacheGetPeersResponse(this.getPeers(),
                this.getRequestId(),
                this.getSource(),
                dest);
        return msg;
    }
}
