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
package se.sics.kompics.manual.example4b;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;

/**
 * The <code>Root</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Root.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class Root extends ComponentDefinition {

	private Component host1;
	private Component host2;

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
		host1 = create(Host.class);
		host2 = create(Host.class);
		subscribe(handleStart, control);
		trigger(new PingPongInit(1), host1.getControl());
		trigger(new PingPongInit(2), host2.getControl());
	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			Negative<PingPongPort> ppNeg1 = host1
					.getNegative(PingPongPort.class);
			Negative<PingPongPort> ppNeg2 = host2
					.getNegative(PingPongPort.class);
			Positive<PingPongPort> ppPos1 = host1
					.getPositive(PingPongPort.class);
			Positive<PingPongPort> ppPos2 = host2
					.getPositive(PingPongPort.class);

			connect(ppNeg1, ppPos2);
			connect(ppNeg2, ppPos1);
		}
	};

}