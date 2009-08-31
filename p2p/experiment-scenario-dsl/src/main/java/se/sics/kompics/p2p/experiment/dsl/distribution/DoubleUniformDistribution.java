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
 * The <code>DoubleUniformDistribution</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: DoubleUniformDistribution.java 750 2009-04-02 09:55:01Z Cosmin
 *          $
 */
public class DoubleUniformDistribution extends Distribution<Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4968929562576254409L;

	private final Random random;

	private final double min;

	private final double max;

	public DoubleUniformDistribution(double min, double max, Random random) {
		super(Type.UNIFORM, Double.class);
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
	public final Double draw() {
		double u = random.nextDouble();
		u *= (max - min);
		u += min;
		return u;
	}
}
