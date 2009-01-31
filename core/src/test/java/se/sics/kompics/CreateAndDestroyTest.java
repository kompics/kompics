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
	 * Test bootstrap.
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
	 * Test create.
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
}
