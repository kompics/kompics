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
package se.sics.kompics.p2p.web.cyclon;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.cyclon.CyclonAddress;

/**
 * The <code>CyclonWebApplicationInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonWebApplicationInit extends Init {

	private final Address monitorWebAddress, bootstrapWebAddress;

	private final CyclonAddress self;

	private final int webPort;

	public CyclonWebApplicationInit(CyclonAddress self,
			Address monitorWebAddress, Address bootstrapWebAddress, int webPort) {
		super();
		this.self = self;
		this.monitorWebAddress = monitorWebAddress;
		this.bootstrapWebAddress = bootstrapWebAddress;
		this.webPort = webPort;
	}

	public final CyclonAddress getSelf() {
		return self;
	}

	public final Address getBootstrapWebAddress() {
		return bootstrapWebAddress;
	}

	public final Address getMonitorWebAddress() {
		return monitorWebAddress;
	}

	public int getWebPort() {
		return webPort;
	}
}
