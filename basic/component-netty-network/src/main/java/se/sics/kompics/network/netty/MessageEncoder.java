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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class MessageEncoder extends MessageToMessageEncoder<MessageNotify.Req> {
    
    private static final byte[] LENGTH_PLACEHOLDER = new byte[2];
    
    private final NettyNetwork component;
    
    public MessageEncoder(NettyNetwork component) {
        this.component = component;
    }

//    @Override
//    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
//        NettyNetwork.LOG.trace("Trying to encode outgoing data to {} from {}.", ctx.channel().remoteAddress(), ctx.channel().localAddress());
//        int startIdx = out.writerIndex();
//        out.writeBytes(LENGTH_PLACEHOLDER);
//
//        Serializers.toBinary(msg, out);
//
//        int endIdx = out.writerIndex();
//        int diff = endIdx - startIdx - LENGTH_PLACEHOLDER.length;
//        if (diff > 65532) { //2^16 - 2bytes for the length header (snappy wants no more than 65536 bytes uncompressed)
//            throw new Exception("Can't encode message longer than 65532 bytes!");
//        }
//        out.setShort(startIdx, diff);
//        NettyNetwork.LOG.trace("Encoded outgoing {} bytes of data to {}: {}.", new Object[]{diff, ctx.channel().remoteAddress(), ByteBufUtil.hexDump(out)});
//    }
    @Override
    protected void encode(ChannelHandlerContext ctx, MessageNotify.Req msgr, List<Object> outL) throws Exception {
        long startTS = System.nanoTime(); // start measuring here to avoid overestimating the throuhgput
        Msg msg = msgr.msg;
        ByteBuf out = ctx.alloc().buffer(NettyNetwork.INITIAL_BUFFER_SIZE, NettyNetwork.SEND_BUFFER_SIZE);
        component.LOG.trace("Trying to encode outgoing data to {} from {}: {}.", ctx.channel().remoteAddress(), ctx.channel().localAddress(), msgr.msg.getClass());
        int startIdx = out.writerIndex();
        out.writeBytes(LENGTH_PLACEHOLDER);
        
        try {
            if (msgr.notifyOfDelivery) {
                component.LOG.trace("Serialising message with AckRequest: {}", msgr.getMsgId());
                AckRequestMsg arm = new AckRequestMsg(msgr.msg, msgr.getMsgId());
                Serializers.toBinary(arm, out);
            } else {
                Serializers.toBinary(msgr.msg, out);
            }
        } catch (Throwable e) {
            component.LOG.warn("There was a problem serialising {}: \n --> {}", msgr, e);
            e.printStackTrace(System.err);
            throw e;
        }
        
        int endIdx = out.writerIndex();
        int diff = endIdx - startIdx - LENGTH_PLACEHOLDER.length;
        if (diff > 65532) { //2^16 - 2bytes for the length header (snappy wants no more than 65536 bytes uncompressed)
            throw new Exception("Can't encode message longer than 65532 bytes!");
        }
        out.setShort(startIdx, diff);
        //component.LOG.trace("Encoded outgoing {} bytes of data to {}: {}.", new Object[]{diff, ctx.channel().remoteAddress(), ByteBufUtil.hexDump(out)});
        msgr.injectSize(diff, startTS);
        outL.add(out);
    }
    
}
