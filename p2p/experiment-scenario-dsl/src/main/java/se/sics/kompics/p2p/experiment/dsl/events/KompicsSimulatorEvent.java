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
 * The <code>KompicsSimulatorEvent</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class KompicsSimulatorEvent extends SimulatorEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8405417898941931667L;

	protected transient KompicsEvent event;

	private boolean canceled = false;

	public boolean canceled() {
		return canceled;
	}

	public void cancel() {
		canceled = true;
	}

	public KompicsSimulatorEvent(KompicsEvent event, long time) {
		super(time);
		this.event = event;
	}

	public final KompicsEvent getEvent() {
		return event;
	}

	@Override
	public String toString() {
		return "KE@" + getTime() + ": " + event;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		KompicsSimulatorEvent other = (KompicsSimulatorEvent) obj;
		if (event == null) {
			if (other.event != null)
				return false;
		} else if (!event.equals(other.event))
			return false;
		return true;
	}
}
