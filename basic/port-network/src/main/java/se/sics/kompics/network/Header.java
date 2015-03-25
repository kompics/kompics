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
package se.sics.kompics.network;

/**
 *
 * @author lkroll
 */
public interface Header<Adr extends Address> {
    /**
     * Where does the message come from.
     * 
     * @return 
     */
    public Adr getSource();
    /**
     * Where is the message supposed to go.
     * 
     * @return 
     */
    public Adr getDestination();
    /**
     * What protocol should be used to send the message.
     * 
     * Note that not all network implementations have to implement 
     * all the available protocols.
     * 
     * @return 
     */
    public Transport getProtocol();
}
