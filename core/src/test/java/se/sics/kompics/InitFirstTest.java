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

import java.util.concurrent.Semaphore;

import org.junit.Assert;
import org.junit.Test;

/**
 * The <code>InitFirstTest</code> class tests component creation and start.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: InitFirstTest.java 739 2009-03-28 18:04:55Z Cosmin $
 */
@SuppressWarnings("unused")
public class InitFirstTest {

	public static class TestEvent extends Event {
		public int id;

		public TestEvent(int id) {
			this.id = id;
		}
	}

	public static class TestPort extends PortType {
		{
			negative(TestEvent.class);
		}
	}

	private static class TestInit extends Init {
		public int initVal;

		public TestInit(int initVal) {
			super();
			this.initVal = initVal;
		}
	}

	private static final int EVENTS = 1000;

	private static class TestRoot extends ComponentDefinition {
		public TestRoot() {
			Component c = create(TestComponent.class);

			for (int i = 0; i < EVENTS; i++) {
				trigger(new TestEvent(i), c.getPositive(TestPort.class));
			}
			trigger(new TestInit(0), c.getControl());
		}
	}

	private static class TestComponent extends ComponentDefinition {
		Negative<TestPort> testPort = negative(TestPort.class);

		private boolean initDone;

		public TestComponent() {
			initDone = false;

			subscribe(handleInit, control);
			subscribe(handleEvent, testPort);
		}

		Handler<TestInit> handleInit = new Handler<TestInit>() {
			public void handle(TestInit event) {
				Assert.assertFalse(initDone); // init runs only once
				initDone = true;
				semaphore.release();
			}
		};

		Handler<TestEvent> handleEvent = new Handler<TestEvent>() {
			public void handle(TestEvent event) {
				Assert.assertTrue(initDone); // init runs before anything else
				semaphore.release();
			}
		};
	}

	private static Semaphore semaphore;

	@Test
	public void testInitFirst() throws Exception {
		semaphore = new Semaphore(0);

		Kompics.createAndStart(TestRoot.class, 1);

		semaphore.acquire(EVENTS + 1);
		Kompics.shutdown();
	}
}
