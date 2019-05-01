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

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.data.DataHeader;
import se.sics.kompics.network.data.test.DataMessage.DataHeaderImpl;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.SpecialSerializers;

/**
 *
 * @author lkroll
 */
public class DataMessageSerialiser implements Serializer {

    private static final Serializer addrS = SpecialSerializers.AddressSerializer.INSTANCE;

    private static final byte DATA = 1;
    private static final byte PREPARE = 2;
    private static final byte PREPARED = 3;

    @Override
    public int identifier() {
        return 101;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        if (o instanceof DataMessage) {
            DataMessage msg = (DataMessage) o;
            // HEADER
            DataHeader<Address> h = msg.getHeader();
            addrS.toBinary(h.getSource(), buf);
            addrS.toBinary(h.getDestination(), buf);
            buf.writeShort(h.getProtocol().ordinal());
            // CONTENT
            if (o instanceof Data) {
                Data d = (Data) o;
                buf.writeByte(DATA);
                buf.writeInt(d.pos);
                buf.writeInt(d.total);
                buf.writeInt(d.data.length);
                buf.writeBytes(d.data);
            } else if (o instanceof Prepare) {
                Prepare p = (Prepare) o;
                buf.writeByte(PREPARE);
                buf.writeInt(p.volume);
            } else if (o instanceof Prepared) {
                // Prepared p = (Prepared) o;
                buf.writeByte(PREPARED);
            }
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        // HEADER
        Address srcAddr = (Address) addrS.fromBinary(buf, Optional.absent());
        Address destAddr = (Address) addrS.fromBinary(buf, Optional.absent());
        Transport proto = Transport.values()[buf.readUnsignedShort()];
        DataHeader<Address> h = new DataHeaderImpl(srcAddr, destAddr, proto);
        // CONTENT
        byte type = buf.readByte();
        switch (type) {
        case DATA: {
            int pos = buf.readInt();
            int total = buf.readInt();
            int length = buf.readInt();
            byte[] data = new byte[length];
            buf.readBytes(data);
            return new Data(h, pos, total, data);
        }
        case PREPARE: {
            int volume = buf.readInt();
            return new Prepare(h, volume);
        }
        case PREPARED: {
            return new Prepared(h);
        }
        }
        return null;
    }

}
