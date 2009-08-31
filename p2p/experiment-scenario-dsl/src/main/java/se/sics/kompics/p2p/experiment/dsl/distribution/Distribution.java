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

import java.io.Serializable;

/**
 * The <code>Distribution</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public abstract class Distribution<E extends Number> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7789173701412786074L;

	/**
	 * The <code>Type</code> class.
	 * 
	 * @author Cosmin Arad <cosmin@sics.se>
	 * @version $Id$
	 */
	public static enum Type {
		CONSTANT, UNIFORM, NORMAL, EXPONENTIAL, OTHER;
	};

	private final Type type;

	private final Class<E> numberType;

	protected Distribution(Type type, Class<E> numberType) {
		this.type = type;
		this.numberType = numberType;
	}

	public final Type getType() {
		return type;
	}

	public final Class<E> getNumberType() {
		return numberType;
	}

	public abstract E draw();
}
