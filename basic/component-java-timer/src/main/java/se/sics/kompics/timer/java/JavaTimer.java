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
package se.sics.kompics.timer.java;

import java.util.HashMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 * The <code>JavaTimer</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public final class JavaTimer extends ComponentDefinition {

	Negative<Timer> timer = negative(Timer.class);

	private static final Logger logger = LoggerFactory
			.getLogger(JavaTimer.class);

	// set of active timers
	private final HashMap<UUID, TimerSignalTask> activeTimers;

	// set of active periodic timers
	private final HashMap<UUID, PeriodicTimerSignalTask> activePeriodicTimers;

	private final java.util.Timer javaTimer;
	private final JavaTimer timerComponent;

	/**
	 * Instantiates a new java timer.
	 */
	public JavaTimer() {
		this.activeTimers = new HashMap<UUID, TimerSignalTask>();
		this.activePeriodicTimers = new HashMap<UUID, PeriodicTimerSignalTask>();
		this.javaTimer = new java.util.Timer("JavaTimer@"
				+ Integer.toHexString(this.hashCode()), true);
		timerComponent = this;

		subscribe(handleST, timer);
		subscribe(handleSPT, timer);
		subscribe(handleCT, timer);
		subscribe(handleCPT, timer);
	}

	Handler<ScheduleTimeout> handleST = new Handler<ScheduleTimeout>() {
		public void handle(ScheduleTimeout event) {
			UUID id = event.getTimeoutEvent().getTimeoutId();

			TimerSignalTask timeOutTask = new TimerSignalTask(timerComponent,
					event.getTimeoutEvent(), id);

			synchronized (activeTimers) {
				activeTimers.put(id, timeOutTask);
			}
			try {
				javaTimer.schedule(timeOutTask, event.getDelay());
				logger.debug("scheduled timer({}) {}", event.getDelay(),
						timeOutTask.timeout);
			} catch (IllegalStateException e) {
				logger.error("Could not schedule timer {}.", event.getDelay(),
						timeOutTask.timeout);
				e.printStackTrace();
			}
		}
	};

	Handler<SchedulePeriodicTimeout> handleSPT = new Handler<SchedulePeriodicTimeout>() {
		public void handle(SchedulePeriodicTimeout event) {
			UUID id = event.getTimeoutEvent().getTimeoutId();

			PeriodicTimerSignalTask timeOutTask = new PeriodicTimerSignalTask(
					event.getTimeoutEvent(), timerComponent);

			synchronized (activePeriodicTimers) {
				activePeriodicTimers.put(id, timeOutTask);
			}
			javaTimer.scheduleAtFixedRate(timeOutTask, event.getDelay(),
					event.getPeriod());
			logger.debug("scheduled periodic timer({}, {}) {}", new Object[] {
					event.getDelay(), event.getPeriod(), timeOutTask.timeout });
		}
	};

	Handler<CancelTimeout> handleCT = new Handler<CancelTimeout>() {
		public void handle(CancelTimeout event) {
			UUID id = event.getTimeoutId();

			TimerSignalTask task = null;
			synchronized (activeTimers) {
				task = activeTimers.get(id);
				if (task != null) {
					task.cancel();
					activeTimers.remove(id);
					logger.debug("canceled timer {}", task.timeout);
				}
			}
		}
	};

	Handler<CancelPeriodicTimeout> handleCPT = new Handler<CancelPeriodicTimeout>() {
		public void handle(CancelPeriodicTimeout event) {
			UUID id = event.getTimeoutId();

			PeriodicTimerSignalTask task = null;
			synchronized (activePeriodicTimers) {
				task = activePeriodicTimers.get(id);
				if (task != null) {
					task.cancel();
					activePeriodicTimers.remove(id);
					logger.debug("canceled periodic timer {}", task.timeout);
				}
			}
		}
	};

	// called by the timeout task
	/**
	 * Timeout.
	 * 
	 * @param timerId
	 *            the timer id
	 * @param timeout
	 *            the timeout
	 */
	final void timeout(UUID timerId, Timeout timeout) {
		synchronized (activeTimers) {
			activeTimers.remove(timerId);
		}
		logger.debug("trigger timeout {}", timeout);
		trigger(timeout, timer);
	}

	// called by the periodic timeout task
	/**
	 * Periodic timeout.
	 * 
	 * @param timeout
	 *            the timeout
	 */
	final void periodicTimeout(Timeout timeout) {
		logger.debug("trigger periodic timeout {}", timeout);
		trigger(timeout, timer);
	}
}
