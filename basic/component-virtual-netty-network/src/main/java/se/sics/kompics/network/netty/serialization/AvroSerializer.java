/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.netty.serialization;

import com.google.common.base.Optional;
import com.google.common.io.Closer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.network.netty.serialization.SpecialSerializers.BitBuffer;

/**
 *
 * @author lkroll
 */
public class AvroSerializer implements Serializer {

    private static final Logger LOG = LoggerFactory.getLogger(AvroSerializer.class);

    private static final int MY_ID = 7;

    private static final ConcurrentMap<Integer, SchemaEntry> idMap = new ConcurrentSkipListMap<Integer, SchemaEntry>();
    private static final ConcurrentMap<String, SchemaEntry> classMap = new ConcurrentHashMap<String, SchemaEntry>();

    private static final ReflectData rData = ReflectData.get();

    public static synchronized void register(int id, Class type) throws KeyExistsException, InvalidKeyException {
        register(id, type, false);
    }

    public static synchronized void register(int id, Class type, boolean force) throws KeyExistsException, InvalidKeyException {
        String typeName = type.getName();
        if (!force) {
            // Check first once, since schema generation is expensive
            if (idMap.containsKey(id)) {
                throw new KeyExistsException(id);
            }
            if (classMap.containsKey(typeName)) {
                throw new KeyExistsException(typeName);
            }
        }
        if (id < 0) {
            throw new InvalidKeyException(id);
        }
        Schema s = rData.getSchema(type);
        SchemaEntry se = new SchemaEntry(s, type, id, false);
        idMap.put(id, se);
        classMap.put(typeName, se);

        Serializers.register(type, MY_ID);
    }

    public static synchronized void register(int id, Class type, Schema schema) throws KeyExistsException, InvalidKeyException {
        register(id, type, schema, false);
    }

    public static synchronized void register(int id, Class type, Schema schema, boolean force) throws KeyExistsException, InvalidKeyException {
        String typeName = type.getName();
        if (!force) {
            // Check first once, since schema generation is expensive
            if (idMap.containsKey(id)) {
                throw new KeyExistsException(id);
            }
            if (classMap.containsKey(typeName)) {
                throw new KeyExistsException(typeName);
            }
        }
        if (id < 0) {
            throw new InvalidKeyException(id);
        }
        SchemaEntry se = new SchemaEntry(schema, type, id, true);
        idMap.put(id, se);
        classMap.put(typeName, se);

        Serializers.register(type, MY_ID);
    }

    @Override
    public int identifier() {
        return MY_ID;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        Class type = o.getClass();
        String typeName = type.getName();
        SchemaEntry se = classMap.get(typeName);
        if (se == null) {
            toBinaryNoSchema(o, type, buf);
        } else {
            toBinaryWithSchema(o, se, buf);
        }
    }

    private void toBinaryWithSchema(Object o, SchemaEntry se, ByteBuf buf) {
        BitBuffer flags;
        if (se.generated) {
            flags = BitBuffer.create(true, true); // with schema and generated
        } else {
            flags = BitBuffer.create(true, false); // with schema but not generated
        }
        byte[] flagsB = flags.finalise();
        buf.writeBytes(flagsB);
        buf.writeInt(se.id);
        try {
            Closer closer = Closer.create(); // Did I mention how much java6 sucks?
            try {
                ByteBufOutputStream out = closer.register(new ByteBufOutputStream(buf));
                BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(out, null);
                DatumWriter writer;
                if (se.generated) {
                    writer = new SpecificDatumWriter(se.schema);
                } else {
                    writer = new ReflectDatumWriter(se.schema);
                }
                writer.write(o, encoder);
                encoder.flush();
            } catch (Throwable ex) {
                LOG.error("Couldn't serialise object.", ex);
                closer.rethrow(ex);
            } finally {
                closer.close();
            }
        } catch (IOException ex) {
            LOG.error("Couldn't serialise object.", ex);
        }
    }

