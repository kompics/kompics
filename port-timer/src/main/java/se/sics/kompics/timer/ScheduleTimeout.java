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
 * The <code>ScheduleTimeout</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: ScheduleTimeout.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public final class ScheduleTimeout extends Request {

	private final long delay;

	private Timeout timeout;

	/**
	 * Instantiates a new schedule timeout.
	 * 
	 * @param delay
	 *            the delay
	 */
	public ScheduleTimeout(long delay) {
		this.delay = delay;
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
