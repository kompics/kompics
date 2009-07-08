/**
 * This file is part of the ID2210 course assignments kit.
 * 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.kompics.wan.master.plab.plc;

import se.sics.kompics.Init;
import se.sics.kompics.wan.master.plab.PlanetLabCredentials;

/**
 * The <code>BootstrapServerInit</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 */
public final class PLControllerInit extends Init {

	private final PlanetLabCredentials credentials;

	public PLControllerInit(PlanetLabCredentials credentials) {
		super();
		this.credentials = credentials;
	}

	/**
	 * @return the credentials
	 */
	public PlanetLabCredentials getCredentials() {
		return credentials;
	}

}
