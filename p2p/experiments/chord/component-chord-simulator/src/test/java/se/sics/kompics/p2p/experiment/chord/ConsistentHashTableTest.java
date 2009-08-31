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
package se.sics.kompics.p2p.experiment.chord;

import java.math.BigInteger;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * The <code>ConsistentHashTableTest</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ConsistentHashTableTest {

	ConsistentHashtable<BigInteger> table = new ConsistentHashtable<BigInteger>();
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testConsistentTable() {
		table.addNode(new BigInteger("1111"));
		table.addNode(new BigInteger("1"));
		
		BigInteger node1 = table.getNode(new BigInteger("0"));
		BigInteger node2 = table.getNode(new BigInteger("10"));
		BigInteger node3 = table.getNode(new BigInteger("1111"));
		BigInteger node4 = table.getNode(new BigInteger("11111"));
		
		Assert.assertTrue(node1.equals(node4));
		Assert.assertTrue(node2.equals(node3));
		Assert.assertFalse(node1.equals(node2));
		
		table.removeNode(node1);
		node1 = table.getNode(new BigInteger("0"));
		Assert.assertTrue(node1.equals(node3));
	}
}
