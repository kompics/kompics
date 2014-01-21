/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009-2011 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009-2011 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
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

import java.net.InetSocketAddress;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.Message;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.Transport;

/**
 * The <code>NettyHandler</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class NettyHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory
			.getLogger(NettyHandler.class);

	private final NettyNetwork networkComponent;
	private final Transport protocol;

	public NettyHandler(NettyNetwork networkComponent, Transport protocol) {
		super();
		this.networkComponent = networkComponent;
		this.protocol = protocol;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		InetSocketAddress address = (InetSocketAddress) e.getChannel()
				.getRemoteAddress();

		if (address != null)
			logger.debug("Problems with {} connection to {}", protocol, address);

		logger.error("Exception caught: {}-{} in {}/{}",
				new Object[] { e.getCause(), e.getCause().getMessage(),
						address, e.getChannel() });
		StackTraceElement[] stackTrace = e.getCause().getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			logger.error(" -> {}", stackTrace[i].toString());
		}

		networkComponent.networkException(new NetworkException(address,
				protocol));

		//throw new RuntimeException("Netty exception", e.getCause());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		super.messageReceived(ctx, e);

		logger.debug("Message received from {} msg={}", e.getChannel()
				.getRemoteAddress(), e.getMessage());
		networkComponent.deliverMessage((Message) e.getMessage(), protocol);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		super.channelClosed(ctx, e);
		logger.debug("Connection closed to {}", e.getChannel()
				.getRemoteAddress());
		networkComponent.networkSessionClosed(e.getChannel());
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		super.channelOpen(ctx, e);
		logger.debug("Connection opened to {}", e.getChannel()
				.getRemoteAddress());
		networkComponent.networkSessionOpened(e.getChannel());
	}
}
