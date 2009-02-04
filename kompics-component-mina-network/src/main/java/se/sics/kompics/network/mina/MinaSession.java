/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
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
package se.sics.kompics.network.mina;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

/**
 * The <code>MinaSession</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class MinaSession implements IoFutureListener<IoFuture> {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(MinaSession.class);

	/** The io connector. */
	private IoConnector ioConnector;

	/** The io session. */
	private IoSession ioSession;

	/** The connect future. */
	private ConnectFuture connectFuture;

	/** The close future. */
	private CloseFuture closeFuture;

	/** The protocol. */
	private Transport protocol;

	/** The remote address. */
	private InetSocketAddress remoteAddress;

	/** The pending messages. */
	private LinkedList<Message> pendingMessages;

	/** The lock. */
	private Lock lock;

	/** The connected. */
	private boolean connected;

	/** The component. */
	private MinaNetwork component;

	/** The retries. */
	private int retries;

	/**
	 * Instantiates a new mina session.
	 * 
	 * @param ioConnector
	 *            the io connector
	 * @param protocol
	 *            the protocol
	 * @param address
	 *            the address
	 * @param component
	 *            the component
	 */
	public MinaSession(IoConnector ioConnector, Transport protocol,
			InetSocketAddress address, MinaNetwork component) {
		super();
		this.ioConnector = ioConnector;
		this.protocol = protocol;
		this.remoteAddress = address;
		this.pendingMessages = new LinkedList<Message>();

		this.component = component;
		this.retries = 0;
		lock = new ReentrantLock();
		connected = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.mina.core.future.IoFutureListener#operationComplete(org.apache
	 * .mina.core.future.IoFuture)
	 */
	public void operationComplete(IoFuture future) {
		ConnectFuture connFuture = (ConnectFuture) future;
		if (connFuture.isConnected()) {
			ioSession = future.getSession();

			// TODO Solve duplicate connection

			ioSession.setAttribute("address", remoteAddress);
			ioSession.setAttribute("protocol", protocol);

			logger.debug("Connected to {}", ioSession.getRemoteAddress());

			// send pending messages
			lock.lock();
			try {
				for (Message deliverEvent : pendingMessages) {
					logger.debug("Sending message {} to {}", deliverEvent
							.toString(), deliverEvent.getDestination());

					ioSession.write(deliverEvent);
				}
				connected = true;
			} finally {
				lock.unlock();
			}
		} else {
			if (retries < component.connectRetries) {
				retries++;
				logger.debug("Retrying {} connection to {}", protocol,
						remoteAddress);
				connectInit();
			} else {
				logger.debug("Dropping {} connection to {}", protocol,
						remoteAddress);
				// drop this session
				component.dropSession(this);
			}
		}
	}

	/**
	 * Send message.
	 * 
	 * @param deliverEvent
	 *            the deliver event
	 */
	public void sendMessage(Message deliverEvent) {
		lock.lock();
		try {
			if (connected) {
				ioSession.write(deliverEvent);
			} else {
				pendingMessages.add(deliverEvent);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Connect init.
	 */
	public void connectInit() {
		connectFuture = ioConnector.connect(remoteAddress);
		connectFuture.addListener(this);
	}

	/**
	 * Close init.
	 */
	public void closeInit() {
		closeFuture = ioSession.close(false);
	}

	/**
	 * Close wait.
	 */
	public void closeWait() {
		closeFuture.awaitUninterruptibly();
	}

	/**
	 * Gets the remote address.
	 * 
	 * @return the remote address
	 */
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * Gets the protocol.
	 * 
	 * @return the protocol
	 */
	public Transport getProtocol() {
		return protocol;
	}
}
