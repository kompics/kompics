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
package se.sics.kompics.network.data;

import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
@SuppressWarnings("rawtypes")
public class DataMsgWrapper implements Msg {

    public final Msg msg;
    public final Transport proto;
    private final DataWrapperHeader header = new DataWrapperHeader();

    public DataMsgWrapper(Msg msg, Transport proto) {
        this.msg = msg;
        this.proto = proto;
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public Address getSource() {
        return header.getSource();
    }

    @Override
    public Address getDestination() {
        return header.getDestination();
    }

    @Override
    public Transport getProtocol() {
        return proto;
    }

    public class DataWrapperHeader implements Header {

        @Override
        public Address getSource() {
            return msg.getHeader().getSource();
        }

        @Override
        public Address getDestination() {
            return msg.getHeader().getDestination();
        }

        @Override
        public Transport getProtocol() {
            return proto;
        }

    }

    @Override
    public String toString() {
        return String.format("DataMsgWrapper(msg: %s, proto: %s)", msg, proto);
    }

}
