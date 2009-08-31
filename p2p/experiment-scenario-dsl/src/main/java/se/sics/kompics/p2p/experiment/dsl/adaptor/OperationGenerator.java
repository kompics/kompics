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

/**
 * The <code>OperationGenerator</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class OperationGenerator implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8472814732879945114L;

	private final ConcreteOperation<?, ?, ?, ?, ?, ?> concreteOperation;

	private final int count;

	public OperationGenerator(
			ConcreteOperation<?, ?, ?, ?, ?, ?> concreteOperation, int count) {
		super();
		this.concreteOperation = concreteOperation;
		this.count = count;
	}

	public final Event generate() {
		return concreteOperation.generate();
	}

	public final int getCount() {
		return count;
	}
}
