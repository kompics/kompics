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

import java.util.LinkedList;

/**
 * The <code>StochasticProcessTerminatedEvent</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: StochasticProcessTerminatedEvent.java 750 2009-04-02 09:55:01Z
 *          Cosmin $
 */
public final class StochasticProcessTerminatedEvent extends SimulatorEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2104010717407824103L;

	private final LinkedList<StochasticProcessStartEvent> startEvents;
	private SimulationTerminatedEvent terminationEvent;
	private TakeSnapshotEvent snapshotEvent;
	private final String processName;

	public StochasticProcessTerminatedEvent(long time,
			LinkedList<StochasticProcessStartEvent> startEvents, String name) {
		super(time);
		this.startEvents = startEvents;
		this.processName = name;
	}

	public final LinkedList<StochasticProcessStartEvent> getStartEvents() {
		return startEvents;
	}

	public final void setTime(long time) {
		if (time > getTime()) {
			// only move time forward
			super.setTime(time);
		}
	}

	public String getProcessName() {
		return processName;
	}

	public SimulationTerminatedEvent getTerminationEvent() {
		return terminationEvent;
	}

	public void setTerminationEvent(SimulationTerminatedEvent terminationEvent) {
		this.terminationEvent = terminationEvent;
	}

	public TakeSnapshotEvent getSnapshotEvent() {
		return snapshotEvent;
	}

	public void setSnapshotEvent(TakeSnapshotEvent snapshotEvent) {
		this.snapshotEvent = snapshotEvent;
	}
}
