/**
 * This file is part of the Kompics component model runtime.
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
package se.sics.kompics.manual.twopc.event;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * The <code>Abort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 */
public class Abort extends Message {

	private final int transactionId;
	public Abort(int transactionId, Address src, Address dest) {
		super(src, dest);
		this.transactionId = transactionId;
	}
	
	public int getTransactionId() {
		return transactionId;
	}
}
