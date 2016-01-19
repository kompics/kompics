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
package se.sics.kompics.network.data.test;

import se.sics.kompics.network.Address;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.data.DataHeader;

/**
 *
 * @author lkroll
 */
public abstract class DataMessage implements Msg<Address, DataHeader<Address>> {

    public static final int MESSAGE_SIZE = 65000;
    
    private final DataHeader header;
    
    
    public DataMessage(Address src, Address dst, Transport proto) {
        this.header = new DataHeaderImpl(src, dst, proto);
    }
    
    DataMessage(DataHeader header) {
        this.header = header;
    }
    
    @Override
    public DataHeader getHeader() {
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
        return header.getProtocol();
    }

    public static final class DataHeaderImpl implements DataHeader<Address> {

        private final Address src;
        private final Address dst;
        private Transport proto;
        
        DataHeaderImpl(Address src, Address dst, Transport proto) {
            this.src = src;
            this.dst = dst;
            this.proto = proto;
        }
        
        @Override
        public void replaceProtocol(Transport newProto) {
            proto = newProto;
        }

        @Override
        public Address getSource() {
            return src;
        }

        @Override
        public Address getDestination() {
            return dst;
        }

        @Override
        public Transport getProtocol() {
            return proto;
        }
    
}
}
