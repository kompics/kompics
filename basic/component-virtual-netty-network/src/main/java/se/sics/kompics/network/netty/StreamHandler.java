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
package se.sics.kompics.network.netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
@Sharable
public class StreamHandler extends BaseHandler<Message> {
    
    public StreamHandler(NettyNetwork component, Transport protocol) {
        super(component, protocol);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Message msg) throws Exception {
        component.deliverMessage(msg);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        component.channelInactive(ctx, protocol);
        super.channelInactive(ctx);
    }
}
