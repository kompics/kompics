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
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

/**
 * The <code>NettySession</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: NettySession.java 636 2009-02-08 01:41:23Z Cosmin $
 */
public class NettySession implements ChannelFutureListener {

	private static final Logger logger = LoggerFactory
			.getLogger(NettySession.class);

	private ClientBootstrap clientBootstrap;
	private Channel channel;

	private ChannelFuture connectFuture;

	private Transport protocol;
	private InetSocketAddress remoteSocketAddress;

	private LinkedList<Message> pendingMessages;

	private Lock lock;
	private boolean connected;
	private NettyNetwork component;
	private int retries;

	public NettySession(ClientBootstrap clientBootstrap, Transport protocol,
			InetSocketAddress remoteSocketAddress, NettyNetwork component) {
		super();
		this.clientBootstrap = clientBootstrap;
		this.protocol = protocol;
		this.remoteSocketAddress = remoteSocketAddress;
		this.pendingMessages = new LinkedList<Message>();

		this.component = component;
		this.retries = 0;
		lock = new ReentrantLock();
		connected = false;
		channel = null;
	}

	public NettySession(Channel channel, Transport protocol,
			InetSocketAddress remoteSocketAddress, NettyNetwork component) {
		super();
		this.protocol = protocol;
		this.remoteSocketAddress = remoteSocketAddress;
		this.component = component;
		this.retries = 0;
		lock = new ReentrantLock();
		connected = true;
		this.channel = channel;
	}

	@Override
	public void operationComplete(ChannelFuture future) {
		doConnect(future);
	}

	public void doConnect(ChannelFuture connectFuture) {
		if (connectFuture.isSuccess()) {
			lock.lock();
			if (channel == null) {
				channel = connectFuture.getChannel();
			}

			logger.debug("Connected to {}", channel.getRemoteAddress());

			// send pending messages
			try {
				for (Message deliverEvent : pendingMessages) {
					logger.debug("Sending message {} to {}",
							deliverEvent.toString(),
							deliverEvent.getDestination());

					channel.write(deliverEvent);
				}
				connected = true;
			} finally {
				lock.unlock();
			}
		} else {
			if (retries < component.connectRetries) {
				retries++;
				logger.debug("Retrying {} connection to {}", protocol,
						remoteSocketAddress);
				connectInit();
			} else {
				logger.debug("Dropping {} connection to {}", protocol,
						remoteSocketAddress);
				// drop this session
				component.dropSession(this);
			}
		}
	}

	public void sendMessage(Message message) {
		lock.lock();
		try {
			if (connected) {
				channel.write(message);
			} else {
				pendingMessages.add(message);
			}
		} finally {
			lock.unlock();
		}
	}

	public void connectInit() {
		if (connected)
			return;
		connectFuture = clientBootstrap.connect(remoteSocketAddress);
		connectFuture.addListener(this);
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteSocketAddress;
	}

	public Transport getProtocol() {
		return protocol;
	}
}
