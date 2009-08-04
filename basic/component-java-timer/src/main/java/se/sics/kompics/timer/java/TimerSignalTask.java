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

import java.util.TimerTask;
import java.util.UUID;

import se.sics.kompics.timer.Timeout;

/**
 * The <code>TimerSignalTask</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
final class TimerSignalTask extends TimerTask {

	final Timeout timeout;

	private final UUID timerId;

	private final JavaTimer timerComponent;
	
	/**
	 * Instantiates a new timer signal task.
	 * 
	 * @param timerComponent
	 *            the timer component
	 * @param timeout
	 *            the timeout
	 * @param timerId
	 *            the timer id
	 */
	TimerSignalTask(JavaTimer timerComponent, Timeout timeout,
			UUID timerId) {
		super();
		this.timerComponent = timerComponent;
		this.timeout = timeout;
		this.timerId = timerId;
	}

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public final void run() {
		timerComponent.timeout(timerId, timeout);
	}
}
