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
package se.sics.kompics.network.data;

import com.larskroll.common.Either;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import org.javatuples.Pair;
import org.jscience.mathematics.number.LargeInteger;
import org.jscience.mathematics.number.Rational;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import static se.sics.kompics.network.data.DataStreamInterceptor.LOG;
import se.sics.kompics.network.data.policies.ProtocolRatioPolicy;
import se.sics.kompics.network.data.policies.ProtocolSelectionPolicy;
import se.sics.kompics.network.netty.serialization.SpecialSerializers;

/**
 *
 * @author lkroll
 */
class ConnectionTracker {

    private static final SpecialSerializers.AddressSerializer addrS = SpecialSerializers.AddressSerializer.INSTANCE;

    final InetSocketAddress target;
    final Statistics stats = new Statistics();
    final ProtocolRatioPolicy ratioPolicy;
    final ProtocolSelectionPolicy selectionPolicy;
    private Rational ratio;
    private long inFlightMessages = 0;
    private final Queue<Either<MessageNotify.Req, Msg>> outstanding = new LinkedList<>();

    ConnectionTracker(InetSocketAddress target, ProtocolSelectionPolicy psp, ProtocolRatioPolicy prp) {
        this.target = target;
        this.ratioPolicy = prp;
        this.selectionPolicy = psp;
        ratio = ratioPolicy.update(stats.avgThroughputApproximation(), stats.avgDeliveryTime());
        selectionPolicy.updateRatio(ratio);
    }

    ConnectionTracker(InetSocketAddress target, Rational initialRatio, ProtocolSelectionPolicy psp, ProtocolRatioPolicy prp) {
        this.target = target;
        this.ratioPolicy = prp;
        this.selectionPolicy = psp;
        this.ratio = initialRatio;
        ratioPolicy.initialState(ratio);
        selectionPolicy.updateRatio(ratio);
    }

    Either<MessageNotify.Req, Msg> dequeue() {
        inFlightMessages++;
        return outstanding.poll();
    }

    void enqueue(MessageNotify.Req msg) {
        Either<MessageNotify.Req, Msg> e = Either.left(msg);
        outstanding.add(e);
    }
    
    void enqueue(Msg msg) {
        Either<MessageNotify.Req, Msg> e = Either.right(msg);
        outstanding.add(e);
    }
    
    void sent(UUID id) {
        inFlightMessages--;
    }

    boolean canSend(long maxInFlight) {
        return !outstanding.isEmpty() && (inFlightMessages < maxInFlight);
    }

    void update() {
        if (stats.isUpdated()) {
            stats.endWindow();
            ratio = ratioPolicy.update(stats.avgThroughputApproximation(), stats.avgDeliveryTime());
            selectionPolicy.updateRatio(ratio);
            LOG.info("Current Stats to {}: ratio={}, { \n {} \n }", new Object[]{target, ratio, stats});
        } else {
            stats.endWindow(); // NOTE: can be an issue with really low throughput (below message size/windowLenth)
        }
    }

    void serialise(ByteBuf buf) {
        addrS.socketToBinary(target, buf);
        writeRational(buf, ratio);
    }

    static ConnectionTracker fromBinary(ByteBuf buf, ProtocolSelectionPolicy psp, ProtocolRatioPolicy prp) {
        InetSocketAddress target = addrS.socketFromBinary(buf);
        ConnectionTracker ct = new ConnectionTracker(target, psp, prp);
        ct.ratio = readRational(buf);
        ct.selectionPolicy.updateRatio(ct.ratio);
        return ct;
    }

    static Pair<InetSocketAddress, Rational> fromBinary(ByteBuf buf) {
        InetSocketAddress target = addrS.socketFromBinary(buf);
        Rational ratio = readRational(buf);
        return Pair.with(target, ratio);
    }

    private static Rational readRational(ByteBuf buf) {
        int length = buf.readInt();
        byte[] data = new byte[length];
        buf.readBytes(data);
        int divisorLength = buf.readInt();
        LargeInteger divisor = LargeInteger.valueOf(data, 0, divisorLength);
        LargeInteger dividend = LargeInteger.valueOf(data, divisorLength, length - divisorLength);
        return Rational.valueOf(dividend, divisor);
    }

    private static void writeRational(ByteBuf buf, Rational r) {
        LargeInteger divisor = r.getDivisor();
        LargeInteger dividend = r.getDividend();
        int byteLength = (divisor.bitLength() >> 3) + (dividend.bitLength() >> 3) + 2;
        byte[] data = new byte[byteLength];
        int written = divisor.toByteArray(data, 0);
        int total = dividend.toByteArray(data, written);
        buf.writeInt(total);
        buf.writeInt(written);
        buf.writeBytes(data, 0, total);
    }

}
