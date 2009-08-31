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
 * The <code>DoubleNormalDistribution</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class DoubleNormalDistribution extends Distribution<Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5252811959351614762L;

	private final Random random;

	private final double mean;
	private final double variance;
	private double u1;
	private double u2;
	private boolean first;

	public DoubleNormalDistribution(double mean, double variance, Random random) {
		super(Type.NORMAL, Double.class);
		this.random = random;
		this.mean = mean;
		this.variance = variance;
		this.first = true;
	}

	@Override
	public final Double draw() {
		if (first) {
			first = false;
			u1 = random.nextDouble();
			u2 = random.nextDouble();
			return mean + variance * Math.sqrt(-2 * Math.log(u1))
					* Math.cos(2 * Math.PI * u2);
		} else {
			first = true;
			return mean + variance * Math.sqrt(-2 * Math.log(u1))
					* Math.sin(2 * Math.PI * u2);
		}
	}
}
