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
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.DisambiguateConnection;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public abstract class SpecialSerializers {

    public static class NullSerializer implements Serializer {

        @Override
        public int identifier() {
            return 0;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            // simply ignore input
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Class> hint) {
            return null;
        }

    }

    public static class ByteSerializer implements Serializer {

        @Override
        public int identifier() {
            return 1;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            byte[] bytes = (byte[]) o;
            int code = buf.ensureWritable(bytes.length + 4, true);
            if (code == 1 || code == 3) {
                Serializers.LOG.error("ByteSerializer: Not enough space left on buffer to serialize " + bytes.length + " bytes.");
                return;
            }
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Class> hint) {
            int length = buf.readInt();
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            return bytes;
        }

    }

    public static class AddressSerializer implements Serializer {

        public static final int BYTE_KEY_SIZE = 255;
        public static final int INT_BYTE_SIZE = Integer.SIZE / 8;

        @Override
        public int identifier() {
            return 2;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            Address addr = (Address) o;
            if (addr == null) {
                buf.writeInt(0); //simply put four 0 bytes since 0.0.0.0 is not a valid host ip
                return;
            }
            int length = 6 + 4 + addr.getId().length;
            int code = buf.ensureWritable(length, true);
            if (code == 1 || code == 3) {
                Serializers.LOG.error("AddressSerializer: Not enough space left on buffer to serialize " + length + " bytes.");
                return;
            }

            buf.writeBytes(addr.getIp().getAddress());
            // Write ports as 2 bytes instead of 4
            byte[] portBytes = Ints.toByteArray(addr.getPort());
            buf.writeByte(portBytes[2]);
            buf.writeByte(portBytes[3]);
            byte[] id = addr.getId();
            BitBuffer bbuf = BitBuffer.create(false, (id == null));
            boolean byteFlag = (id != null) && (id.length <= BYTE_KEY_SIZE);
            bbuf.write(byteFlag);
            byte[] flags = bbuf.finalise();
            buf.writeBytes(flags);
            if (id != null) {
                if (byteFlag) {
                    buf.writeByte(UnsignedBytes.checkedCast(id.length));
                } else {
                    buf.writeInt(id.length);
                }
                buf.writeBytes(id);
            }
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Class> hint) {
            byte[] ipBytes = new byte[4];
            buf.readBytes(ipBytes);
            if ((ipBytes[0] == 0) && (ipBytes[1] == 0) && (ipBytes[2] == 0) && (ipBytes[3] == 0)) {
                return null; // IP 0.0.0.0 is not valid but null Address encoding
            }
            InetAddress ip;
            try {
                ip = InetAddress.getByAddress(ipBytes);
            } catch (UnknownHostException ex) {
                Serializers.LOG.error("AddressSerializer: Could not create InetAddress.", ex);
                return null;
            }
            byte portUpper = buf.readByte();
            byte portLower = buf.readByte();
            int port = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);

            byte[] flagBytes = new byte[1];
            buf.readBytes(flagBytes);
            boolean[] flags = BitBuffer.extract(3, flagBytes);
            boolean infFlag = flags[0];
            boolean nullFlag = flags[1];
            boolean byteFlag = flags[2];

            byte[] id;

            if (nullFlag || infFlag) {
                id = null;
                return new Address(ip, port, id);
            }
            int keySize;
            if (byteFlag) {
                keySize = UnsignedBytes.toInt(buf.readByte());
            } else {
                keySize = buf.readInt();
            }
            id = new byte[keySize];
            buf.readBytes(id);

            return new Address(ip, port, id);
        }

    }

    public static class DisambiguateSerializer implements Serializer {

        public static final boolean REQ = false;
        public static final boolean RESP = true;

        private static final AddressSerializer addrS = new AddressSerializer();

        @Override
        public int identifier() {
            return 5;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            if (o instanceof DisambiguateConnection.Req) {
                DisambiguateConnection.Req r = (DisambiguateConnection.Req) o;
                reqToBinary(r, buf);
                return;
            }
            if (o instanceof DisambiguateConnection.Resp) {
                DisambiguateConnection.Resp r = (DisambiguateConnection.Resp) o;
                respToBinary(r, buf);
                return;
            }
            throw new RuntimeException("Can't serialize " + o.getClass() + " with this serializer!");
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Class> hint) {
            byte[] flagB = new byte[1];
            buf.readBytes(flagB);
            boolean[] flags = BitBuffer.extract(7, flagB);
            boolean isResp = flags[0];
            boolean sourceEqOrigin = flags[1];
            Transport proto = null;
            if (flags[2]) {
                proto = Transport.UDP;
            }
            if (flags[3]) {
                proto = Transport.TCP;
            }
            if (flags[4]) {
                proto = Transport.MULTICAST_UDP;
            }
            if (flags[5]) {
                proto = Transport.UDT;
            }
            if (flags[6]) {
                proto = Transport.LEDBAT;
            }
            if (isResp) {
                return respFromBinary(buf, sourceEqOrigin, proto);
            } else {
                return reqFromBinary(buf, sourceEqOrigin, proto);
            }
        }

        private void reqToBinary(DisambiguateConnection.Req r, ByteBuf buf) {
            // Flags 1byte
            boolean sourceEqOrigin = r.getSource().equals(r.getOrigin());
            BitBuffer bbuf = BitBuffer.create(REQ, // good that a byte has so many bits... can compress it more if more protocols are necessary
                    sourceEqOrigin,
                    r.getProtocol() == Transport.UDP,
                    r.getProtocol() == Transport.TCP,
                    r.getProtocol() == Transport.MULTICAST_UDP,
                    r.getProtocol() == Transport.UDT,
                    r.getProtocol() == Transport.LEDBAT);
            byte[] bbufb = bbuf.finalise();
            buf.writeBytes(bbufb);
            // Addresses
            addrS.toBinary(r.getSource(), buf);
            addrS.toBinary(r.getDestination(), buf);
            if (!sourceEqOrigin) {
                addrS.toBinary(r.getOrigin(), buf);
            }
            // Ports 2 byte each
            byte[] portBytes = Ints.toByteArray(r.localPort);
            buf.writeByte(portBytes[2]);
            buf.writeByte(portBytes[3]);
            portBytes = Ints.toByteArray(r.udtPort);
            buf.writeByte(portBytes[2]);
            buf.writeByte(portBytes[3]);
        }

        private void respToBinary(DisambiguateConnection.Resp r, ByteBuf buf) {
            // Flags 1byte
            boolean sourceEqOrigin = r.getSource().equals(r.getOrigin());
            BitBuffer bbuf = BitBuffer.create(RESP, // good that a byte has so many bits... can compress it more if more protocols are necessary
                    sourceEqOrigin,
                    r.getProtocol() == Transport.UDP,
                    r.getProtocol() == Transport.TCP,
                    r.getProtocol() == Transport.MULTICAST_UDP,
                    r.getProtocol() == Transport.UDT,
                    r.getProtocol() == Transport.LEDBAT);
            byte[] bbufb = bbuf.finalise();
            buf.writeBytes(bbufb);
            // Addresses
            addrS.toBinary(r.getSource(), buf);
            addrS.toBinary(r.getDestination(), buf);
            if (!sourceEqOrigin) {
                addrS.toBinary(r.getOrigin(), buf);
            }
            // Ports 2 byte each
            byte[] portBytes = Ints.toByteArray(r.localPort);
            buf.writeByte(portBytes[2]);
            buf.writeByte(portBytes[3]);
            portBytes = Ints.toByteArray(r.boundPort);
            buf.writeByte(portBytes[2]);
            buf.writeByte(portBytes[3]);
            portBytes = Ints.toByteArray(r.udtPort);
            buf.writeByte(portBytes[2]);
            buf.writeByte(portBytes[3]);
        }

        private Object respFromBinary(ByteBuf buf, boolean sourceEqOrigin, Transport proto) {
            // Addresses
            Address src = (Address) addrS.fromBinary(buf, Optional.absent());
            Address dst = (Address) addrS.fromBinary(buf, Optional.absent());
            Address orig = src; // We don't care about it, buf if it's there it needs to be read to move the buffer pointer
            if (!sourceEqOrigin) {
                orig = (Address) addrS.fromBinary(buf, Optional.absent());
            }
            // Ports 2 byte each
            byte portUpper = buf.readByte();
            byte portLower = buf.readByte();
            int localPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            portUpper = buf.readByte();
            portLower = buf.readByte();
            int boundPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            portUpper = buf.readByte();
            portLower = buf.readByte();
            int udtPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            return new DisambiguateConnection.Resp(src, dst, proto, localPort, boundPort, udtPort);
        }

        private Object reqFromBinary(ByteBuf buf, boolean sourceEqOrigin, Transport proto) {
            // Addresses
            Address src = (Address) addrS.fromBinary(buf, Optional.absent());
            Address dst = (Address) addrS.fromBinary(buf, Optional.absent());
            Address orig = src; // We don't care about it, buf if it's there it needs to be read to move the buffer pointer
            if (!sourceEqOrigin) {
                orig = (Address) addrS.fromBinary(buf, Optional.absent());
            }
            // Ports 2 byte each
            byte portUpper = buf.readByte();
            byte portLower = buf.readByte();
            int localPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            portUpper = buf.readByte();
            portLower = buf.readByte();
            int udtPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
            return new DisambiguateConnection.Req(src, dst, proto, localPort, udtPort);
        }

    }

    public static class BitBuffer {

        private static final int ZERO = 0;
        private static final int[] POS = {1, 2, 4, 8, 16, 32, 64, 128};

        private final ArrayList<Boolean> buffer = new ArrayList<Boolean>();

        private BitBuffer() {
        }

        public static BitBuffer create(Boolean... args) {
            BitBuffer b = new BitBuffer();
            b.buffer.addAll(Arrays.asList(args));
            return b;
        }

        public BitBuffer write(Boolean... args) {
            buffer.addAll(Arrays.asList(args));
            return this;
        }

        public byte[] finalise() {
            int numBytes = (int) Math.ceil(((double) buffer.size()) / 8.0);
            byte[] bytes = new byte[numBytes];
            for (int i = 0; i < numBytes; i++) {
                int b = ZERO;
                for (int j = 0; j < 8; j++) {
                    int pos = i * 8 + j;
                    if (buffer.size() > pos) {
                        if (buffer.get(pos)) {
                            b = b ^ POS[j];
                        }
                    }
                }
                bytes[i] = UnsignedBytes.checkedCast(b);
            }
            return bytes;
        }

        public static boolean[] extract(int numValues, byte[] bytes) {
            assert (((int) Math.ceil(((double) numValues) / 8.0)) <= bytes.length);

            boolean[] output = new boolean[numValues];
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i];
                for (int j = 0; j < 8; j++) {
                    int pos = i * 8 + j;
                    if (pos >= numValues) {
                        return output;
                    }
                    output[pos] = ((b & POS[j]) != 0);
                }
            }

            return output;
        }
    }
}
