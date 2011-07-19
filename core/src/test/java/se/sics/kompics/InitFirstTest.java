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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * The <code>InitFirstTest</code> class tests component creation and start.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: InitFirstTest.java 739 2009-03-28 18:04:55Z Cosmin $
 */
@SuppressWarnings("unused")
@RunWith(Parameterized.class)
public class InitFirstTest {

	private static final int REPETITIONS = 1000;

	private static final int EVENTS = 20;

	@Parameters
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> params = new ArrayList<Object[]>();

		for (int i = 0; i < REPETITIONS; i++) {
			params.add(new Object[] { 0 });
		}
		return params;
	}

	public InitFirstTest(int param) {
		synchronized (this) {
			param = 1;
		}
	}

	private static Semaphore semaphore;

	// @Test(timeout = 5000)
	@Test
	public void testInitFirst() throws Exception {
		semaphore = new Semaphore(0);

		Kompics.createAndStart(TestRoot.class, 12);

		Component c3 = c2;

		// semaphore.acquire(EVENTS + 1);
		boolean ok = semaphore.tryAcquire(EVENTS + 1, 5, TimeUnit.SECONDS);
		Assert.assertTrue("Scheduling blocked", ok);
	}

	@After
	public void tearDown() {
		Kompics.shutdown();
	}

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

	public static Component c2;

	private static class TestRoot extends ComponentDefinition {
		public TestRoot() {
			final Component c = create(TestComponent.class);
			c2 = c;

			Thread t[] = new Thread[EVENTS], t1;
			for (int i = 0; i < EVENTS; i++) {
				final int j = i;
				t[i] = new Thread() {
					public void run() {
						trigger(new TestEvent(j), c.getPositive(TestPort.class));
					}
				};
			}
			t1 = new Thread() {
				public void run() {
					trigger(new TestInit(0), c.getControl());
				}
			};
			for (int i = 0; i < EVENTS; i++) {
				t[i].start();
			}
			t1.start();
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
				semaphore.release();

				// init runs only once
				Assert.assertFalse("Init already handled.", initDone);
				initDone = true;
			}
		};

		Handler<TestEvent> handleEvent = new Handler<TestEvent>() {
			public void handle(TestEvent event) {
				semaphore.release();

				// init runs before anything else
				Assert.assertTrue("Init was not first to run.", initDone);
			}
		};
	}
}
