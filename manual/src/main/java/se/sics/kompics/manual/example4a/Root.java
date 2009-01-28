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
package se.sics.kompics.manual.example4a;

import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.manual.example1.PingPort;
import se.sics.kompics.manual.example2b.PongPortReversed;

/**
 * The <code>Root</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Root.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class Root extends ComponentDefinition {

	private Component pingHost;
	private Component pongHost;

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		Kompics.createAndStart(Root.class);
	}

	/**
	 * Instantiates a new root.
	 */
	public Root() {
		pingHost = create(PingHost.class);
		pongHost = create(PongHost.class);

		subscribe(handleFault, pingHost.getControl());
		subscribe(handleFault, pongHost.getControl());
		subscribe(handleStart, control);

		// If the PingPongInit event is sent to pingHost and pongHost in
		// handleStart(),
		// then the startHandler of pingHost and pongHost will be called before
		// their
		// initHandler. Therefore, we send the init events, before the start
		// events are
		// called (at the end of this constructor).
		trigger(new PingPongInit(1, 10), pingHost.getControl());
		trigger(new PingPongInit(2, 10), pongHost.getControl());
	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {

			Positive<PingPort> pingPosPort = pingHost
					.getPositive(PingPort.class);
			Negative<PingPort> pingNegPort = pongHost
					.getNegative(PingPort.class);
			Channel<PingPort> x1 = connect(pingNegPort, pingPosPort);

			Positive<PongPortReversed> pongPosPort = pongHost
					.getPositive(PongPortReversed.class);
			Negative<PongPortReversed> pongNegPort = pingHost
					.getNegative(PongPortReversed.class);
			Channel<PongPortReversed> x2 = connect(pongNegPort, pongPosPort);

			x1.equals(x2);
		}
	};
	private Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault event) {
			System.out.println("Root Error: " + event.getFault().getMessage());
		}
	};

}