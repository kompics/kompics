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
package se.sics.kompics.timer;

import se.sics.kompics.Request;

/**
 * The <code>SchedulePeriodicTimeout</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public final class SchedulePeriodicTimeout extends Request {

	private final long delay;

	private final long period;

	private Timeout timeout;

	/**
	 * Instantiates a new schedule periodic timeout.
	 * 
	 * @param delay
	 *            the delay
	 * @param period
	 *            the period
	 */
	public SchedulePeriodicTimeout(long delay, long period) {
		this.delay = delay;
		this.period = period;
	}

	/**
	 * Gets the delay.
	 * 
	 * @return the delay
	 */
	public final long getDelay() {
		return delay;
	}

	/**
	 * Gets the period.
	 * 
	 * @return the period
	 */
	public final long getPeriod() {
		return period;
	}

	/**
	 * Sets the timeout event.
	 * 
	 * @param timeout
	 *            the new timeout event
	 */
	public final void setTimeoutEvent(Timeout timeout) {
		this.timeout = timeout;
	}

	/**
	 * Gets the timeout event.
	 * 
	 * @return the timeout event
	 */
	public final Timeout getTimeoutEvent() {
		return timeout;
	}
}
