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
package se.sics.kompics.network.grizzly;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

/**
 * The <code>GrizzlySession</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GrizzlySession {

	private static final Logger logger = LoggerFactory
			.getLogger(GrizzlySession.class);

	private TCPNIOTransport clientBootstrap;
	Connection connection;

	private Transport protocol;
	private InetSocketAddress remoteSocketAddress;

	private LinkedList<Message> pendingMessages;

	private Lock lock;
	private boolean connected;
	private GrizzlyNetwork component;
	private int retries;

	public GrizzlySession(TCPNIOTransport clientBootstrap, Transport protocol,
			InetSocketAddress remoteSocketAddress, GrizzlyNetwork component) {
		super();
		this.clientBootstrap = clientBootstrap;
		this.protocol = protocol;
		this.remoteSocketAddress = remoteSocketAddress;
		this.pendingMessages = new LinkedList<Message>();

		this.component = component;
		this.retries = 0;
		lock = new ReentrantLock();
		connected = false;
	}

	public void connectComplete(Connection connection) {
		lock.lock();
		this.connection = connection;

		logger.debug("Connected to {}", connection.getPeerAddress());

		// send pending messages
		try {
			for (Message deliverEvent : pendingMessages) {
				logger.debug("Sending message {} to {}",
						deliverEvent.toString(), deliverEvent.getDestination());

				try {
					connection.write(deliverEvent);
				} catch (IOException e) {
					throw new RuntimeException("Grizzly exception", e);
				}
			}
			connected = true;
		} finally {
			lock.unlock();
		}
	}

	public void connectFailed() {
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

	public void sendMessage(final Message message) {
		lock.lock();
		try {
			if (connected) {
				component.sendersPool.execute(new Runnable() {
					public void run() {
						try {
							connection.write(message);
						} catch (IOException e) {
							throw new RuntimeException("Grizzly exception", e);
						}
					}
				});
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
		try {
			clientBootstrap.connect(remoteSocketAddress,
					new CompletionHandler<Connection>() {
						@Override
						public void updated(Connection result) {
						}

						@Override
						public void failed(Throwable throwable) {
							connectFailed();
						}

						@Override
						public void completed(Connection result) {
							connectComplete(result);
						}

						@Override
						public void cancelled() {
							connectFailed();
						}
					});
		} catch (IOException e) {
			throw new RuntimeException("Grizzly exception", e);
		}
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteSocketAddress;
	}

	public Transport getProtocol() {
		return protocol;
	}
}
