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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IoSession;
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

	/** The "implemented" Network port. */
	Negative<Network> net = negative(Network.class);

	/** The "implemented" NetworkControl port. */
	Negative<NetworkControl> netControl = negative(NetworkControl.class);

	private static final Logger logger = LoggerFactory
			.getLogger(MinaNetwork.class);

	/* Acceptors and connectors */
	/** The UDP acceptor. */
	private NioDatagramAcceptor udpAcceptor;

	/** The UDP connector. */
	private NioDatagramConnector udpConnector;

	/** The TCP acceptor. */
	private NioSocketAcceptor tcpAcceptor;

	/** The TCP connector. */
	private NioSocketConnector tcpConnector;

	/* Sessions maps */
	/**
	 * Map of accepted TCP sessions, indexed by the socket address of the remote
	 * process .
	 */
	private HashSet<InetSocketAddress> knownTcpSession;

	/**
	 * Map of initiated TCP sessions, indexed by the server socket address of
	 * the remote process.
	 */
	private HashMap<InetSocketAddress, MinaSession> tcpSession;
	private HashMap<InetSocketAddress, InetSocketAddress> tcpClient2Server;

	/** The UDP session. */
	private HashSet<InetSocketAddress> knownUdpSession;
	private HashMap<InetSocketAddress, MinaSession> udpSession;
	private HashMap<InetSocketAddress, InetSocketAddress> udpClient2Server;

	/** The network handler. */
	private MinaHandler tcpHandler;
	private MinaHandler udpHandler;

	/** The local socket address. */
	private InetSocketAddress localSocketAddress;

	/** The connect retries. */
	int connectRetries;

	/**
	 * Instantiates a new MINA network component.
	 */
	public MinaNetwork() {
		knownTcpSession = new HashSet<InetSocketAddress>();
		tcpSession = new HashMap<InetSocketAddress, MinaSession>();
		tcpClient2Server = new HashMap<InetSocketAddress, InetSocketAddress>();

		knownUdpSession = new HashSet<InetSocketAddress>();
		udpSession = new HashMap<InetSocketAddress, MinaSession>();
		udpClient2Server = new HashMap<InetSocketAddress, InetSocketAddress>();

		tcpHandler = new MinaHandler(this, Transport.TCP);
		udpHandler = new MinaHandler(this, Transport.UDP);

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
			udpAcceptor.setHandler(udpHandler);
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
			udpConnector.setHandler(udpHandler);
			DefaultIoFilterChainBuilder udpConnectorChain = udpConnector
					.getFilterChain();
			udpConnectorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));

			// TCP Acceptor
			tcpAcceptor = new NioSocketAcceptor();
			tcpAcceptor.setHandler(tcpHandler);
			DefaultIoFilterChainBuilder tcpAcceptorChain = tcpAcceptor
					.getFilterChain();
			tcpAcceptorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));
			tcpAcceptor.setReuseAddress(true);
			try {
				tcpAcceptor.bind(localSocketAddress);
			} catch (IOException e) {
				throw new RuntimeException("Cannot bind TCP port "
						+ localSocketAddress, e);
			}

			// TCP Connector
			tcpConnector = new NioSocketConnector();
			tcpConnector.setHandler(tcpHandler);
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
		InetSocketAddress socketAddress = address2SocketAddress(destination);
		synchronized (tcpSession) {
			MinaSession session = tcpSession.get(socketAddress);
			if (session == null) {
				session = new MinaSession(tcpConnector, Transport.TCP,
						address2SocketAddress(destination), this);
				session.connectInit();
				tcpSession.put(socketAddress, session);
				tcpClient2Server.put(socketAddress, socketAddress);
				knownTcpSession.add(socketAddress);
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
		InetSocketAddress socketAddress = address2SocketAddress(destination);
		synchronized (udpSession) {
			MinaSession session = udpSession.get(socketAddress);
			if (session == null) {
				session = new MinaSession(udpConnector, Transport.UDP,
						address2SocketAddress(destination), this);
				session.connectInit();
				udpSession.put(socketAddress, session);
				udpClient2Server.put(socketAddress, socketAddress);
				knownUdpSession.add(socketAddress);
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
				tcpClient2Server.remove(s.getRemoteAddress());
			}
			trigger(new NetworkConnectionRefused(s.getRemoteAddress(),
					Transport.TCP), netControl);
		}
		if (s.getProtocol() == Transport.UDP) {
			synchronized (udpSession) {
				udpSession.remove(s.getRemoteAddress());
				udpClient2Server.remove(s.getRemoteAddress());
			}
			trigger(new NetworkConnectionRefused(s.getRemoteAddress(),
					Transport.UDP), netControl);
		}
	}

	/**
	 * Deliver message.
	 * 
	 * @param message
	 *            the message
	 * @param protocol
	 *            the protocol
	 */
	final void deliverMessage(Message message, Transport protocol,
			IoSession session) {
		logger.debug("Delivering message {} from {} to {} protocol {}",
				new Object[] { message.toString(), message.getSource(),
						message.getDestination(), message.getProtocol() });

		/*
		 * if the message arrived on a new session, try to resolve the session:
		 * i.e., make it our outgoing session for that destination if we do not
		 * have one already, or, if we do, pick one of the two
		 */
		resolveSession(message, session, protocol);

		message.setProtocol(protocol);
		trigger(message, net);
	}

	private void resolveSession(Message message, IoSession session,
			Transport protocol) {
		if (protocol == Transport.TCP) {
			resolveTcpSession(message, session);
		} else if (protocol == Transport.UDP) {
			resolveUdpSession(message, session);
		}
	}

	private void resolveTcpSession(Message message, IoSession session) {
		InetSocketAddress sessionSocketAddress = (InetSocketAddress) session
				.getRemoteAddress();
		InetSocketAddress messageSocketAddress = address2SocketAddress(message
				.getSource());

		synchronized (knownTcpSession) {
			if (knownTcpSession.contains(sessionSocketAddress)) {
				// we are aware of this session. nothing to resolve
				return;
			}
			// first time we receive a message on an unknown session, we try
			// to resolve the session
			knownTcpSession.add(sessionSocketAddress);
		}

		synchronized (tcpSession) {
			MinaSession currentMinaSession = tcpSession
					.get(messageSocketAddress);
			if (currentMinaSession != null) {
				// we already have an outgoing session to this peer and now
				// we received a message on a different session. we need to
				// resolve
				if (keepMine(messageSocketAddress)) {
					// we keep our current MinaSession
					// the peer will close his
					;
				} else {
					// we replace our current MinaSession to this peer with
					// one that contains this IoSession
					currentMinaSession.replaceIoSession(session,
							messageSocketAddress, Transport.TCP);
					tcpClient2Server.remove(messageSocketAddress);
					tcpClient2Server.put(sessionSocketAddress,
							messageSocketAddress);
				}
			} else {
				// we don't already have a MinaSession to this peer. We use this
				currentMinaSession = new MinaSession(session, Transport.TCP,
						address2SocketAddress(message.getSource()), this);
				tcpSession.put(messageSocketAddress, currentMinaSession);
				tcpClient2Server
						.put(sessionSocketAddress, messageSocketAddress);
			}
		}
	}

	private void resolveUdpSession(Message message, IoSession session) {
		InetSocketAddress sessionSocketAddress = (InetSocketAddress) session
				.getRemoteAddress();
		InetSocketAddress messageSocketAddress = address2SocketAddress(message
				.getSource());

		synchronized (knownUdpSession) {
			if (knownUdpSession.contains(sessionSocketAddress)) {
				// we are aware of this session. nothing to resolve
				return;
			}
			// first time we receive a message on an unknown session, we try
			// to resolve the session
			knownUdpSession.add(sessionSocketAddress);
		}

		synchronized (udpSession) {
			MinaSession currentMinaSession = udpSession
					.get(messageSocketAddress);
			if (currentMinaSession != null) {
				// we already have an outgoing session to this peer and now
				// we received a message on a different session. we need to
				// resolve
				if (keepMine(messageSocketAddress)) {
					// we keep our current MinaSession
					// the peer will close his
					;
				} else {
					// we replace our current MinaSession to this peer with
					// one that contains this IoSession
					currentMinaSession.replaceIoSession(session,
							messageSocketAddress, Transport.UDP);
					udpClient2Server.remove(messageSocketAddress);
					udpClient2Server.put(sessionSocketAddress,
							messageSocketAddress);
				}
			} else {
				// we don't already have a MinaSession to this peer. We use this
				currentMinaSession = new MinaSession(session, Transport.UDP,
						address2SocketAddress(message.getSource()), this);
				udpSession.put(messageSocketAddress, currentMinaSession);
				udpClient2Server
						.put(sessionSocketAddress, messageSocketAddress);
			}
		}
	}

	private final boolean keepMine(InetSocketAddress remoteSocketAddress) {
		if (remoteSocketAddress.getPort() > localSocketAddress.getPort()) {
			return true;
		}
		if (ip2long(remoteSocketAddress.getAddress()) > ip2long(localSocketAddress
				.getAddress())) {
			return true;
		}
		return false;
	}

	/**
	 * Network exception.
	 * 
	 * @param event
	 *            the event
	 */
	final void networkException(NetworkException event) {
		trigger(event, netControl);
	}

	final void networkSessionClosed(IoSession session) {
		InetSocketAddress clientSocketAddress = (InetSocketAddress) session
				.getRemoteAddress();
		Transport protocol = (Transport) session.getAttribute("protocol");

		if (protocol == Transport.TCP) {
			synchronized (knownTcpSession) {
				knownTcpSession.remove(clientSocketAddress);
			}
			synchronized (tcpSession) {
				InetSocketAddress serverSocketAddress = tcpClient2Server
						.get(clientSocketAddress);
				tcpSession.remove(serverSocketAddress);
				tcpClient2Server.remove(clientSocketAddress);
			}
		} else if (protocol == Transport.UDP) {
			synchronized (knownUdpSession) {
				knownUdpSession.remove(clientSocketAddress);
			}
			synchronized (udpSession) {
				InetSocketAddress serverSocketAddress = udpClient2Server
						.get(clientSocketAddress);
				udpSession.remove(serverSocketAddress);
				udpClient2Server.remove(clientSocketAddress);
			}
		}

		trigger(new NetworkSessionClosed(clientSocketAddress, protocol),
				netControl);
	}

	final void networkSessionOpened(IoSession session) {
		InetSocketAddress clientSocketAddress = (InetSocketAddress) session
				.getRemoteAddress();
		Transport protocol = (Transport) session.getAttribute("protocol");

		trigger(new NetworkSessionOpened(clientSocketAddress, protocol),
				netControl);
	}

	private final InetSocketAddress address2SocketAddress(Address address) {
		return new InetSocketAddress(address.getIp(), address.getPort());
	}

	private final long ip2long(InetAddress address) {
		return new BigInteger(address.getAddress()).longValue();
	}
}
