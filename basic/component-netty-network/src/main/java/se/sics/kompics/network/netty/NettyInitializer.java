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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.SnappyFramedDecoder;
import io.netty.handler.codec.compression.SnappyFramedEncoder;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class NettyInitializer<C extends Channel> extends ChannelInitializer<C> {

    private final BaseHandler handler;

    /**
     *
     * @param handler
     * @param msgDecoderClass
     */
    public NettyInitializer(BaseHandler handler) {
        super();
        this.handler = handler;
    }

    /**
     * Initiate the Pipeline for the newly active connection.
     */
    @Override
    protected void initChannel(C ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // IN
        pipeline.addLast("decompressor", new SnappyFramedDecoder());
        pipeline.addLast("decoder", new MessageDecoder(handler.component));
        pipeline.addLast("handler", handler);
        //pipeline.addBefore("handler", "handlerLogger", new LoggingHandler("handlerLogger"));
        //pipeline.addBefore("handler", "decoder", new MessageDecoder());
        //pipeline.addBefore("decoder", "decoderLogger", new LoggingHandler("decoderLogger"));
        //pipeline.addBefore("decoderLogger", "deframer", new LengthFieldBasedFrameDecoder(65532, 0, 2)); //2^16 - 2bytes for the length header (snappy wants to more than 65536 bytes uncompressed)
        //pipeline.addBefore("deframer", "deframerLogger", new LoggingHandler("deframerLogger"));
        //pipeline.addBefore("decoder", "decompressor", new SnappyFramedDecoder());
        // OUT
        pipeline.addLast("compressor", new SnappyFramedEncoder());
        pipeline.addLast("encoder", new MessageEncoder(handler.component));
        //pipeline.addAfter("encoder", "encoderLogger", new LoggingHandler("encoderLogger"));
        //pipeline.addAfter("encoderLogger", "framer", new LengthFieldPrepender(2));
        //pipeline.addAfter("framer", "framerLogger", new LoggingHandler("framerLogger"));
        //pipeline.addAfter("encoder", "compressor", new SnappyFramedEncoder());
    }
}
