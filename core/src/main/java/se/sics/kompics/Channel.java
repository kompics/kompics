/*
 * This file is part of the Kompics component model runtime.
 * <p>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

/**
 * The <code>Channel</code> class.
 * <p>
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @version $Id$
 */
public interface Channel<P extends PortType> {

    /**
     * Factory for bidirectional channels.
     */
    public static ChannelFactory TWO_WAY = new BiDirectionalChannel.Factory();
    /**
     * Factory for unidirectional channels from negative to positive.
     */
    public static ChannelFactory ONE_WAY_POS = new UniDirectionalChannel.Factory(
            UniDirectionalChannel.Direction.TO_POSITIVE);
    /**
     * Factory for unidirectional channels from positive to negative.
     */
    public static ChannelFactory ONE_WAY_NEG = new UniDirectionalChannel.Factory(
            UniDirectionalChannel.Direction.TO_NEGATIVE);

    public void disconnect();

    /**
     * Gets the port type.
     * <p>
     * 
     * @return the port type
     */
    public P getPortType();

}
