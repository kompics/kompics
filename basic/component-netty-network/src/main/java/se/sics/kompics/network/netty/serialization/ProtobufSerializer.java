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
package se.sics.kompics.network.netty.serialization;

import java.util.Optional;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class ProtobufSerializer implements Serializer {

    private static final boolean HAS_PARSER;

    static {
        boolean hasParser = false;
        try {
            // MessageLite.getParsetForType() is not available until protobuf 2.5.0.
            MessageLite.class.getDeclaredMethod("getParserForType");
            hasParser = true;
        } catch (Throwable t) {
            // Ignore
        }

        HAS_PARSER = hasParser;
    }

    private final MessageLite prototype;
    private final ExtensionRegistry extensionRegistry;

    public ProtobufSerializer(MessageLite prototype) {
        this(prototype, null);
    }

    public ProtobufSerializer(MessageLite prototype, ExtensionRegistry extensionRegistry) {
        if (prototype == null) {
            throw new NullPointerException("prototype");
        }
        this.prototype = prototype.getDefaultInstanceForType();
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public int identifier() {
        return 4;
    }

    @Override
    public void toBinary(Object msg, ByteBuf buf) {
        if (msg instanceof MessageLite) {
            byte[] bytes = ((MessageLite) msg).toByteArray();
            buf.writeBytes(bytes);
            return;
        }
        if (msg instanceof MessageLite.Builder) {
            byte[] bytes = ((MessageLite.Builder) msg).build().toByteArray();
            buf.writeBytes(bytes);
            return;
        }
    }

    @Override
    public Object fromBinary(ByteBuf msg, Optional<Object> hint) {
        final byte[] array;
        final int offset;
        final int length = msg.readableBytes();
        if (msg.hasArray()) {
            array = msg.array();
            offset = msg.arrayOffset() + msg.readerIndex();
        } else {
            array = new byte[length];
            msg.getBytes(msg.readerIndex(), array, 0, length);
            offset = 0;
        }

        Object o = null;
        try {
            if (extensionRegistry == null) {
                if (HAS_PARSER) {
                    o = prototype.getParserForType().parseFrom(array, offset, length);
                } else {
                    o = prototype.newBuilderForType().mergeFrom(array, offset, length).build();
                }
            } else {
                if (HAS_PARSER) {
                    o = prototype.getParserForType().parseFrom(array, offset, length, extensionRegistry);
                } else {
                    o = prototype.newBuilderForType().mergeFrom(array, offset, length, extensionRegistry).build();
                }
            }
        } catch (InvalidProtocolBufferException ex) {
            Serializers.LOG.error("ProtobufSerializer: Couldn't deserialize object.", ex);
        }

        return o;
    }

}
