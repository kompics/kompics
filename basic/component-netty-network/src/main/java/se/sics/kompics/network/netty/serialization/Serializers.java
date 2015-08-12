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

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.serialization.ClassResolvers;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.network.netty.DirectMessage;
import se.sics.kompics.network.netty.NettyAddress;
import se.sics.kompics.network.netty.NettySerializer;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public abstract class Serializers {

    public static enum IdBytes {

        ONE(1, 256),
        TWO(2, 65536),
        THREE(3, 16777216),
        FOUR(4, Integer.MAX_VALUE);

        private int values;
        private int bytes;

        private IdBytes(int bytes, int values) {
            this.values = values;
            this.bytes = bytes;
        }

        public int getValues() {
            return values;
        }

        public int getBytes() {
            return bytes;
        }
    }

    public static final Logger LOG = LoggerFactory.getLogger(Serializers.class);
    static final int BYTES = Integer.SIZE / 8;

    private static final HashMap<String, Integer> classMappings = new HashMap<String, Integer>();
    private static final HashMap<String, Integer> serializerNames = new HashMap<String, Integer>();
    private static final ConcurrentHashMap<String, Serializer> resolutionCache = new ConcurrentHashMap<String, Serializer>();
    private static Serializer[] bindings;
    private static IdBytes idSerializationBytes = IdBytes.ONE;
    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Serializer nullS = new SpecialSerializers.NullSerializer();

    static {
        rwLock.writeLock().lock();
        try {
            bindings = new Serializer[IdBytes.ONE.values];
            register(nullS, "nullS");
            register(new SpecialSerializers.ByteSerializer(), "byteS");
            register(byte[].class, "byteS");
            register(SpecialSerializers.AddressSerializer.INSTANCE, "nettyAddrS");
            register(NettyAddress.class, "nettyAddrS");
            register(new NettySerializer(), "nettyS");
            register(DirectMessage.class, "nettyS");
            register(SpecialSerializers.UUIDSerializer.INSTANCE, "uuidS");
            register(UUID.class, "uuidS");
            register(new JavaSerializer(ClassResolvers.softCachingConcurrentResolver(ClassLoader.getSystemClassLoader())), "javaS");
            register(Serializable.class, "javaS");
            //register(new AvroSerializer(), "avroS");
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static void resize(IdBytes idSB) {
        rwLock.writeLock().lock();
        try {
            if (idSB == idSerializationBytes) {
                return; // nothing to do
            }
            resolutionCache.clear();
            Serializer[] oldBindings = bindings;
            idSerializationBytes = idSB;
            bindings = new Serializer[idSerializationBytes.getValues()];
            System.arraycopy(oldBindings, 0, bindings, 0, java.lang.Math.min(oldBindings.length, bindings.length));
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static void register(Serializer s, String name) {
        rwLock.writeLock().lock();
        try {
            resolutionCache.clear();
            bindings[s.identifier()] = s;
            serializerNames.put(name, s.identifier());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static void register(Class<?> type, int serializerId) {
        rwLock.writeLock().lock();
        try {
            resolutionCache.clear();
            classMappings.put(type.getName(), serializerId);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static void register(Class<?> type, String serializerName) {
        rwLock.writeLock().lock();
        try {
            resolutionCache.clear();
            Integer serializerId = serializerNames.get(serializerName);
            classMappings.put(type.getName(), serializerId);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static void register(Class<?> type, Serializer s) {
        rwLock.writeLock().lock();
        try {
            resolutionCache.clear();
            bindings[s.identifier()] = s;
            serializerNames.put(s.getClass().getSimpleName(), s.identifier());
            classMappings.put(type.getName(), s.identifier());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public static void toBinary(Object o, ByteBuf buf) {
        Serializer s = null;
        if (o == null) {
            s = nullS; // NullSerializer
        } else {
            s = lookupSerializer(o.getClass());
        }
        if (s == null) {
            LOG.error("Object {} of type {} has no serialization rule!", o, o.getClass());
            return;
        }
        Integer sId = s.identifier();
        byte[] unserializedId = Ints.toByteArray(sId);
        LOG.trace("ID: {} ({}, {})", new Object[]{unserializedId, BYTES - idSerializationBytes.getBytes(), BYTES});
        byte[] serializedId = Arrays.copyOfRange(Ints.toByteArray(sId), BYTES - idSerializationBytes.getBytes(), BYTES);
        buf.writeBytes(serializedId);
        LOG.debug("Using serializer {} for object {} (sID : {}) .", new Object[]{s, o, serializedId});
        s.toBinary(o, buf);
    }

    // this SHOULD be an Optional<Class> but java6 is moronic and doesn't read generics
    // from method declarations correctly. This is fixed in java8.
    public static Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        rwLock.readLock().lock();
        try {
            Serializer s = null;
            if (hint.isPresent()) {
                Object ho = hint.get();
                if (ho instanceof Class) {
                    Class c = (Class) hint.get(); // see comment above -.-
                    s = lookupSerializer(c);
                }
            }
            int sId = -1;
            if (s == null) {
                byte[] serializedId = new byte[idSerializationBytes.getBytes()];
                if (!(buf.readableBytes() >= serializedId.length)) {
                    LOG.error("Could not read serializer id ({}) from buffer with length {}!", serializedId.length, buf.readableBytes());
                }
                buf.readBytes(serializedId);
                byte[] unserializedId = new byte[BYTES];
                Arrays.fill(unserializedId, (byte) 0);
                System.arraycopy(serializedId, 0, unserializedId, BYTES - idSerializationBytes.getBytes(), serializedId.length);
                sId = Ints.fromByteArray(unserializedId);
                LOG.trace("DS-ID: {} ({}, {})", new Object[]{sId, serializedId, BYTES});

                s = bindings[sId];
            }
            if (s == null) {
                LOG.error("No deserializer {} found for buffer with hint {}.", sId, hint);
                return null;
            }
            LOG.debug("Using deserializer {} for buffer with hint {}.", s, hint);
            return s.fromBinary(buf, hint);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static Object fromBinary(ByteBuf buf, DatagramPacket datagram) {
        rwLock.readLock().lock();
        try {
            Serializer s = null;
            int sId = -1;
            byte[] serializedId = new byte[idSerializationBytes.getBytes()];
            if (!(buf.readableBytes() >= serializedId.length)) {
                LOG.error("Could not read serializer id ({}) from buffer with length {}!", serializedId.length, buf.readableBytes());
            }
            buf.readBytes(serializedId);
            byte[] unserializedId = new byte[BYTES];
            Arrays.fill(unserializedId, (byte) 0);
            System.arraycopy(serializedId, 0, unserializedId, BYTES - idSerializationBytes.getBytes(), serializedId.length);
            sId = Ints.fromByteArray(unserializedId);
            LOG.trace("DS-ID: {} ({}, {})", new Object[]{sId, serializedId, BYTES});

            s = bindings[sId];

            if (s == null) {
                LOG.error("No deserializer {} found for buffer with hint {}.", sId);
                return null;
            }
            LOG.debug("Using deserializer {} for buffer with hint {}.", s);
            if (s instanceof DatagramSerializer) {
                DatagramSerializer ds = (DatagramSerializer) s;
                return ds.fromBinary(buf, datagram);
            } else {
                LOG.warn("Datagram message was serialised with a Serializer that is not a DatagramSerializer: \n   s: {}", s.getClass());
                return s.fromBinary(buf, Optional.absent());
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static Serializer lookupSerializer(Class<? extends Object> aClass) {
        Serializer s = resolutionCache.get(aClass.getName());
        if (s != null) { // the fast path, no locking, concurrent map
            return s;
        }
        // the slow path on cache misses
        rwLock.readLock().lock();
        try {
            //printRules();
            Class<?> clazz = aClass;
            while (clazz != null) { // the long way around
                Integer sId = classMappings.get(clazz.getName());
                LOG.trace("Checked rule for {}, found: {}", clazz, sId);
                if (sId != null) {
                    resolutionCache.put(aClass.getName(), bindings[sId]); // remember this for later
                    return bindings[sId];
                }
                for (Class<?> intf : clazz.getInterfaces()) {
                    sId = classMappings.get(intf.getName());
                    LOG.trace("Checked rule for {}, found: {}", intf.getName(), sId);
                    if (sId != null) {
                        resolutionCache.put(aClass.getName(), bindings[sId]); // remember this for later
                        return bindings[sId];
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static Serializer getSerializer(int serializerId) {
        rwLock.readLock().lock();
        try {
            if ((serializerId < 0) || (serializerId >= bindings.length)) {
                return null;
            }
            return bindings[serializerId];
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public static Serializer getSerializer(String name) {
        rwLock.readLock().lock();
        try {
            Integer sId = serializerNames.get(name);
            if (sId != null) {
                return bindings[sId];
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return null;
    }

    public static void printRules() {
        rwLock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Current Ruleset: {\n");

            for (Entry<String, Integer> e : classMappings.entrySet()) {
                sb.append("    ");
                sb.append(e.getKey());
                sb.append(" -> ");
                sb.append(e.getValue());
                sb.append('\n');
            }
            sb.append("}\n");
            LOG.trace(sb.toString());
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
