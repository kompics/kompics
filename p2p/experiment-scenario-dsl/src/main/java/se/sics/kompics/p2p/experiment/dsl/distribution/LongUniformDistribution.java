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

/**
 * The <code>LongUniformDistribution</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class LongUniformDistribution extends Distribution<Long> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3038020134434091009L;

	private final Random random;

	private final long min;

	private final long max;

	public LongUniformDistribution(long min, long max, Random random) {
		super(Type.UNIFORM, Long.class);
		if (min < 0 || max < 0) {
			throw new RuntimeException("I can only generate positive numbers");
		}
		this.random = random;
		if (min > max) {
			this.min = max;
			this.max = min;
		} else {
			this.min = min;
			this.max = max;
		}
	}

	@Override
	public final Long draw() {
		double u = random.nextDouble();
		u *= (max - min);
		u += min;
		return Math.round(u);
	}
}
