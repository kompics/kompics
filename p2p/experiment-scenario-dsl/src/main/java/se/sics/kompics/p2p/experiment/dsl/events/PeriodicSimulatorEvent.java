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
package se.sics.kompics.p2p.experiment.dsl.events;

import se.sics.kompics.KompicsEvent;

/**
 * The <code>PeriodicSimulatorEvent</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class PeriodicSimulatorEvent extends KompicsSimulatorEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1740286333656694634L;
	
	private final long period;

	public PeriodicSimulatorEvent(KompicsEvent event, long time, long period) {
		super(event, time);
		this.period = period;
	}

	public final long getPeriod() {
		return period;
	}

	public final void setTime(long time) {
		super.setTime(time);
	}

	public final void setEvent(KompicsEvent event) {
		this.event = event;
	}
}
