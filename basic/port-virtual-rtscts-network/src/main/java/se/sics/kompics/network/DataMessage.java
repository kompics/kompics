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

/**
 * The <code>DataMessage</code> class.
 *
 * @author Lars Kroll <lkroll@sics.se>
 * @version $$
 *
 */
public class DataMessage extends Event {
    
    private final int flowId;
    private final int requestId;
    private final Message msg;
    private boolean isFinal = false;
    
    public DataMessage(int flowId, int requestId, Message msg) {
        this.flowId = flowId;
        this.requestId = requestId;
        this.msg = msg;
    }
    
    public void setFinal() {
        isFinal = true;
    }
    
    public boolean isFinal() {
        return this.isFinal;
    }
    
    public int getFlowId() {
        return this.flowId;
    }
    
    public int getRequestId() {
        return this.requestId;
    }
    
    public Message getMessage() {
        return this.msg;
    }
    
}
