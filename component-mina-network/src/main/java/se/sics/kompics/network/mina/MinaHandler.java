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

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.Message;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.NetworkSessionClosed;
import se.sics.kompics.network.NetworkSessionOpened;
import se.sics.kompics.network.Transport;

/**
 * The <code>MinaHandler</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class MinaHandler extends IoHandlerAdapter {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(MinaHandler.class);

	/** The network component. */
	private MinaNetwork networkComponent;

	/**
	 * Instantiates a new mina handler.
	 * 
	 * @param networkComponent
	 *            the network component
	 */
	public MinaHandler(MinaNetwork networkComponent) {
		super();
		this.networkComponent = networkComponent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.mina.core.service.IoHandlerAdapter#exceptionCaught(org.apache
	 * .mina.core.session.IoSession, java.lang.Throwable)
	 */
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		InetSocketAddress address = (InetSocketAddress) session
				.getAttribute("address");

		if (address != null)
			logger.debug("Problems with {} connection to {}",
					(Transport) session.getAttribute("protocol"), address);

		logger.warn("Exception caught: {}-{} in {}/{}", new Object[] { cause,
				cause.getMessage(), address, session });
		StackTraceElement[] stackTrace = cause.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			logger.warn(" -> {}", stackTrace[i].toString());
		}

		networkComponent.networkException(new NetworkException(address));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.mina.core.service.IoHandlerAdapter#messageReceived(org.apache
	 * .mina.core.session.IoSession, java.lang.Object)
	 */
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		super.messageReceived(session, message);
		Transport protocol = (Transport) session.getAttribute("protocol");

		logger.debug("Message received from {}", session.getRemoteAddress());
		networkComponent.deliverMessage((Message) message, protocol);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.mina.core.service.IoHandlerAdapter#messageSent(org.apache.
	 * mina.core.session.IoSession, java.lang.Object)
	 */
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		super.messageSent(session, message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.mina.core.service.IoHandlerAdapter#sessionClosed(org.apache
	 * .mina.core.session.IoSession)
	 */
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		logger.debug("Connection closed to {}", session.getRemoteAddress());
		networkComponent.networkSessionClosed(new NetworkSessionClosed(session
				.getRemoteAddress()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.mina.core.service.IoHandlerAdapter#sessionOpened(org.apache
	 * .mina.core.session.IoSession)
	 */
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);
		logger.debug("Connection opened to {}", session.getRemoteAddress());
		networkComponent.networkSessionOpened(new NetworkSessionOpened(session
				.getRemoteAddress()));
	}
}