    private void toBinaryNoSchema(Object o, Class type, ByteBuf buf) {
        LOG.info("Prepending schema to object of type {}. This is not efficient. It's recommended to register the class instead.", type);
        Schema s;
        BitBuffer flags;
        DatumWriter refWriter;
        if (o instanceof GenericContainer) {
            GenericContainer ag = (GenericContainer) o;
            s = ag.getSchema();
            flags = BitBuffer.create(false, true); // no schema and generated
            refWriter = new SpecificDatumWriter(s);
        } else {
            s = rData.getSchema(type);
            flags = BitBuffer.create(false, false); // no schema and not generated
            refWriter = new ReflectDatumWriter(s);
        }
        byte[] flagsB = flags.finalise();
        buf.writeBytes(flagsB);
        try {
            Closer closer = Closer.create(); // Did I mention how much java6 sucks?
            try {
                ByteBufOutputStream out = closer.register(new ByteBufOutputStream(buf));
                DataFileWriter writer = closer.register(new DataFileWriter(refWriter).create(s, out));
                writer.append(o);
                writer.flush();
            } catch (Throwable ex) {
                LOG.error("Couldn't serialise object.", ex);
                closer.rethrow(ex);
            } finally {
                closer.close();
            }
        } catch (IOException ex) {
            LOG.error("Couldn't serialise object.", ex);
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        byte[] flagsB = new byte[1];
        buf.readBytes(flagsB);
        boolean[] flags = BitBuffer.extract(2, flagsB);
        boolean registered = flags[0];
        boolean generated = flags[1];
        if (registered) {
            int id = buf.readInt();
            SchemaEntry se = idMap.get(id);
            if (se == null) {
                LOG.warn("Could not deserialize object for id {}! Not registered!", id);
                return null;
            }
            return fromBinaryWithSchema(buf, se, generated);
        } else {
            return fromBinaryNoSchema(buf, generated);
        }
    }

    private Object fromBinaryNoSchema(ByteBuf buf, boolean generated) {
        DatumReader refReader;
        if (generated) {
            refReader = new SpecificDatumReader();
        } else {
            refReader = new ReflectDatumReader();
        }
        try {
            Closer closer = Closer.create();
            try {
                ByteBufInputStream in = closer.register(new ByteBufInputStream(buf));
                DataFileStream reader = closer.register(new DataFileStream(in, refReader));
                return reader.next(); // there should only be one
            } catch (Throwable ex) {
                LOG.error("Couldn't deserialise object.", ex);
                closer.rethrow(ex);
                return null;
            } finally {
                closer.close();
            }
        } catch (IOException ex) {
            LOG.error("Couldn't deserialise object.", ex);
            return null;
        }
    }

    private Object fromBinaryWithSchema(ByteBuf buf, SchemaEntry se, boolean generated) {
        DatumReader refReader;
        if (generated) {
            refReader = new SpecificDatumReader(se.schema);
        } else {
            refReader = new ReflectDatumReader(se.schema);
        }
        try {
            Closer closer = Closer.create();
            try {
                ByteBufInputStream in = closer.register(new ByteBufInputStream(buf));
                BinaryDecoder decoder = DecoderFactory.get().directBinaryDecoder(in, null);
                return refReader.read(null, decoder); // there should only be one
            } catch (Throwable ex) {
                LOG.error("Couldn't deserialise object.", ex);
                closer.rethrow(ex);
                return null;
            } finally {
                closer.close();
            }
        } catch (IOException ex) {
            LOG.error("Couldn't deserialise object.", ex);
            return null;
        }
    }

    private static class SchemaEntry {

        public final Schema schema;
        public final Class type;
        public final int id;
        public final boolean generated;

        public SchemaEntry(Schema schema, Class type, int id, boolean generated) {
            this.schema = schema;
            this.type = type;
            this.id = id;
            this.generated = generated;
        }
    }

    public static class KeyExistsException extends Exception {

        private final Object key;

        public KeyExistsException(Object key) {
            this.key = key;
        }

        @Override
        public String getMessage() {
            return "Key " + key + " already exists!";
        }
    }

    public static class InvalidKeyException extends Exception {

        private final int key;

        public InvalidKeyException(int key) {
            this.key = key;
        }

        @Override
        public String getMessage() {
            return "Key " + key + " is invalid! Must be positive integer.";
        }
    }
}
