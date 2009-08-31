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
package se.sics.kompics.p2p.experiment.dsl.distribution;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The <code>LongUniformDistributionTest</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: LongUniformDistributionTest.java 750 2009-04-02 09:55:01Z
 *          Cosmin $
 */
public class LongUniformDistributionTest {

	LongUniformDistribution distribution;
	long min, max;

	@Before
	public void setUp() throws Exception {
		Random random = new Random();
		min = random.nextLong();
		max = random.nextLong();
		min = (min < 0 ? -min : min);
		max = (max < 0 ? -max : max);
		if (min > max) {
			long aux = min;
			min = max;
			max = aux;
		}
		distribution = new LongUniformDistribution(min, max, random);
	}

	@Test
	public void testLongUniformDistribution() {
		int count = 1000000;
		long l;
		for (int i = 0; i < count; i++) {
			l = distribution.draw();
			try {
				Assert.assertTrue(l >= min);
				Assert.assertTrue(l <= max);
			} catch (AssertionError e) {
				System.err.println(l + " not in [" + min + "," + max + "]");
				throw e;
			}
		}
	}
}
