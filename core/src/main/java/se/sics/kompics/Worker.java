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
package se.sics.kompics;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The <code>Worker</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class Worker extends Thread {

	private final Scheduler scheduler;

	private final int wid;

	private final SpinlockQueue<ComponentCore> workQueue;
	
	private final AtomicInteger workCount;
	
	int executionCount, workStealingCount, sleepCount;

	/**
	 * Instantiates a new worker.
	 * 
	 * @param scheduler
	 *            the scheduler
	 * @param wid
	 *            the wid
	 */
	public Worker(Scheduler scheduler, int wid) {
		super();
		this.scheduler = scheduler;
		this.wid = wid;
		this.workQueue = new SpinlockQueue<ComponentCore>();
		this.workCount = new AtomicInteger(0);
		super.setName("Worker-" + wid);
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public final void run() {
		Kompics.logger.error("{} started", getName());
		while (true) {
			try {
				executeComponent();
			} finally {
				// run forever
			}
		}
	}

	private final void executeComponent() {
		ComponentCore core = null;
		do {
			// try to do local work
			core = workQueue.poll();
			if (core == null) {
				// try to steal work from other workers
				workStealingCount++;
				core = scheduler.stealWork(wid);
				if (core == null) {
					// there is no work in the system
					sleepCount++;
					scheduler.waitForWork(this);
				}
			}
		} while (core == null);

		executionCount++;
		core.execute(wid);
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
}
