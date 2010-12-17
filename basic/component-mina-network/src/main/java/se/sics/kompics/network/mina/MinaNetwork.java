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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.compression.CompressionFilter;
import org.apache.mina.filter.logging.LoggingFilter;
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
import se.sics.kompics.network.ConnectionStatusRequest;
import se.sics.kompics.network.ConnectionStatusResponse;
import se.sics.kompics.network.JoinMulticastGroup;
import se.sics.kompics.network.LeaveMulticastGroup;
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

	/**
	 * Map of initiated TCP sessions, indexed by the server socket address of
	 * the remote process.
	 */
	private HashMap<InetSocketAddress, MinaSession> tcpSession;

	/** The UDP session. */
	private HashMap<InetSocketAddress, MinaSession> udpSession;

	/** The network handler. */
	private MinaHandler tcpHandler;
	private MinaHandler udpHandler;

	/** The local socket address. */
	private InetSocketAddress localSocketAddress;

	/** The connect retries. */
	int connectRetries;

	private boolean multicastOn = false;
	private int multicastPort;
	private MulticastSocket multicastSocket = null;
	private DatagramSocket datagramSocket = null;

	/**
	 * Instantiates a new MINA network component.
	 */
	public MinaNetwork() {
		tcpSession = new HashMap<InetSocketAddress, MinaSession>();

		udpSession = new HashMap<InetSocketAddress, MinaSession>();

		tcpHandler = new MinaHandler(this, Transport.TCP);
		udpHandler = new MinaHandler(this, Transport.UDP);

		subscribe(handleInit, control);
		subscribe(handleMessage, net);
		subscribe(handleJoinMulticast, net);
		subscribe(handleLeaveMulticast, net);
		
		subscribe(handleStatusReq, netControl);
	}

	/** The handle init. */
	Handler<MinaNetworkInit> handleInit = new Handler<MinaNetworkInit>() {
		public void handle(MinaNetworkInit init) {
			connectRetries = init.getConnectRetries();
			multicastPort = init.getMulticastPort();
			localSocketAddress = new InetSocketAddress(init.getSelf().getIp(),
					init.getSelf().getPort());

			int compressionLevel = CompressionFilter.COMPRESSION_MAX;

			// UDP Acceptor
			udpAcceptor = new NioDatagramAcceptor();
			udpAcceptor.setHandler(udpHandler);
			DefaultIoFilterChainBuilder udpAcceptorChain = udpAcceptor
					.getFilterChain();
			// udpAcceptorChain.addLast("compress", new
			// CompressionFilter(compressionLevel));
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
			// udpConnectorChain.addLast("compress", new
			// CompressionFilter(compressionLevel));
			udpConnectorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));

			// TCP Acceptor
			tcpAcceptor = new NioSocketAcceptor();
			tcpAcceptor.setHandler(tcpHandler);
			DefaultIoFilterChainBuilder tcpAcceptorChain = tcpAcceptor
					.getFilterChain();
			tcpAcceptorChain.addLast("compress", new CompressionFilter(
					compressionLevel));
			tcpAcceptorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));
			tcpAcceptorChain.addLast("logger", new LoggingFilter("mina"));

			tcpAcceptor.setReuseAddress(true);
			try {
				tcpAcceptor.bind(localSocketAddress);
			} catch (IOException e) {
				throw new RuntimeException("Cannot bind TCP port "
						+ localSocketAddress, e);
			}

			// TCP Connector
			tcpConnector = new NioSocketConnector();
			tcpConnector.getSessionConfig().setTcpNoDelay(true); // Nagle's
			// algorithm
			// disabled
			tcpConnector.setHandler(tcpHandler);
			DefaultIoFilterChainBuilder tcpConnectorChain = tcpConnector
					.getFilterChain();
			tcpConnectorChain.addLast("compress", new CompressionFilter(
					compressionLevel));
			tcpConnectorChain.addLast("protocol", new ProtocolCodecFilter(
					new ObjectSerializationCodecFactory()));
			tcpConnectorChain.addLast("logger", new LoggingFilter("mina"));

			ExceptionMonitor.setInstance(new ExceptionMonitor() {
				@Override
				public void exceptionCaught(Throwable throwable) {
					logger.error("MINA exception: {}", throwable);
					trigger(new Fault(throwable), control);

					throw new RuntimeException("MINA exception", throwable);
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

			switch (protocol) {
			case TCP:
				MinaSession tcpSession = getTcpSession(destination);
				tcpSession.sendMessage(message);
				break;
			case UDP:
				MinaSession udpSession = getUdpSession(destination);
				udpSession.sendMessage(message);
				break;
			case MULTICAST_UDP:
				sendMulticast(message);
				break;
			}
		}
	};

	/* ======================== MULTICAST ========================== */

	// starts the multicast receiver
	// - a multicast socket to join/leave groups and receive messages
	// - a thread to blockingly receive messages
	private void startMulticast() throws IOException {
		if (multicastOn) {
			return;
		}

		multicastSocket = new MulticastSocket(multicastPort);
		multicastSocket.setReuseAddress(true);

		new Thread(new Runnable() {
			private byte[] buffer = new byte[65535];

			@Override
			public void run() {
				while (true) {
					try {
						DatagramPacket packet = new DatagramPacket(buffer,
								buffer.length);

						multicastSocket.receive(packet);

						ByteArrayInputStream bais = new ByteArrayInputStream(
								packet.getData(), packet.getOffset(), packet
										.getLength());
						ObjectInputStream ois = new ObjectInputStream(bais);

						Message message = (Message) ois.readObject();

						multicastReceived(message);
					} catch (IOException e) {
						logger.error("MINA exception: {}", e);
						trigger(new Fault(e), control);

						throw new RuntimeException("MINA exception", e);
					} catch (ClassNotFoundException e) {
						logger.error("MINA exception: {}", e);
						trigger(new Fault(e), control);

						throw new RuntimeException("MINA exception", e);
					}
				}
			}
		}).start();
	}

	// sends a message to a multicast receiver
	private void sendMulticast(Message message) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(message);
			oos.flush();
			oos.close();

			byte[] buffer = baos.toByteArray();

			DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
					message.getDestination().getIp(), message.getDestination()
							.getPort());

			if (datagramSocket == null) {
				// create a socket on first use only
				datagramSocket = new DatagramSocket();
			}
			datagramSocket.send(packet);
		} catch (IOException e) {
			logger.error("MINA exception: {}", e);
			trigger(new Fault(e), control);

			throw new RuntimeException("MINA exception", e);
		}
	}

	// handles a received multicast message
	private void multicastReceived(Message message) {
		if (addressesByGroup == null) {
			return;
		}

		InetAddress group = message.getDestination().getIp();

		HashSet<Address> addresses = addressesByGroup.get(group);

		if (addresses == null || addresses.isEmpty()) {
			return;
		}

		Iterator<Address> iter = addresses.iterator();
		Address first = iter.next();

		while (iter.hasNext()) {
			Address address = iter.next();

			Message m = (Message) message.clone();
			m.setDestination(address);
			trigger(m, net);
		}

		message.setDestination(first);
		trigger(message, net);
	}

	// node addresses interested in multicast group
	HashMap<InetAddress, HashSet<Address>> addressesByGroup;

	Handler<JoinMulticastGroup> handleJoinMulticast = new Handler<JoinMulticastGroup>() {
		public void handle(JoinMulticastGroup event) {
			InetAddress group = event.getGroup();
			Address address = event.getAddress();

			if (addressesByGroup == null) {
				addressesByGroup = new HashMap<InetAddress, HashSet<Address>>();
			}

			HashSet<Address> addresses = addressesByGroup.get(group);

			if (addresses == null) {
				addresses = new HashSet<Address>();
				addressesByGroup.put(group, addresses);
			}

			if (addresses.contains(address)) {
				// already joined this group
				return;
			}

			try {
				startMulticast(); // if not already started
			} catch (IOException e) {
				logger.error("MINA exception: {}", e);
				trigger(new Fault(e), control);

				throw new RuntimeException("MINA exception", e);
			}

			// join the group
			try {
				multicastSocket.joinGroup(group);

				// mark that we joined the group
				addresses.add(event.getAddress());
			} catch (IOException e) {
				logger.error("MINA exception: {}", e);
				trigger(new Fault(e), control);

				throw new RuntimeException("MINA exception", e);
			}
		}
	};

	Handler<LeaveMulticastGroup> handleLeaveMulticast = new Handler<LeaveMulticastGroup>() {
		public void handle(LeaveMulticastGroup event) {
			if (addressesByGroup == null) {
				return;
			}
			InetAddress group = event.getGroup();
			Address address = event.getAddress();

			HashSet<Address> addresses = addressesByGroup.get(group);

			if (addresses == null || !addresses.contains(address)) {
				return;
			}

			addresses.remove(address);

			if (addresses.isEmpty()) {
				addressesByGroup.remove(group);

				try {
					multicastSocket.leaveGroup(group);
				} catch (IOException e) {
					logger.error("MINA exception: {}", e);
					trigger(new Fault(e), control);

					throw new RuntimeException("MINA exception", e);
				}
			}
			if (addressesByGroup.isEmpty()) {
				addressesByGroup = null;
			}
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
			trigger(new NetworkConnectionRefused(s.getRemoteAddress(),
					Transport.TCP), netControl);
		}
		if (s.getProtocol() == Transport.UDP) {
			synchronized (udpSession) {
				udpSession.remove(s.getRemoteAddress());
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
		message.setProtocol(protocol);
		logger.debug("Delivering message {} from {} to {} protocol {}",
				new Object[] { message.toString(), message.getSource(),
						message.getDestination(), message.getProtocol() });

		trigger(message, net);
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
			synchronized (tcpSession) {
				tcpSession.remove(clientSocketAddress);
			}
		} else if (protocol == Transport.UDP) {
			synchronized (udpSession) {
				udpSession.remove(clientSocketAddress);
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

	Handler<ConnectionStatusRequest> handleStatusReq = new Handler<ConnectionStatusRequest>() {
		public void handle(ConnectionStatusRequest event) {
			HashSet<InetSocketAddress> tcp = null;
			HashSet<InetSocketAddress> udp = null;
			synchronized (tcpSession) {
				tcp = new HashSet<InetSocketAddress>(tcpSession.keySet());
			}
			synchronized (udpSession) {
				udp = new HashSet<InetSocketAddress>(udpSession.keySet());
			}
			trigger(new ConnectionStatusResponse(event,	tcp, udp), netControl);
		}
	};
}
