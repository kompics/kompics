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
 * The <code>ShuffleResponse</code> class represents a shuffle response message
 * sent by a shuffle acceptor node back to a shuffle initiator node during a
 * shuffle, in response to a <code>ShuffleRequest</code> message.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ShuffleResponse extends CyclonMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5022051054665787770L;

	private final UUID requestId;

	private final DescriptorBuffer buffer;

	public ShuffleResponse(UUID requestId, DescriptorBuffer buffer,
			CyclonAddress source, CyclonAddress destination) {
		super(source, destination);
		this.requestId = requestId;
		this.buffer = buffer;
	}

	public UUID getRequestId() {
		return requestId;
	}

	public DescriptorBuffer getBuffer() {
		return buffer;
	}
}
