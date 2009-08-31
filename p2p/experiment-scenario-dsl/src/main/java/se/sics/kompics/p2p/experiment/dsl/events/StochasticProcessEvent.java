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

import java.util.List;
import java.util.Random;

import se.sics.kompics.Event;
import se.sics.kompics.p2p.experiment.dsl.adaptor.OperationGenerator;
import se.sics.kompics.p2p.experiment.dsl.distribution.Distribution;

/**
 * The <code>StochasticProcessEvent</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class StochasticProcessEvent extends SimulatorEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 751562212455769648L;

	private final Distribution<Long> interArrivalTimeDistribution;
	private int currentCount;
	private final int count[];
	private final OperationGenerator operation[];
	private final StochasticProcessTerminatedEvent terminatedEvent;
	private final String processName;

	public StochasticProcessEvent(long time,
			Distribution<Long> interArrivalTimeDistribution,
			StochasticProcessTerminatedEvent terminatedEvent,
			List<OperationGenerator> operations, String name) {
		super(time);
		this.interArrivalTimeDistribution = interArrivalTimeDistribution;
		this.terminatedEvent = terminatedEvent;
		this.count = new int[operations.size()];
		this.operation = new OperationGenerator[operations.size()];
		currentCount = 0;
		int i = 0;
		for (OperationGenerator operationGenerator : operations) {
			operation[i] = operationGenerator;
			count[i] = operationGenerator.getCount();
			currentCount += count[i];
			i++;
		}
		this.processName = name;
	}

	public final void setTime(long time) {
		if (time > getTime()) {
			// only move time forward
			super.setTime(time);
		}
	}

	public final void setNextTime() {
		super.setTime(getTime() + interArrivalTimeDistribution.draw());
	}

	public final long getNextTime() {
		return interArrivalTimeDistribution.draw();
	}

	public int getCurrentCount() {
		return currentCount;
	}

	public Event generateOperation(Random random) {
		OperationGenerator generator = chooseOperation(random);
		return generator.generate();
	}

	public StochasticProcessTerminatedEvent getTerminatedEvent() {
		return terminatedEvent;
	}

	public String getProcessName() {
		return processName;
	}

	private OperationGenerator chooseOperation(Random random) {
		int r = random.nextInt(currentCount);
		int s = 0;
		for (int i = 0; i < count.length; i++) {
			if (count[i] == 0) {
				continue;
			}
			s += count[i];
			if (r < s) {
				count[i]--;
				currentCount--;
				return operation[i];
			}
		}
		return null;
	}
}
