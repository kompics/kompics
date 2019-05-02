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
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.MDC;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class MessageDecoder extends LengthFieldBasedFrameDecoder {

    private final NettyNetwork component;

    public MessageDecoder(NettyNetwork component) {
        super(65532, 0, 2, 0, 2);
        this.component = component;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        component.setCustomMDC();
        try {
            component.extLog.trace("Trying to decode incoming {} bytes of data from {} to {}.", new Object[] {
                    frame.readableBytes(), ctx.channel().remoteAddress(), ctx.channel().localAddress() });
            Object o = Serializers.fromBinary(frame, Optional.empty());
            component.extLog.trace("Decoded incoming data from {}: {}", ctx.channel().remoteAddress(), o);
            if (o instanceof AckRequestMsg) {
                AckRequestMsg arm = (AckRequestMsg) o;
                component.extLog.trace("Got AckRequest for {}. Replying...", arm.id);
                NotifyAck an = arm.reply();
                ctx.channel().writeAndFlush(new MessageWrapper(an));
                return arm.content;
            } else if (o instanceof Msg) {
                return o;
            } else {
                component.extLog.warn("Got unexpected Stream message type: {} -> {}", o.getClass().getCanonicalName(),
                        o);
            }
            return o;
        } finally {
            MDC.clear();
        }
    }

    @Override
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        return buffer.slice(index, length);
    }

}
