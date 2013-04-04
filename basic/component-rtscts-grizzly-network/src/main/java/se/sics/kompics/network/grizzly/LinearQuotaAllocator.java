/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009-2011 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009-2011 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.network.grizzly;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import se.sics.kompics.address.Address;

/**
 * A linearly increasing quota allocator.
 * 
 * Assigns old flow + inc as quota to extended flows.
 * 
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class LinearQuotaAllocator implements QuotaAllocator {
    
    private final ConcurrentMap<String, Integer> activeFlows = new ConcurrentHashMap<String, Integer>();
    private final int inc;
    
    LinearQuotaAllocator(int incr) {
        inc = incr;
    }
    
    @Override
    public void newFlow(int id, Address adr) {
        String combinedId = adr.toString() + "#" + id;
        activeFlows.put(combinedId, 0);
    }

    @Override
    public void endFlow(int id, Address adr) {
        String combinedId = adr.toString() + "#" + id;
        activeFlows.remove(combinedId);
    }

    @Override
    public int getQuota(int id, Address adr) {
        String combinedId = adr.toString() + "#" + id;
        int oldq = -1;
        int newq = -1;
        boolean succ = false;
        while(!succ) {
            oldq = activeFlows.get(combinedId);
            newq = oldq+inc;
            succ = activeFlows.replace(combinedId, oldq, newq);
        }
        return newq;
    }
    
}
