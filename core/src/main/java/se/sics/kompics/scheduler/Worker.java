/**
 * This file is part of the Kompics component model runtime.
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
package se.sics.kompics.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import se.sics.kompics.ComponentCore;
import se.sics.kompics.SpinlockQueue;

/**
 * The <code>Worker</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class Worker extends Thread {

	private final WorkStealingScheduler scheduler;

	private final int wid;

	private final SpinlockQueue<ComponentCore> workQueue;

	private final AtomicInteger workCount;

	private final AtomicBoolean shouldQuit;

	int executionCount, workStealingCount, sleepCount;

	/**
	 * Instantiates a new worker.
	 * 
	 * @param scheduler
	 *            the scheduler
	 * @param wid
	 *            the wid
	 */
	public Worker(WorkStealingScheduler scheduler, int wid) {
		super();
		this.scheduler = scheduler;
		this.wid = wid;
		this.workQueue = new SpinlockQueue<ComponentCore>();
		this.workCount = new AtomicInteger(0);
		this.shouldQuit = new AtomicBoolean(false);
		super.setName("Kompics worker-" + wid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public final void run() {
		while (true) {
			try {
				boolean stillOn = executeComponent();
				if (!stillOn) {
					return;
				}
			} finally {
				// run forever
			}
		}
	}

	private final boolean executeComponent() {
		ComponentCore core = null;
		do {
			// try to do local work
			core = workQueue.poll();
			if (core == null) {
				// got no more work; should I quit?
				if (shouldQuit.get()) {
					return false;
				}

				// try to steal work from other workers
				workStealingCount++;
				core = scheduler.stealWork(wid);
				if (core == null) {
					// there is no work in the system
					sleepCount++;
					scheduler.waitForWork(this);
				}
			} else {
				workCount.decrementAndGet();
			}
		} while (core == null);

		executionCount++;
		scheduler.execute(core, wid);
		// core.execute(wid);

		return true;
	}

	final ComponentCore getWork() {
		ComponentCore core = workQueue.poll();
		if (core != null) {
			workCount.decrementAndGet();
		}
		return core;
	}

	final void addWork(ComponentCore core) {
		workQueue.offer(core);
		workCount.incrementAndGet();
	}

	/**
	 * Gets the work count.
	 * 
	 * @return the work count
	 */
	public final int getWorkCount() {
		return workCount.get();
	}

	/**
	 * Gets the wid.
	 * 
	 * @return the wid
	 */
	public final int getWid() {
		return wid;
	}

	final void quitWhenNoMoreWork() {
		shouldQuit.set(true);
	}
}
