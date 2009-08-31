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
package se.sics.kompics.p2p.simulator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;

import se.sics.kompics.p2p.experiment.dsl.events.SimulatorEvent;

/**
 * The <code>FutureEventList</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class FutureEventList {

	private PriorityQueue<SimulatorEvent> futureEventList;

	public FutureEventList() {
		futureEventList = new PriorityQueue<SimulatorEvent>();
	}

	void scheduleFutureEvent(long now, SimulatorEvent event) {
		if (event.getTime() < now) {
			throw new RuntimeException("Cannot schedule an event in the past");
		}
		futureEventList.add(event);

		event.setOnList(true);

		// System.err.print(now + ": FEL ADDED: " + event.getTime());
		// dumpFEL();
	}

	boolean cancelFutureEvent(long now, SimulatorEvent event) {
		if (event == null) {
			throw new RuntimeException("Cannot cancel a null event");
		}

		boolean removed = futureEventList.remove(event);

		if (removed)
			event.setOnList(false);

		// System.err.print(now + ": FEL CANCELED (" + (removed ? "OK" : "NOK")
		// + "): " + event.getTime());
		// dumpFEL();
		return removed;
	}

	boolean hasMoreEventsAtTime(long now) {
		SimulatorEvent event = futureEventList.peek();
		if (event != null) {
			return (event.getTime() == now);
		}
		return false;
	}

	SimulatorEvent getAndRemoveFirstEvent(long now) {
		SimulatorEvent event = futureEventList.poll();
		// System.err.print(now + ": FEL CONSUMED: " + event.getTime());
		// dumpFEL();

		if (event != null) {
			event.setOnList(false);
		}

		return event;
	}

	void dumpFEL() {
		System.err.print(". FEL(" + futureEventList.size() + "): ");
		LinkedList<Long> times = new LinkedList<Long>();

		for (SimulatorEvent simulatorEvent : futureEventList) {
			times.add(simulatorEvent.getTime());
		}
		Collections.sort(times);

		for (Long long1 : times) {
			System.err.print(long1 + ", ");
		}
		System.err.println();
		System.err.flush();
	}
}
