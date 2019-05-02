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

import java.util.Optional;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import java.util.UUID;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.DatagramSerializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.network.netty.serialization.SpecialSerializers;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class NettySerializer implements DatagramSerializer {

    private static final byte DIS = 0;
    private static final byte ACK = 1;
    private static final byte ACK_REQ = 2;
    private static final byte CHECK = 3;
    private static final byte CLOSE = 4;
    private static final byte CLOSED = 5;

    @Override
    public int identifier() {
        return 5;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        if (o instanceof DisambiguateConnection) {
            DisambiguateConnection r = (DisambiguateConnection) o;
            buf.writeByte(DIS);
            SpecialSerializers.MessageSerializationUtil.msgToBinary(r, buf, r.reply, false);
            // Port 2 byte
            byte[] portBytes = Ints.toByteArray(r.udtPort);
            buf.writeByte(portBytes[2]);
            buf.writeByte(portBytes[3]);
            return;
        }
        if (o instanceof NotifyAck) {
            NotifyAck ack = (NotifyAck) o;
            buf.writeByte(ACK);
            SpecialSerializers.MessageSerializationUtil.msgToBinary(ack, buf, false, false);
            SpecialSerializers.UUIDSerializer.INSTANCE.toBinary(ack.id, buf);
            return;
        }
        if (o instanceof AckRequestMsg) {
            AckRequestMsg arm = (AckRequestMsg) o;
            buf.writeByte(ACK_REQ);
            SpecialSerializers.UUIDSerializer.INSTANCE.toBinary(arm.id, buf);
            Serializers.toBinary(arm.content, buf);
            return;
        }
        if (o instanceof CheckChannelActive) {
            CheckChannelActive cca = (CheckChannelActive) o;
            buf.writeByte(CHECK);
            SpecialSerializers.MessageSerializationUtil.msgToBinary(cca, buf, false, false);
            return;
        }
        if (o instanceof CloseChannel) {
            CloseChannel cc = (CloseChannel) o;
            buf.writeByte(CLOSE);
            SpecialSerializers.MessageSerializationUtil.msgToBinary(cc, buf, false, false);
            return;
        }
        if (o instanceof ChannelClosed) {
            ChannelClosed cc = (ChannelClosed) o;
            buf.writeByte(CLOSED);
            SpecialSerializers.MessageSerializationUtil.msgToBinary(cc, buf, false, false);
            return;
        }
        throw new RuntimeException("Can't serialize " + o.getClass() + " with this serializer!");
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        byte type = buf.readByte();
        switch (type) {
        case DIS: {
            SpecialSerializers.MessageSerializationUtil.MessageFields fields = SpecialSerializers.MessageSerializationUtil
                    .msgFromBinary(buf);
            // Port 2 byte
            byte portUpper = buf.readByte();
            byte portLower = buf.readByte();
            int udtPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            return new DisambiguateConnection(fields.src, fields.dst, fields.proto, udtPort, fields.flag1);
        }
        case ACK: {
            SpecialSerializers.MessageSerializationUtil.MessageFields fields = SpecialSerializers.MessageSerializationUtil
                    .msgFromBinary(buf);
            UUID id = (UUID) SpecialSerializers.UUIDSerializer.INSTANCE.fromBinary(buf, Optional.empty());
            return new NotifyAck(fields.src, fields.dst, fields.proto, id);
        }
        case ACK_REQ: {
            UUID id = (UUID) SpecialSerializers.UUIDSerializer.INSTANCE.fromBinary(buf, Optional.empty());
            Msg<?, ?> msg = (Msg<?, ?>) Serializers.fromBinary(buf, Optional.empty());
            return new AckRequestMsg(msg, id);
        }
        case CHECK: {
            SpecialSerializers.MessageSerializationUtil.MessageFields fields = SpecialSerializers.MessageSerializationUtil
                    .msgFromBinary(buf);
            return new CheckChannelActive(fields.src, fields.dst, fields.proto);
        }
        case CLOSE: {
            SpecialSerializers.MessageSerializationUtil.MessageFields fields = SpecialSerializers.MessageSerializationUtil
                    .msgFromBinary(buf);
            return new CloseChannel(fields.src, fields.dst, fields.proto);
        }
        case CLOSED: {
            SpecialSerializers.MessageSerializationUtil.MessageFields fields = SpecialSerializers.MessageSerializationUtil
                    .msgFromBinary(buf);
            return new ChannelClosed(fields.src, fields.dst, fields.proto);
        }
        }
        return null;
    }

    @Override
    public Object fromBinary(ByteBuf buf, DatagramPacket datagram) {
        byte type = buf.readByte();
        SpecialSerializers.MessageSerializationUtil.MessageFields fields = SpecialSerializers.MessageSerializationUtil
                .msgFromBinary(buf);
        switch (type) {
        case DIS: {
            // Port 2 byte
            byte portUpper = buf.readByte();
            byte portLower = buf.readByte();
            int udtPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            return new DisambiguateConnection(new NettyAddress(datagram.sender()),
                    new NettyAddress(datagram.recipient()), Transport.UDP, udtPort, fields.flag1);
        }
        case ACK: {
            UUID id = (UUID) SpecialSerializers.UUIDSerializer.INSTANCE.fromBinary(buf, Optional.empty());
            return new NotifyAck(new NettyAddress(datagram.sender()), new NettyAddress(datagram.recipient()),
                    Transport.UDP, id);
        }
        case ACK_REQ: {
            UUID id = (UUID) SpecialSerializers.UUIDSerializer.INSTANCE.fromBinary(buf, Optional.empty());
            Msg<?, ?> msg = (Msg<?, ?>) Serializers.fromBinary(buf, datagram);
            return new AckRequestMsg(msg, id);
        }
        }
        return null;
    }

}
