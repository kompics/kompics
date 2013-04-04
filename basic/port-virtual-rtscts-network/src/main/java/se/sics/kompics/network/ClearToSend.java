/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
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
package se.sics.kompics.network;

import se.sics.kompics.Event;
import se.sics.kompics.Response;
import se.sics.kompics.address.Address;

/**
 * The
 * <code>ClearToSend</code> class.
 *
 * @author Lars Kroll <lkroll@sics.se>
 * @version $$
 *
 */
public abstract class ClearToSend extends Response implements Cloneable {
    
    private Address destination;
    private Address source;
    private int quota;
    private int flowId;
    private int requestId;

    public ClearToSend(Address src, Address dest, RequestToSend rts) {
        super(rts);
        this.destination = dest;
        this.source = src;
    }

    /**
     * Used by Network implementation!
     *  
     */
    public void setQuota(int quota) {
        this.quota = quota;
    }
    
    /**
     * Used by Network implementation!
     *  
     */
    public void setFlowId(int flowId) {
        this.flowId = flowId;
    }
    
    /**
     * Used by Network implementation!
     *  
     */
    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public Address getDestination() {
        return this.destination;
    }

    public Address getSource() {
        return this.source;
    }

    public int getQuota() {
        return this.quota;
    }

    public int getFlowId() {
        return this.flowId;
    }
    
    public int getRequestId() {
        return this.requestId;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClearToSend(");
        sb.append(source.toString());
        sb.append(", ");
        sb.append(destination.toString());
        sb.append(", ");
        sb.append(quota);
        sb.append(", ");
        sb.append(flowId);
        sb.append(", ");
        sb.append(requestId);
        sb.append(')');
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see se.sics.kompics.Response#clone()
     */
    @Override
    public final Object clone() {
        ClearToSend cts = (ClearToSend) super.clone();
        cts.destination = this.destination;
        cts.source = this.source;
        cts.quota = this.quota;
        cts.flowId = this.flowId;
        cts.requestId = this.requestId;
        return cts;
    }
    
    public DataMessage constructMessage(Message msg) {
        return new DataMessage(flowId, requestId, msg);
    }
}
