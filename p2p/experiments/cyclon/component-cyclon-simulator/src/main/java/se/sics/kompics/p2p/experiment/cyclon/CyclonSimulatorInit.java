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
package se.sics.kompics.p2p.experiment.cyclon;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonMonitorConfiguration;
import se.sics.kompics.p2p.overlay.cyclon.CyclonConfiguration;

/**
 * The <code>CyclonSimulatorInit</code> class represents an Init event for the
 * CyclonSimulator component.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class CyclonSimulatorInit extends Init {

	private final BootstrapConfiguration bootstrapConfiguration;
	private final CyclonMonitorConfiguration monitorConfiguration;
	private final CyclonConfiguration cyclonConfiguration;

	private final Address peer0Address;

	public CyclonSimulatorInit(BootstrapConfiguration bootstrapConfiguration,
			CyclonMonitorConfiguration monitorConfiguration,
			CyclonConfiguration cyclonConfiguration, Address peer0Address) {
		super();
		this.bootstrapConfiguration = bootstrapConfiguration;
		this.monitorConfiguration = monitorConfiguration;
		this.cyclonConfiguration = cyclonConfiguration;
		this.peer0Address = peer0Address;
	}

	public BootstrapConfiguration getBootstrapConfiguration() {
		return bootstrapConfiguration;
	}

	public CyclonMonitorConfiguration getMonitorConfiguration() {
		return monitorConfiguration;
	}

	public CyclonConfiguration getCyclonConfiguration() {
		return cyclonConfiguration;
	}

	public Address getPeer0Address() {
		return peer0Address;
	}
}
