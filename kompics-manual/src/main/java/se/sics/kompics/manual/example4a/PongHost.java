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

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.manual.example1.Ping;
import se.sics.kompics.manual.example1.PingPort;
import se.sics.kompics.manual.example2a.Pong;
import se.sics.kompics.manual.example2b.PongPortReversed;

/**
 * The <code>PongHost</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: PongHost.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class PongHost extends ComponentDefinition {

	Positive<PingPort> posPing = positive(PingPort.class);
	Negative<PongPortReversed> negPong = negative(PongPortReversed.class);

	private int id;
	private int numPings = 0;

	/**
	 * Instantiates a new pong host.
	 */
	public PongHost() {
		subscribe(handleInit, control);
		subscribe(handleStart, control);
		subscribe(handlePong, negPong);
	}

	private Handler<PingPongInit> handleInit = new Handler<PingPongInit>() {
		public void handle(PingPongInit event) {
			id = event.getId();
			numPings = event.getNumPings();
		}
	};

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {

			for (int i = 0; i < numPings; i++) {
				System.out.println(id + ": sending ping " + i);
				trigger(new Ping(), posPing);
			}

		}
	};

	private Handler<Pong> handlePong = new Handler<Pong>() {
		public void handle(Pong event) {
			System.out.print(id + ": ");
			System.out.println("pong received.");
		}
	};

}
