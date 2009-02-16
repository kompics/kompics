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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.scheduler.WorkStealingScheduler;

/**
 * The <code>Kompics</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public final class Kompics {

	// TODO Deal with unneeded drop warning/implement needSet.
	// TODO port scheduler including Free List and spin-lock queue.
	// TODO BUG in execution PortCore.pickWork() returns null.

	public static Logger logger = LoggerFactory.getLogger("Kompics");

	private static boolean on = false;

	private static Scheduler scheduler;

	public static void setScheduler(Scheduler sched) {
		scheduler = sched;
	}
	
	public static Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Creates the and start.
	 * 
	 * @param main
	 *            the main
	 */
	public static void createAndStart(Class<? extends ComponentDefinition> main) {
		createAndStart(main, Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Creates the and start.
	 * 
	 * @param main
	 *            the main
	 * @param workers
	 *            the workers
	 */
	public static void createAndStart(
			Class<? extends ComponentDefinition> main, int workers) {
		if (on)
			throw new RuntimeException("Kompics already created");
		on = true;

		if (scheduler == null) {
			scheduler = new WorkStealingScheduler(workers);
		}

		try {
			ComponentDefinition mainComponent = main.newInstance();
			ComponentCore mainCore = mainComponent.getComponentCore();
			mainCore.setScheduler(scheduler);

			// start Main
			((PortCore<ControlPort>) mainCore.getControl()).doTrigger(
					Start.event, 0);
		} catch (InstantiationException e) {
			throw new RuntimeException("Cannot create main component "
					+ main.getCanonicalName(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot create main component "
					+ main.getCanonicalName(), e);
		}

		scheduler.proceed();
	}

	private Kompics() {
	}

	static void shutdown() {
		// TODO stop and destroy components
		on = false;
		scheduler = null;
	}

	/**
	 * Log stats.
	 */
	public static void logStats() {
		if (scheduler instanceof WorkStealingScheduler) {
			((WorkStealingScheduler) scheduler).logStats();
		}
	}
}
