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

import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.slf4j.MDC;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 * @param <M>
 */
@ChannelHandler.Sharable
public abstract class BaseHandler<M> extends SimpleChannelInboundHandler<M> {

    protected final NettyNetwork component;
    protected final Transport protocol;

    public BaseHandler(NettyNetwork component, Transport protocol) {
        this.component = component;
        this.protocol = protocol;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        component.setCustomMDC();
        try {
            component.extLog.trace("Channel connected: {} {} => {} ({})", new Object[]{protocol, ctx.channel().localAddress(), ctx.channel().remoteAddress(), ctx.channel()});
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        component.setCustomMDC();
        try {
            Channel channel = ctx.channel();
            SocketAddress address = channel.remoteAddress();
            InetSocketAddress inetAddress = null;

            if (address != null && address instanceof InetSocketAddress) {
                inetAddress = (InetSocketAddress) address;
                Address addr = new NettyAddress(inetAddress);
                component.networkException(new NetworkException("Error in Channel handler to " + addr, addr, protocol, Optional.of(cause)));
            }

            component.extLog.error("Error in channel.", cause);
            //cause.printStackTrace();
            component.extLog.error("Closing channel {} due to error.", channel);
            channel.close();
        } finally {
            MDC.clear();
        }
    }
}
