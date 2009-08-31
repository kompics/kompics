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

/**
 * The <code>TakeSnapshotEvent</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class TakeSnapshotEvent extends SimulatorEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3908945603058810039L;

	private final TakeSnapshot takeSnapshotEvent;

	private final long delay;

	private int waitFor;

	public TakeSnapshotEvent(long time, TakeSnapshot takeSnapshotEvent,
			int waitFor) {
		super(time);
		this.takeSnapshotEvent = takeSnapshotEvent;
		this.delay = time;
		this.waitFor = waitFor;
	}

	public final boolean shouldHandleNow() {
		waitFor--;
		return waitFor <= 0 ? true : false;
	}

	public final void setTime(long time) {
		time += delay;
		if (time > getTime()) {
			// only move time forward
			super.setTime(time);
		}
	}

	public TakeSnapshot getTakeSnapshotEvent() {
		return takeSnapshotEvent;
	}

	public long getDelay() {
		return delay;
	}
}
