/* 
 * This file is part of the CaracalDB distributed storage system.
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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import junit.framework.Assert;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.DisambiguateConnection;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
@RunWith(JUnit4.class)
public class SerializationTest {

    @Test
    public void byteBufTest() {
        ByteBuf buf = Unpooled.directBuffer();
        buf.writeInt(1234);
        int val = buf.readInt();
        assertEquals(1234, val);
    }

    @Test
    public void byteTest() {
        byte[] some = new byte[]{1, 2, 3, 4};
        ByteBuf buf = Unpooled.directBuffer();
        Serializers.toBinary(some, buf);
        System.out.println("Bytes: " + ByteBufUtil.hexDump(buf));
        byte[] someRes = (byte[]) Serializers.fromBinary(buf, Optional.absent());
        assertArrayEquals(some, someRes);
    }

    @Test
    public void nullTest() {
        Object some = null;
        ByteBuf buf = Unpooled.directBuffer();
        Serializers.toBinary(some, buf);
        System.out.println("Nulls: " + ByteBufUtil.hexDump(buf));
        Object someRes = Serializers.fromBinary(buf, Optional.absent());
        assertEquals(some, someRes);
    }

    @Test
    public void intTest() {
        Integer some = 1234;
        ByteBuf buf = Unpooled.directBuffer();
        Serializers.toBinary(some, buf);
        System.out.println("Ints: " + ByteBufUtil.hexDump(buf) + " : " + ByteBufUtil.hexDump(buf).length());
        Integer someRes = (Integer) Serializers.fromBinary(buf, Optional.absent());
        assertEquals(some, someRes);

        int someI = 1234;
        Serializers.toBinary(someI, buf);
        System.out.println("Ints2: " + ByteBufUtil.hexDump(buf) + " : " + ByteBufUtil.hexDump(buf).length());
        int someResI = (Integer) Serializers.fromBinary(buf, Optional.absent());
        assertEquals(someI, someResI);
    }

    @Test
    public void serializableTest() {
        SomeSerializable some = new SomeSerializable();
        ByteBuf buf = Unpooled.directBuffer();
        Serializers.toBinary(some, buf);
        System.out.println("SomeSerializable: " + ByteBufUtil.hexDump(buf) + " : " + ByteBufUtil.hexDump(buf).length());
        SomeSerializable someRes = (SomeSerializable) Serializers.fromBinary(buf, Optional.absent());
        assertEquals(some.getField(), someRes.getField());
    }

    @Test
    public void parentSerializableTest() {
        SomeSerializable someI = new SomeSerializable();
        ParentSome some = new ParentSome(someI);
        ByteBuf buf = Unpooled.directBuffer();
        Serializers.toBinary(some, buf);
        System.out.println("SomeParent: " + ByteBufUtil.hexDump(buf) + " : " + ByteBufUtil.hexDump(buf).length());
        ParentSome someRes = (ParentSome) Serializers.fromBinary(buf, Optional.absent());
        assertEquals(some.getMySer().getField(), someRes.getMySer().getField());
    }

    @Test
    public void addressTest() {
        try {
            Address addr = new Address(InetAddress.getByName("127.0.0.1"), 1234, new byte[]{1, 2, 3, 4});
            Address hostAddr = addr.hostAddress();
            ByteBuf buf = Unpooled.directBuffer();
            Serializers.toBinary(addr, buf);
            System.out.println("Address: " + ByteBufUtil.hexDump(buf));
            Address someRes = (Address) Serializers.fromBinary(buf, Optional.absent());
            assertEquals(addr, someRes);
            
            buf.clear();
            
            Serializers.toBinary(hostAddr, buf);
            System.out.println("HostAddress: " + ByteBufUtil.hexDump(buf));
            Address someHostRes = (Address) Serializers.fromBinary(buf, Optional.absent());
            assertEquals(hostAddr, someHostRes);
            
            buf.release();
        } catch (UnknownHostException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void disambTest() {
        for (Transport proto : Transport.values()) {
            try {
                Address src = new Address(InetAddress.getByName("127.0.0.1"), 1234, new byte[]{1, 2, 3, 4});
                Address dst = new Address(InetAddress.getByName("127.0.0.1"), 5678, new byte[]{5, 6, 7, 8});
                DisambiguateConnection.Req req = new DisambiguateConnection.Req(src, dst, proto, 1234, 5678);
                DisambiguateConnection.Resp resp = new DisambiguateConnection.Resp(src, dst, proto, 1234, 5678, 9876);
                ByteBuf buf = Unpooled.directBuffer();
                Serializers.toBinary(req, buf);
                System.out.println("DisambReq: " + ByteBufUtil.hexDump(buf));
                Object someRes = Serializers.fromBinary(buf, Optional.absent());
                Assert.assertNotNull(someRes);
                someRes = null;
                buf.clear();
                Serializers.toBinary(resp, buf);
                System.out.println("DisambResp: " + ByteBufUtil.hexDump(buf));
                someRes = Serializers.fromBinary(buf, Optional.absent());
                Assert.assertNotNull(someRes);
            } catch (UnknownHostException ex) {
                Assert.fail(ex.getMessage());
            }
        }
    }
    
    @Test
    public void uuidTest() {
        ByteBuf buf = Unpooled.directBuffer();
        UUID orig = UUID.randomUUID();
        Serializers.toBinary(orig, buf);
        System.out.println("UUID: " + ByteBufUtil.hexDump(buf));
        UUID copy = (UUID) Serializers.fromBinary(buf, Optional.absent());
        Assert.assertEquals(orig, copy);
    }
    
    @Test
    public void avroTest() throws AvroSerializer.KeyExistsException, AvroSerializer.InvalidKeyException {
        Serializers.register(new AvroSerializer(), "avroS");
        AvroSerializer.register(15, SomeAvro.class);
        Serializers.register(ParentSomeAvro.class, "avroS");
        AvroSerializer.register(16, SomeGeneratedAvro.class);
        SomeAvro some = new SomeAvro();
        ByteBuf buf = Unpooled.directBuffer();
        Serializers.toBinary(some, buf);
        System.out.println("AVRO - SomeAvro: " + ByteBufUtil.hexDump(buf) + " : " + ByteBufUtil.hexDump(buf).length());
        SomeAvro someRes = (SomeAvro) Serializers.fromBinary(buf, Optional.absent());
        assertEquals(some.getField(), someRes.getField());
        //
        buf.clear();
        //
        ParentSomeAvro someP = new ParentSomeAvro(some);
        Serializers.toBinary(someP, buf);
        System.out.println("AVRO - ParentSomeAvro: " + ByteBufUtil.hexDump(buf) + " : " + ByteBufUtil.hexDump(buf).length());
        ParentSomeAvro somePRes = (ParentSomeAvro) Serializers.fromBinary(buf, Optional.absent());
        assertEquals(someP.getMySer().getField(), somePRes.getMySer().getField());
        //
        buf.clear();
        //
        SomeGeneratedAvro sga = SomeGeneratedAvro.newBuilder()
                .setSomeNumber(1234).build();
        Serializers.toBinary(sga, buf);
        System.out.println("AVRO - SomeGeneratedAvro: " + ByteBufUtil.hexDump(buf) + " : " + ByteBufUtil.hexDump(buf).length());
        SomeGeneratedAvro sgaR = (SomeGeneratedAvro) Serializers.fromBinary(buf, Optional.absent());
        assertEquals(sga.getSomeNumber(), sgaR.getSomeNumber());
        //
        buf.release();
    }

    public static class SomeSerializable implements Serializable {

        private int someField = 12345;

        public void setField(int i) {
            someField = i;
        }

        public int getField() {
            return someField;
        }
    }
    
    public static class SomeAvro {

        private int someField = 12345;

        public void setField(int i) {
            someField = i;
        }

        public int getField() {
            return someField;
        }
    }

    public static class ParentSome implements Serializable {

        private SomeSerializable mySer;

        public ParentSome(SomeSerializable ss) {
            mySer = ss;
        }

        public void setMySer(SomeSerializable ss) {
            mySer = ss;
        }

        public SomeSerializable getMySer() {
            return mySer;
        }
    }
    
    public static class ParentSomeAvro {

        private SomeAvro mySer;

        ParentSomeAvro() {
            
        }
        
        public ParentSomeAvro(SomeAvro ss) {
            mySer = ss;
        }

        public void setMySer(SomeAvro ss) {
            mySer = ss;
        }

        public SomeAvro getMySer() {
            return mySer;
        }
    }
}
