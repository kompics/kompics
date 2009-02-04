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
import se.sics.kompics.manual.example1.Ping;
import se.sics.kompics.manual.example1.PingPort;
import se.sics.kompics.manual.example2a.Pong;
import se.sics.kompics.manual.example2b.PongPortReversed;

/**
 * The <code>PingHost</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: PingHost.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class PingHost extends ComponentDefinition {

	Negative<PingPort> negPing = negative(PingPort.class);
	Positive<PongPortReversed> posPong = positive(PongPortReversed.class);

	private int id;

	/**
	 * Instantiates a new ping host.
	 */
	public PingHost() {
		subscribe(handleInit, control);
		subscribe(handlePing, negPing);
	}

	private Handler<PingPongInit> handleInit = new Handler<PingPongInit>() {
		public void handle(PingPongInit event) {
			id = event.getId();
		}
	};

	private Handler<Ping> handlePing = new Handler<Ping>() {
		public void handle(Ping event) {
			System.out.print(id + ": ");
			System.out.println("Received ping. Sending a Pong..");
			trigger(new Pong(), posPong);
		}
	};

}
