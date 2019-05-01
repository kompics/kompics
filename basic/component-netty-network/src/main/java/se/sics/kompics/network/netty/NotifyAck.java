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
package se.sics.kompics.network.netty;

import java.util.UUID;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class NotifyAck extends DirectMessage {

    public final UUID id;

    public NotifyAck(Address src, Address dst, Transport protocol, UUID id) {
        super(src, dst, protocol);
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("(");
        sb.append("SRC: ");
        sb.append(this.getSource());
        sb.append(", DST: ");
        sb.append(this.getDestination());
        sb.append(", PRT: ");
        sb.append(this.getProtocol());
        sb.append(", ID: ");
        sb.append(id);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.getSource() != null ? this.getSource().hashCode() : 0);
        hash = 79 * hash + (this.getDestination() != null ? this.getDestination().hashCode() : 0);
        hash = 79 * hash + (this.getProtocol() != null ? this.getProtocol().hashCode() : 0);
        hash = 79 * hash + this.id.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NotifyAck other = (NotifyAck) obj;
        if (this.getSource() != other.getSource()
                && (this.getSource() == null || !this.getSource().equals(other.getSource()))) {
            return false;
        }
        if (this.getDestination() != other.getDestination()
                && (this.getDestination() == null || !this.getDestination().equals(other.getDestination()))) {
            return false;
        }
        if (this.getProtocol() != other.getProtocol()) {
            return false;
        }
        return !this.id.equals(other.id);
    }
}
