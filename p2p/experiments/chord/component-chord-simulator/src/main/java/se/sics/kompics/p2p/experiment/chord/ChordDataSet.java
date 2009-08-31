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
package se.sics.kompics.p2p.experiment.chord;

import java.util.HashMap;
import java.util.LinkedList;

import se.sics.kompics.network.Message;
import se.sics.kompics.p2p.overlay.key.NumericRingKey;

/**
 * The <code>ChordDataSet</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ChordDataSet {

	public long beganAt, endedAt;

	public long totalLookups, failedLookups, successLookups, correctLookups;

	public LinkedList<ChordLookupStat> lookups = new LinkedList<ChordLookupStat>();

	public HashMap<NumericRingKey, Integer> loadHistogram = new HashMap<NumericRingKey, Integer>();

	public HashMap<Class<? extends Message>, ReceivedMessage> messageHistogram = new HashMap<Class<? extends Message>, ReceivedMessage>();
}
