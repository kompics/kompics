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
 * The <code>StochasticProcessStartEvent</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: StochasticProcessStartEvent.java 750 2009-04-02 09:55:01Z
 *          Cosmin $
 */
public final class StochasticProcessStartEvent extends SimulatorEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4299863622116185513L;

	private final LinkedList<StochasticProcessStartEvent> startEvents;
	private final StochasticProcessEvent stochasticEvent;
	private int waitFor;
	private final String processName;
	private final long delay;

	public StochasticProcessStartEvent(long time,
			LinkedList<StochasticProcessStartEvent> startEvents,
			StochasticProcessEvent stochasticEvent, int waitFor, String name) {
		super(time);
		this.delay = time;
		this.startEvents = startEvents;
		this.stochasticEvent = stochasticEvent;
		this.waitFor = waitFor;
		this.processName = name;
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

	public final long getDelay() {
		return delay;
	}
	
	public final LinkedList<StochasticProcessStartEvent> getStartEvents() {
		return startEvents;
	}

	public final StochasticProcessEvent getStochasticEvent() {
		return stochasticEvent;
	}

	public String getProcessName() {
		return processName;
	}
}
