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

import se.sics.kompics.network.Message;

/**
 * The <code>CyclonMessage</code> class is a base class for all Cyclon messages.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public abstract class CyclonMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2404961198522692850L;

	private final CyclonAddress source;

	private final CyclonAddress destination;

	public CyclonMessage(CyclonAddress source, CyclonAddress destination) {
		super(source.getPeerAddress(), destination.getPeerAddress());
		this.source = source;
		this.destination = destination;
	}

	public final CyclonAddress getCyclonSource() {
		return source;
	}

	public final CyclonAddress getCyclonDestination() {
		return destination;
	}
}
