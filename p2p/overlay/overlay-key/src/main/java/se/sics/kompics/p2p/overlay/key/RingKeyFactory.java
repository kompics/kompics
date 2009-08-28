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
package se.sics.kompics.p2p.overlay.key;

import java.math.BigInteger;

/**
 * A factory for creating RingKey objects.
 */
public class RingKeyFactory {

	private final boolean numeric;

	public RingKeyFactory(Class<? extends RingKey> ringKeyType) {
		if (NumericRingKey.class.isAssignableFrom(ringKeyType)) {
			numeric = true;
		} else if (StringRingKey.class.isAssignableFrom(ringKeyType)) {
			numeric = false;
		} else {
			throw new RuntimeException(
					"I can create only a NumericRingKey or a StringRingKey");
		}
	}

	public RingKey create(String id) {
		if (numeric)
			throw new RuntimeException("I only create NumericRingKeys");
		return new StringRingKey(id);
	}

	public RingKey create(BigInteger id) {
		if (!numeric)
			throw new RuntimeException("I only create StringRingKeys");
		return new NumericRingKey(id);
	}

	public RingKey create(long id) {
		if (!numeric)
			throw new RuntimeException("I only create StringRingKeys");
		return new NumericRingKey(id);
	}
}
