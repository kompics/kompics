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

import java.util.Map;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * The <code>Commit</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 */
public class Ack extends Message {

	private static final long serialVersionUID = -2160411310927208910L;

	private final int transactionId;
	
	private final Map<String,String> readValues;
	
	public Ack(int transactionId, Map<String,String> responses, Address src, Address dest) {
		super(src, dest);
		if (responses == null)
		{
			throw new IllegalArgumentException("responses was null");
		}
		this.readValues = responses;
		this.transactionId = transactionId;
	}
	
	public int getTransactionId() {
		return transactionId;
	}
	
	public Map<String, String> getReadValues() {
		return readValues;
	}
}
