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

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.Message;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.Transport;

/**
 * The <code>GrizzlyHandler</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: GrizzlyHandler.java 4003 2011-05-22 09:24:32Z Cosmin $
 */
public class GrizzlyHandler extends BaseFilter {

	private static final Logger logger = LoggerFactory
			.getLogger(GrizzlyHandler.class);

	private final GrizzlyNetwork networkComponent;
	private final Transport protocol;

	public GrizzlyHandler(GrizzlyNetwork networkComponent, Transport protocol) {
		super();
		this.networkComponent = networkComponent;
		this.protocol = protocol;
	}

	@Override
	public NextAction handleRead(FilterChainContext ctx) throws IOException {
		logger.debug("Message received from {} msg={}", ctx.getAddress(),
				ctx.getMessage());
		networkComponent.deliverMessage((Message) ctx.getMessage(), protocol);
		return ctx.getStopAction();
	}

	@Override
	public void exceptionOccurred(FilterChainContext ctx, Throwable error) {
		InetSocketAddress address = (InetSocketAddress) ctx.getConnection()
				.getPeerAddress();

		if (address != null)
			logger.debug("Problems with {} connection to {}", protocol, address);

		logger.error("Exception caught: {}-{} in {}/{}", new Object[] { error,
				error.getMessage(), address, ctx.getConnection() });
		StackTraceElement[] stackTrace = error.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			logger.error(" -> {}", stackTrace[i].toString());
		}

		networkComponent.networkException(new NetworkException(address,
				protocol));

		throw new RuntimeException("Grizzly exception", error);
	}

	@Override
	public NextAction handleClose(FilterChainContext ctx) throws IOException {
		logger.debug("Connection closed to {}", ctx.getConnection()
				.getPeerAddress());
		networkComponent.networkSessionClosed(ctx.getConnection());
		return super.handleClose(ctx);
	}

	@Override
	public NextAction handleConnect(FilterChainContext ctx) throws IOException {
		logger.debug("Connection opened to {}", ctx.getConnection()
				.getPeerAddress());
		networkComponent.networkSessionOpened(ctx.getConnection());
		return super.handleConnect(ctx);
	}

	@Override
	public NextAction handleAccept(FilterChainContext ctx) throws IOException {
		logger.debug("Connection accepted from {}", ctx.getConnection()
				.getPeerAddress());
		networkComponent.networkSessionOpened(ctx.getConnection());
		return super.handleAccept(ctx);
	}
}
