/**
 * This file is part of the Kompics component model runtime.
 * <p>
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 * <p>
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.network;

import java.io.Serializable;
import se.sics.kompics.address.Address;

/**
 * The <code>Message</code> class.
 * <p>
 * @deprecated Use {@link se.sics.kompics.network.Msg Msg} instead.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @author Lars Kroll <lkroll@sics.se>
 * @version $Id: Message.java 4051 2012-03-30 14:32:40Z Cosmin $
 */
@Deprecated
public abstract class Message implements Msg, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 2644373757327105586L;

    private Address source;

    private Address destination;

    private Address origin;

    private Transport protocol;

    /**
     * Instantiates a new message.
     * <p>
     * @param source the source
     * @param destination the destination
     */
    protected Message(Address source, Address destination) {
        this(source, destination, source, Transport.TCP);
    }

    protected Message(Address source, Address destination, Address origin) {
        this(source, destination, origin, Transport.TCP);
    }

    protected Message(Address source, Address destination, Transport protocol) {
        this(source, destination, source, protocol);
    }

    /**
     * Instantiates a new message.
     * <p>
     * @param source the source
     * @param destination the destination
     * @param protocol the protocol
     * @param highPriority whether this message is should be sent with high
     * priority
     */
    protected Message(Address source, Address destination, Address origin, Transport protocol) {
        this.source = source;
        this.destination = destination;
        this.origin = origin;
        this.protocol = protocol;
    }

    /**
     * Gets the source.
     * <p>
     * @return the source
     */
    public final Address getSource() {
        return source;
    }

    public void setSource(Address source) {
        this.source = source;
    }

    /**
     * Gets the destination.
     * <p>
     * @return the destination
     */
    public final Address getDestination() {
        return destination;
    }

    public void setDestination(Address destination) {
        this.destination = destination;
    }

    public final Address getOrigin() {
        return this.origin;
    }

    /**
     * Sets the protocol.
     * <p>
     * @param protocol the new protocol
     */
    public final void setProtocol(Transport protocol) {
        this.protocol = protocol;
    }

    /**
     * Gets the protocol.
     * <p>
     * @return the protocol
     */
    public final Transport getProtocol() {
        return protocol;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("(");
        sb.append("SRC: ");
        sb.append(source);
        sb.append(", DST: ");
        sb.append(destination);
        sb.append(", ORI: ");
        sb.append(origin);
        sb.append(", PRT: ");
        sb.append(protocol);
        sb.append(")");
        return sb.toString();
    }
}
