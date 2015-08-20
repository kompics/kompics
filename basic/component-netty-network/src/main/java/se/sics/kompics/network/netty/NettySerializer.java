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

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.SpecialSerializers;

/**
 *
 * @author lkroll
 */
public class NettySerializer implements Serializer {

    @Override
    public int identifier() {
         return 5;
    }

    @Override
        public void toBinary(Object o, ByteBuf buf) {
            if (o instanceof DisambiguateConnection) {
                DisambiguateConnection r = (DisambiguateConnection) o;
                SpecialSerializers.MessageSerializationUtil.msgToBinary(r, buf, r.reply, false);
                // Port 2 byte
                byte[] portBytes = Ints.toByteArray(r.udtPort);
                buf.writeByte(portBytes[2]);
                buf.writeByte(portBytes[3]);
                return;
            }
            throw new RuntimeException("Can't serialize " + o.getClass() + " with this serializer!");
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            SpecialSerializers.MessageSerializationUtil.MessageFields fields = SpecialSerializers.MessageSerializationUtil.msgFromBinary(buf);
            // Port 2 byte
            byte portUpper = buf.readByte();
            byte portLower = buf.readByte();
            int udtPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            return new DisambiguateConnection(fields.src, fields.dst, fields.proto, udtPort, fields.flag1);
        }
    
}
