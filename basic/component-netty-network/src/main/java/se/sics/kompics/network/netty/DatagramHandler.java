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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.MDC;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
@ChannelHandler.Sharable
public class DatagramHandler extends BaseHandler<DatagramPacket> {

    public DatagramHandler(NettyNetwork component, Transport protocol) {
        super(component, protocol);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        component.setCustomMDC();
        try {
            Object o = Serializers.fromBinary(msg.content(), msg);

            if (o instanceof AckRequestMsg) {
                AckRequestMsg arm = (AckRequestMsg) o;
                ByteBuf buf = ctx.alloc().ioBuffer(NettyNetwork.INITIAL_BUFFER_SIZE, NettyNetwork.SEND_BUFFER_SIZE);
                component.deliverMessage(arm.content, ctx.channel());
                component.extLog.trace("Got AckRequest for {}. Replying...", arm.id);
                NotifyAck ack = arm.reply();
                Serializers.toBinary(ack, buf);
                DatagramPacket pack = new DatagramPacket(buf, ack.getDestination().asSocket());
                component.extLog.trace("Sending Datagram ACK {} ({}bytes)", ack, buf.readableBytes());
                ctx.writeAndFlush(pack);
            } else if (o instanceof Msg) {
                Msg m = (Msg) o;
                component.deliverMessage(m, ctx.channel());
            } else {
                component.extLog.warn("Got unexpected Datagram message type: {} -> {}", o.getClass().getCanonicalName(), o);
            }
        } catch (Exception e) { // Catch anything...the Serializer could throw any kind of weird exception if you get message that were send by someone else
            component.extLog.warn("Got weird Datagram message, ignoring it: {}", ByteBufUtil.hexDump(msg.content()));
            component.extLog.trace("Exception was: \n{}", e);
        } finally {
            MDC.clear();
        }
    }

}
