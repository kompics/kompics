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
package se.sics.kompics.p2p.experiment.dsl.adaptor;

import java.io.Serializable;

import se.sics.kompics.Event;
import se.sics.kompics.p2p.experiment.dsl.distribution.Distribution;

/**
 * The <code>ConcreteOperation</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ConcreteOperation<E extends Event, P1 extends Number, P2 extends Number, P3 extends Number, P4 extends Number, P5 extends Number>
		implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2937585392288426864L;

	private final Operation<E> operation;
	private final Operation1<E, P1> operation1;
	private final Operation2<E, P1, P2> operation2;
	private final Operation3<E, P1, P2, P3> operation3;
	private final Operation4<E, P1, P2, P3, P4> operation4;
	private final Operation5<E, P1, P2, P3, P4, P5> operation5;

	private final int paramCount;

	private final Distribution<P1> distribution1;
	private final Distribution<P2> distribution2;
	private final Distribution<P3> distribution3;
	private final Distribution<P4> distribution4;
	private final Distribution<P5> distribution5;

	public ConcreteOperation(Operation<E> op) {
		paramCount = 0;
		operation = op;
		operation1 = null;
		operation2 = null;
		operation3 = null;
		operation4 = null;
		operation5 = null;
		distribution1 = null;
		distribution2 = null;
		distribution3 = null;
		distribution4 = null;
		distribution5 = null;
	}

	public ConcreteOperation(Operation1<E, P1> op1, Distribution<P1> d1) {
		paramCount = 1;
		operation = null;
		operation1 = op1;
		operation2 = null;
		operation3 = null;
		operation4 = null;
		operation5 = null;
		distribution1 = d1;
		distribution2 = null;
		distribution3 = null;
		distribution4 = null;
		distribution5 = null;
	}

	public ConcreteOperation(Operation2<E, P1, P2> op2, Distribution<P1> d1,
			Distribution<P2> d2) {
		paramCount = 2;
		operation = null;
		operation1 = null;
		operation2 = op2;
		operation3 = null;
		operation4 = null;
		operation5 = null;
		distribution1 = d1;
		distribution2 = d2;
		distribution3 = null;
		distribution4 = null;
		distribution5 = null;
	}

	public ConcreteOperation(Operation3<E, P1, P2, P3> op3,
			Distribution<P1> d1, Distribution<P2> d2, Distribution<P3> d3) {
		paramCount = 3;
		operation = null;
		operation1 = null;
		operation2 = null;
		operation3 = op3;
		operation4 = null;
		operation5 = null;
		distribution1 = d1;
		distribution2 = d2;
		distribution3 = d3;
		distribution4 = null;
		distribution5 = null;
	}

	public ConcreteOperation(Operation4<E, P1, P2, P3, P4> op4,
			Distribution<P1> d1, Distribution<P2> d2, Distribution<P3> d3,
			Distribution<P4> d4) {
		paramCount = 4;
		operation = null;
		operation1 = null;
		operation2 = null;
		operation3 = null;
		operation4 = op4;
		operation5 = null;
		distribution1 = d1;
		distribution2 = d2;
		distribution3 = d3;
		distribution4 = d4;
		distribution5 = null;
	}

	public ConcreteOperation(Operation5<E, P1, P2, P3, P4, P5> op5,
			Distribution<P1> d1, Distribution<P2> d2, Distribution<P3> d3,
			Distribution<P4> d4, Distribution<P5> d5) {
		paramCount = 5;
		operation = null;
		operation1 = null;
		operation2 = null;
		operation3 = null;
		operation4 = null;
		operation5 = op5;
		distribution1 = d1;
		distribution2 = d2;
		distribution3 = d3;
		distribution4 = d4;
		distribution5 = d5;
	}

	public E generate() {
		switch (paramCount) {
		case 1:
			return operation1.generate(distribution1.draw());
		case 2:
			return operation2.generate(distribution1.draw(), distribution2
					.draw());
		case 3:
			return operation3.generate(distribution1.draw(), distribution2
					.draw(), distribution3.draw());
		case 4:
			return operation4.generate(distribution1.draw(), distribution2
					.draw(), distribution3.draw(), distribution4.draw());
		case 5:
			return operation5.generate(distribution1.draw(), distribution2
					.draw(), distribution3.draw(), distribution4.draw(),
					distribution5.draw());
		default:
			return operation.generate();
		}
	}
}
