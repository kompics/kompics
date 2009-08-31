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
package se.sics.kompics.p2p.peer.chord;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.fd.ping.PingFailureDetectorConfiguration;
import se.sics.kompics.p2p.monitor.chord.server.ChordMonitorConfiguration;
import se.sics.kompics.p2p.overlay.chord.ChordConfiguration;

/**
 * The <code>ChordPeerInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class ChordPeerInit extends Init {

	private final Address self;
	private final BootstrapConfiguration bootstrapConfiguration;
	private final ChordMonitorConfiguration monitorConfiguration;
	private final ChordConfiguration chordConfiguration;
	private final PingFailureDetectorConfiguration fdConfiguration;

	public ChordPeerInit(Address self,
			BootstrapConfiguration bootstrapConfiguration,
			ChordMonitorConfiguration monitorConfiguration,
			ChordConfiguration chordConfiguration,
			PingFailureDetectorConfiguration fdConfiguration) {
		super();
		this.self = self;
		this.bootstrapConfiguration = bootstrapConfiguration;
		this.monitorConfiguration = monitorConfiguration;
		this.chordConfiguration = chordConfiguration;
		this.fdConfiguration = fdConfiguration;
	}

	public Address getSelf() {
		return self;
	}

	public BootstrapConfiguration getBootstrapConfiguration() {
		return bootstrapConfiguration;
	}

	public ChordMonitorConfiguration getMonitorConfiguration() {
		return monitorConfiguration;
	}

	public ChordConfiguration getChordConfiguration() {
		return chordConfiguration;
	}

	public PingFailureDetectorConfiguration getFdConfiguration() {
		return fdConfiguration;
	}
}
