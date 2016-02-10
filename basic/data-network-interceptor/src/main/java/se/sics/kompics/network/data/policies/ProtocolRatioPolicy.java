/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.kompics.network.data.policies;

import org.jscience.mathematics.number.Rational;

/**
 *
 * @author lkroll
 */
public interface ProtocolRatioPolicy {
    /**
     * Called periodically by the component whenever new statistics are available.
     * 
     * Returns r in [-1, 1], the ratio between TCP (-1.0) and UDT (1.0) where 0.0 is 50/50
     * 
     * @param throughput average throughput estimate since last update call
     * @param deliveryLatency average delivery latency since last update call
     * @return the ratio between TCP and UDT
     */
    public Rational update(double throughput, double deliveryLatency);
    
    public void initialState(Rational initState);
}
