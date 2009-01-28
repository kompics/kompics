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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.ExceptionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkConnectionRefused;
import se.sics.kompics.network.NetworkControl;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.NetworkSessionClosed;
import se.sics.kompics.network.NetworkSessionOpened;
import se.sics.kompics.network.Transport;

/**
 * The <code>MinaNetwork</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public final class MinaNetwork extends ComponentDefinition {

	/** The net. */
	Negative<Network> net = negative(Network.class);

	/** The net control. */
	Negative<NetworkControl> netControl = negative(NetworkControl.class);

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory
			.getLogger(MinaNetwork.class);

	/* Acceptors and connectors */
	/** The udp acceptor. */
	private NioDatagramAcceptor udpAcceptor;

	/** The udp connector. */
	private NioDatagramConnector udpConnector;

	/** The tcp acceptor. */
	private NioSocketAcceptor tcpAcceptor;

	/** The tcp connector. */
	private NioSocketConnector tcpConnector;

	/* Sessions maps */
	/** The tcp session. */
	private HashMap<InetSocketAddress, MinaSession> tcpSession;

	/** The udp session. */
	private HashMap<InetSocketAddress, MinaSession> udpSession;

	/** The network handler. */
	private MinaHandler networkHandler;

	/** The local socket address. */
	private SocketAddress localSocketAddress;

	/** The connect retries. */
	int connectRetries;

	/**
	 * Instantiates a new mina network.
	 */
	public MinaNetwork() {
		tcpSession = new HashMap<InetSocketAddress, MinaSession>();
		udpSession = new HashMap<InetSocketAddress, MinaSession>();

		networkHandler = new MinaHandler(this);

		subscribe(handleInit, control);
		subscribe(handleMessage, net);
	}

	/** The handle init. */
	Handler<MinaNetworkInit> handleInit = new Handler<MinaNetworkInit>() {
		public void handle(MinaNetworkInit init) {
			connectRetries = init.getConnectRetries();
			localSocketAddress = new InetSocketAddress(init.getSelf().getIp(),
					init.getSelf().getPort());

			// UDP Acceptor
			udpAcceptor = new NioDatagramAcceptor();
			udpAcceptor.setHandler(networkHandler);
			DefaultIoFilterChainBuilder udpAcceptorChain = udpAcceptor
					.getFilterChain();
			udpAcceptorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));
			udpAcceptor.getSessionConfig().setReuseAddress(true);
			try {
				udpAcceptor.bind(localSocketAddress);
			} catch (IOException e) {
				throw new RuntimeException("Cannot bind UDP port "
						+ localSocketAddress, e);
			}

			// UDP Connector
			udpConnector = new NioDatagramConnector();
			udpConnector.setHandler(networkHandler);
			DefaultIoFilterChainBuilder udpConnectorChain = udpConnector
					.getFilterChain();
			udpConnectorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));

			// TCP Acceptor
			tcpAcceptor = new NioSocketAcceptor();
			tcpAcceptor.setHandler(networkHandler);
			DefaultIoFilterChainBuilder tcpAcceptorChain = tcpAcceptor
					.getFilterChain();
			tcpAcceptorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));
			tcpAcceptor.getSessionConfig().setReuseAddress(true);
			// tcpAcceptor.setReuseAddress(true);
			try {
				tcpAcceptor.bind(localSocketAddress);
			} catch (IOException e) {
				throw new RuntimeException("Cannot bind UDP port "
						+ localSocketAddress, e);
			}

			// TCP Connector
			tcpConnector = new NioSocketConnector();
			tcpConnector.setHandler(networkHandler);
			DefaultIoFilterChainBuilder tcpConnectorChain = tcpConnector
					.getFilterChain();
			tcpConnectorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));

			ExceptionMonitor.setInstance(new ExceptionMonitor() {
				@Override
				public void exceptionCaught(Throwable throwable) {
					logger.error("MINA exception: {}", throwable);
					trigger(new Fault(throwable), control);
				}
			});
		}
	};

	/** The handle message. */
	Handler<Message> handleMessage = new Handler<Message>() {
		public void handle(Message message) {
			logger.debug("Handling Message {} from {} to {} protocol {}",
					new Object[] { message, message.getSource(),
							message.getDestination(), message.getProtocol() });

			if (message.getDestination().getIp().equals(
					message.getSource().getIp())
					&& message.getDestination().getPort() == message
							.getSource().getPort()) {
				// deliver locally
				trigger(message, net);
				return;
			}

			Transport protocol = message.getProtocol();
			Address destination = message.getDestination();

			MinaSession session = protocol.equals(Transport.TCP) ? getTcpSession(destination)
					: getUdpSession(destination);

			session.sendMessage(message);
		}
	};

	/**
	 * Gets the tcp session.
	 * 
	 * @param destination
	 *            the destination
	 * 
	 * @return the tcp session
	 */
	private MinaSession getTcpSession(Address destination) {
		InetSocketAddress destinationSocket = address2SocketAddress(destination);
		synchronized (tcpSession) {
			MinaSession session = tcpSession.get(destinationSocket);

			if (session == null) {
				session = new MinaSession(tcpConnector, Transport.TCP,
						destinationSocket, this);
				session.connectInit();

				tcpSession.put(destinationSocket, session);
			}
			return session;
		}
	}

	/**
	 * Gets the udp session.
	 * 
	 * @param destination
	 *            the destination
	 * 
	 * @return the udp session
	 */
	private MinaSession getUdpSession(Address destination) {
		InetSocketAddress destinationSocket = address2SocketAddress(destination);
		MinaSession session = udpSession.get(destinationSocket);
		synchronized (udpSession) {
			if (session == null) {
				session = new MinaSession(udpConnector, Transport.UDP,
						destinationSocket, this);
				session.connectInit();

				udpSession.put(destinationSocket, session);
			}
			return session;
		}
	}

	/**
	 * Drop session.
	 * 
	 * @param s
	 *            the s
	 */
	final void dropSession(MinaSession s) {
		if (s.getProtocol() == Transport.TCP) {
			synchronized (tcpSession) {
				tcpSession.remove(s.getRemoteAddress());
			}
		}
		if (s.getProtocol() == Transport.UDP) {
			synchronized (udpSession) {
				udpSession.remove(s.getRemoteAddress());
			}
		}

		// save dropped session?
		trigger(new NetworkConnectionRefused(s.getRemoteAddress()), netControl);
	}

	/**
	 * Deliver message.
	 * 
	 * @param message
	 *            the message
	 * @param protocol
	 *            the protocol
	 */
	final void deliverMessage(Message message, Transport protocol) {
		logger.debug("Delivering message {} from {} to {} protocol {}",
				new Object[] { message.toString(), message.getSource(),
						message.getDestination(), message.getProtocol() });

		message.setProtocol(protocol);
		trigger(message, net);
	}

	/**
	 * Network exception.
	 * 
	 * @param event
	 *            the event
	 */
	void networkException(NetworkException event) {
		trigger(event, netControl);
	}

	/**
	 * Network session closed.
	 * 
	 * @param event
	 *            the event
	 */
	void networkSessionClosed(NetworkSessionClosed event) {
		trigger(event, netControl);
	}

	/**
	 * Network session opened.
	 * 
	 * @param event
	 *            the event
	 */
	void networkSessionOpened(NetworkSessionOpened event) {
		trigger(event, netControl);
	}

	/**
	 * Address2 socket address.
	 * 
	 * @param address
	 *            the address
	 * 
	 * @return the inet socket address
	 */
	private InetSocketAddress address2SocketAddress(Address address) {
		return new InetSocketAddress(address.getIp(), address.getPort());
	}
}
