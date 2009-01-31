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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;

import org.junit.Test;

/**
 * The <code>CreateAndDestroyTest</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: CreateAndDestroyTest.java 466 2009-01-27 22:30:37Z cosmin $
 */
public class CreateAndDestroyTest {

	private static class TestRoot0 extends ComponentDefinition {
		public TestRoot0() {
			root0Created = true;
		}
	}

	private static boolean root0Created;

	/**
	 * Tests the creation of the main component.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testBootstrap() throws Exception {
		root0Created = false;
		Kompics.createAndStart(TestRoot0.class, 1);
		assertTrue(root0Created);
		Kompics.shutdown();
	}

	private static class TestRoot1 extends ComponentDefinition {
		public TestRoot1() {
			root1Created = true;
			create(TestComponent1.class);
		}
	}

	private static class TestComponent1 extends ComponentDefinition {
		public TestComponent1() {
			comp1Created = true;
		}
	}

	private static boolean root1Created;
	private static boolean comp1Created;

	/**
	 * Test the creation of a subcomponent.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCreate() throws Exception {
		root1Created = false;
		Kompics.createAndStart(TestRoot1.class, 1);
		assertTrue(root1Created);
		assertTrue(comp1Created);
		Kompics.shutdown();
	}

	private static class TestRoot2 extends ComponentDefinition {
		public TestRoot2() {
			root2Created = true;

			subscribe(startHandler, control);
		}

		Handler<Start> startHandler = new Handler<Start>() {
			public void handle(Start event) {
				root2Started = true;
				semaphore2.release();
			}
		};
	}

	private static boolean root2Created;
	private static boolean root2Started;
	private static Semaphore semaphore2;

	/**
	 * Tests that the main component is automatically started.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testStart() throws Exception {
		root2Created = false;
		root2Started = false;
		semaphore2 = new Semaphore(0);
		
		Kompics.createAndStart(TestRoot2.class, 1);
		
		semaphore2.acquire();
		assertTrue(root2Created);
		assertTrue(root2Started);
		Kompics.shutdown();
	}

	private static class TestRoot3 extends ComponentDefinition {
		public TestRoot3() {
			root3Created = true;

			create(TestComponent3.class);

			subscribe(startHandler, control);
		}

		Handler<Start> startHandler = new Handler<Start>() {
			public void handle(Start event) {
				root3Started = true;
				semaphore3.release();
			}
		};
	}

	private static class TestComponent3 extends ComponentDefinition {
		public TestComponent3() {
			comp3Created = true;

			subscribe(startHandler, control);
		}

		Handler<Start> startHandler = new Handler<Start>() {
			public void handle(Start event) {
				comp3Started = true;
				semaphore3.release();
			}
		};
	}

	private static boolean root3Created;
	private static boolean root3Started;
	private static boolean comp3Created;
	private static boolean comp3Started;
	private static Semaphore semaphore3;

	/**
	 * Tests that the main component is automatically started.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCreateAndStart() throws Exception {
		root3Created = false;
		root3Started = false;
		comp3Created = false;
		comp3Started = false;
		semaphore3 = new Semaphore(0);

		Kompics.createAndStart(TestRoot3.class, 1);

		semaphore3.acquire(2);
		assertTrue(root3Created);
		assertTrue(root3Started);
		assertTrue(comp3Created);
		assertTrue(comp3Started);
		Kompics.shutdown();
	}
}
