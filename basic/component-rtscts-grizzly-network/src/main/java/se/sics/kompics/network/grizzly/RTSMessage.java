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

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class RTSMessage extends Message {

    public final int reqId;
    public final boolean extension;

    public RTSMessage(Address from, Address to, int reqId, boolean extension) {
        super(from, to);

        this.reqId = reqId;
        this.extension = extension;
    }

    public RTSMessage(Address from, Address to, int reqId) {
        this(from, to, reqId, false);
    }

    @Override
    public String toString() {
        return "RSTMessage(reqId=" + reqId + ", ext?=" + extension + ")";
    }
}
