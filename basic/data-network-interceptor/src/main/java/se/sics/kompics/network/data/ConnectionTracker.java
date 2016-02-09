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

import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.javatuples.Pair;
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
    private float ratio;

    ConnectionTracker(InetSocketAddress target, ProtocolSelectionPolicy psp, ProtocolRatioPolicy prp) {
        this.target = target;
        this.ratioPolicy = prp;
        this.selectionPolicy = psp;
        ratio = ratioPolicy.update(stats.avgThroughputApproximation(), stats.avgDeliveryTime());
        selectionPolicy.updateRatio(ratio);
    }

    ConnectionTracker(InetSocketAddress target, float initialRatio, ProtocolSelectionPolicy psp, ProtocolRatioPolicy prp) {
        this.target = target;
        this.ratioPolicy = prp;
        this.selectionPolicy = psp;
        this.ratio = initialRatio;
        //ratio = ratioPolicy.update(stats.avgThroughputApproximation(), stats.avgDeliveryTime());
        selectionPolicy.updateRatio(ratio);
    }

    void update() {
        if (stats.isUpdated()) {
            stats.endWindow();
            ratio = ratioPolicy.update(stats.avgThroughputApproximation(), stats.avgDeliveryTime());
            selectionPolicy.updateRatio(ratio);
            LOG.info("Current Stats to {}: { \n {} \n }", target, stats);
        } else {
            stats.endWindow(); // NOTE: can be an issue with really low throughput (below message size/windowLenth)
        }
    }

    void serialise(ByteBuf buf) {
        addrS.socketToBinary(target, buf);
        buf.writeFloat(ratio);
    }

    static ConnectionTracker fromBinary(ByteBuf buf, ProtocolSelectionPolicy psp, ProtocolRatioPolicy prp) {
        InetSocketAddress target = addrS.socketFromBinary(buf);
        ConnectionTracker ct = new ConnectionTracker(target, psp, prp);
        ct.ratio = buf.readFloat();
        ct.selectionPolicy.updateRatio(ct.ratio);
        return ct;
    }

    static Pair<InetSocketAddress, Float> fromBinary(ByteBuf buf) {
        InetSocketAddress target = addrS.socketFromBinary(buf);
        float ratio = buf.readFloat();
        return Pair.with(target, ratio);
    }

}
