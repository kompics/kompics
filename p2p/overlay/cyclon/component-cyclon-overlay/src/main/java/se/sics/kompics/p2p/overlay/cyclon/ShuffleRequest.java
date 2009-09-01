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

import java.util.UUID;

/**
 * The <code>ShuffleRequest</code> class represents a shuffle request message
 * sent by a shuffle initiator node to a shuffle acceptor node during a shuffle.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ShuffleRequest extends CyclonMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8493601671018888143L;

	private final UUID requestId;

	private final DescriptorBuffer buffer;

	public ShuffleRequest(UUID requestId, DescriptorBuffer buffer,
			CyclonAddress source, CyclonAddress destination) {
		super(source, destination);
		this.requestId = requestId;
		this.buffer = buffer;
	}

	/**
	 * @return a unique identifier of this shuffle operation used also for
	 *         timeout.
	 */
	public UUID getRequestId() {
		return requestId;
	}

	/**
	 * @return the buffer of Cyclon node descriptors sent by the shuffle
	 *         initiator to the shuffle acceptor node.
	 */
	public DescriptorBuffer getBuffer() {
		return buffer;
	}
}
