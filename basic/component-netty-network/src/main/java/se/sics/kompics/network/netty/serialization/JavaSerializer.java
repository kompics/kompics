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
import com.google.common.io.Closer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.serialization.ClassResolver;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class JavaSerializer implements Serializer {

    private ClassResolver resolver;

    public JavaSerializer(ClassResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public int identifier() {
        return 3;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        try {
            Closer closer = Closer.create(); // TODO: Convert to try-with-resources once Java6 is faded out
            try {
                ByteBufOutputStream bout = closer.register(new ByteBufOutputStream(buf));
                ObjectOutputStream oout = closer.register(new CompactObjectOutputStream(bout));
                oout.writeObject(o);
                oout.flush();
            } catch (Throwable e) { // must catch Throwable
                throw closer.rethrow(e);
            } finally {
                closer.close();
            }
        } catch (IOException ex) {
            Serializers.LOG.error("JavaSerializer: Could not Serialize object of type " + o.getClass(), ex);
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        // Ignore hint
        try {
            Closer closer = Closer.create();
            try {
                ByteBufInputStream bbis = closer.register(new ByteBufInputStream(buf));
                CompactObjectInputStream cois = closer.register(new CompactObjectInputStream(bbis, resolver));
                return cois.readObject();
            } catch (Throwable e) {
                throw closer.rethrow(e);
            } finally {
                closer.close();
            }
        } catch (IOException ex) {
            Serializers.LOG.error("JavaSerializer: Could not deserialize object", ex);
            return null;
        }
    }

    private static class CompactObjectInputStream extends ObjectInputStream {

        private final ClassResolver classResolver;

        CompactObjectInputStream(InputStream in, ClassResolver classResolver) throws IOException {
            super(in);
            this.classResolver = classResolver;
        }

        @Override
        protected void readStreamHeader() throws IOException {
            int version = readByte() & 0xFF;
            if (version != STREAM_VERSION) {
                throw new StreamCorruptedException("Unsupported version: " + version);
            }
        }

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
            int type = read();
            if (type < 0) {
                throw new EOFException();
            }
            switch (type) {
            case CompactObjectOutputStream.TYPE_FAT_DESCRIPTOR:
                return super.readClassDescriptor();
            case CompactObjectOutputStream.TYPE_THIN_DESCRIPTOR:
                String className = readUTF();
                Class<?> clazz = classResolver.resolve(className);
                return ObjectStreamClass.lookupAny(clazz);
            default:
                throw new StreamCorruptedException("Unexpected class descriptor type: " + type);
            }
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            Class<?> clazz;
            try {
                clazz = classResolver.resolve(desc.getName());
            } catch (ClassNotFoundException ex) {
                clazz = super.resolveClass(desc);
            }

            return clazz;
        }

    }

    private static class CompactObjectOutputStream extends ObjectOutputStream {

        static final byte TYPE_FAT_DESCRIPTOR = 0;
        static final byte TYPE_THIN_DESCRIPTOR = 1;

        CompactObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            writeByte(STREAM_VERSION);
        }

        @Override
        protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
            Class<?> clazz = desc.forClass();
            if (clazz.isPrimitive() || clazz.isArray() || clazz.isInterface() || desc.getSerialVersionUID() == 0) {
                writeByte(TYPE_FAT_DESCRIPTOR);
                super.writeClassDescriptor(desc);
            } else {
                writeByte(TYPE_THIN_DESCRIPTOR);
                writeUTF(desc.getName());
            }
        }
    }

}
