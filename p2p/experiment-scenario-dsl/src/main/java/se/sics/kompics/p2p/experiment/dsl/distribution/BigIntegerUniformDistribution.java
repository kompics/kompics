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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Random;

/**
 * The <code>BigIntegerUniformDistribution</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: BigIntegerUniformDistribution.java 750 2009-04-02 09:55:01Z
 *          Cosmin $
 */
public class BigIntegerUniformDistribution extends Distribution<BigInteger> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3816663807544317577L;

	private final Random random;

	private final BigDecimal min;

	private final BigDecimal range;

	private final boolean numBitsVersion;

	private final int numBits;

	public BigIntegerUniformDistribution(BigInteger min, BigInteger max,
			Random random) {
		super(Type.UNIFORM, BigInteger.class);
		this.numBitsVersion = false;
		this.random = random;
		if (min.compareTo(max) > 0) {
			this.min = new BigDecimal(max);
			this.range = new BigDecimal(min).subtract(this.min);
		} else {
			this.min = new BigDecimal(min);
			this.range = new BigDecimal(max).subtract(this.min);
		}
		numBits = 0;
	}

	public BigIntegerUniformDistribution(int numBits, Random random) {
		super(Type.UNIFORM, BigInteger.class);
		this.numBitsVersion = true;
		this.random = random;
		this.numBits = numBits;
		this.min = new BigDecimal(0);
		this.range = new BigDecimal(BigInteger.valueOf(2).pow(numBits));
	}

	@Override
	public final BigInteger draw() {
		if (numBitsVersion) {
			return new BigInteger(numBits, random);
		}
		double u = random.nextDouble();
		BigDecimal d = new BigDecimal(u);
		return d.multiply(range).add(min).round(MathContext.UNLIMITED)
				.toBigInteger();
	}
}
