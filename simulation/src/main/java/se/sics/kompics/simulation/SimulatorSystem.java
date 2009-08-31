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
package se.sics.kompics.simulation;

/**
 * The <code>SimulatorSystem</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class SimulatorSystem {

	private static Simulator simulator = null;

	public static void setSimulator(Simulator s) {
		simulator = s;
	}

	// System.currentTimeMillis() redirected here
	public static long currentTimeMillis() {
		if (simulator != null) {
			return simulator.java_lang_System_currentTimeMillis();
		}
		throw new RuntimeException("No simulator is yet set to handle time");
	}

	// System.nanoTime() redirected here
	public static long nanoTime() {
		if (simulator != null) {
			return simulator.java_lang_System_nanoTime();
		}
		throw new RuntimeException("No simulator is yet set to handle time");
	}

	// Thread.sleep(long milliseconds) redirected here
	public static void sleep(long millis) {
		if (simulator != null) {
			simulator.java_lang_Thread_sleep(millis);
			return;
		}
		throw new RuntimeException("No simulator is yet set to handle time");
	}

	// Thread.sleep(long milliseconds, int nanoseconds) redirected here
	public static void sleep(long millis, int nanos) {
		if (simulator != null) {
			simulator.java_lang_Thread_sleep(millis, nanos);
			return;
		}
		throw new RuntimeException("No simulator is yet set to handle time");
	}

	// Thread.start() redirected here
	public static void start() {
		if (simulator != null) {
			simulator.java_lang_Thread_start();
			return;
		}
		throw new RuntimeException("No simulator is yet set. from "
				+ Thread.currentThread().getStackTrace()[2]);
	}

	// java.util.Random.next*() redirected here
	public static Object random() {
		throw new RuntimeException("Warning: simulated code generates random "
				+ "numbers. Make sure the seed is chosen deterministically.");
	}
}
