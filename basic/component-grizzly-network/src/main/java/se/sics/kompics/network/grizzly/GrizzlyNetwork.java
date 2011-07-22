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
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.ConnectionStatusRequest;
import se.sics.kompics.network.ConnectionStatusResponse;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkConnectionRefused;
import se.sics.kompics.network.NetworkControl;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.NetworkSessionClosed;
import se.sics.kompics.network.NetworkSessionOpened;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.grizzly.kryo.KryoSerializationFilter;

/**
 * The <code>GrizzlyNetwork</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class GrizzlyNetwork extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(GrizzlyNetwork.class);

	/** The "implemented" Network port. */
	Negative<Network> net = negative(Network.class);

	/** The "implemented" NetworkControl port. */
	Negative<NetworkControl> netControl = negative(NetworkControl.class);

	private FilterChainBuilder filterChainBuilder;
	private TCPNIOTransportBuilder builder;
	private TCPNIOTransport transport;

	private HashMap<InetSocketAddress, GrizzlySession> tcpSession;
	private GrizzlyHandler tcpHandler;
	private InetSocketAddress localSocketAddress;
	int connectRetries;
	ExecutorService sendersPool;

	/**
	 * Instantiates a new Netty network component.
	 */
	public GrizzlyNetwork() {
		filterChainBuilder = FilterChainBuilder.stateless();

		tcpSession = new HashMap<InetSocketAddress, GrizzlySession>();
		tcpHandler = new GrizzlyHandler(this, Transport.TCP);

		sendersPool = Executors.newCachedThreadPool(new SenderThreadFactory());

		subscribe(handleInit, control);
		subscribe(handleMessage, net);

		subscribe(handleStatusReq, netControl);
	}

	/** The handle init. */
	Handler<GrizzlyNetworkInit> handleInit = new Handler<GrizzlyNetworkInit>() {
		public void handle(final GrizzlyNetworkInit init) {
			connectRetries = init.getConnectRetries();
			localSocketAddress = new InetSocketAddress(init.getSelf().getIp(),
					init.getSelf().getPort());

			boolean compress = init.getCompressionLevel() != 0;
			int initialBufferCapacity = init.getInitialBufferCapacity();
			int maxBufferCapacity = init.getMaxBufferCapacity();
			int workerCount = init.getWorkerCount();

			filterChainBuilder.add(new TransportFilter());
			// filterChainBuilder.add(new ProtostuffSerializationFilter());
			filterChainBuilder.add(new KryoSerializationFilter(compress,
					initialBufferCapacity, maxBufferCapacity));
			// filterChainBuilder.add(new JavaSerializationFilter());
			filterChainBuilder.add(tcpHandler);

			builder = TCPNIOTransportBuilder.newInstance();

			final ThreadPoolConfig config = ThreadPoolConfig.defaultConfig();
			config.setCorePoolSize(workerCount).setMaxPoolSize(workerCount)
					.setQueueLimit(-1);

			builder.setIOStrategy(SameThreadIOStrategy.getInstance())
					// .setIOStrategy(WorkerThreadIOStrategy.getInstance())
					// .setIOStrategy(SimpleDynamicNIOStrategy.getInstance())
					// .setIOStrategy(LeaderFollowerNIOStrategy.getInstance())
					.setKeepAlive(true).setReuseAddress(true)
					.setTcpNoDelay(true);

			transport = builder.build();

			transport.setProcessor(filterChainBuilder.build());
			transport.setConnectionTimeout(5000);
			transport.setReuseAddress(true);
			transport.setKeepAlive(true);
			transport.setTcpNoDelay(true);

			transport.setSelectorRunnersCount(init.getSelectorCount());
			transport.setWorkerThreadPoolConfig(config);

			// final GrizzlyJmxManager manager = GrizzlyJmxManager.instance();
			// JmxObject jmxTransportObject = transport.getMonitoringConfig()
			// .createManagementObject();
			// manager.registerAtRoot(jmxTransportObject, "GrizzlyTransport");

			try {
				transport.bind(localSocketAddress);
				transport.start();
			} catch (IOException e) {
				throw new RuntimeException("Grizzly cannot bind/start port", e);
			}
		}
	};

	/** The handle message. */
	Handler<Message> handleMessage = new Handler<Message>() {
		public void handle(Message message) {
			logger.debug(
					"Handling Message {} from {} to {} protocol {}",
					new Object[] { message, message.getSource(),
							message.getDestination(), message.getProtocol() });

			if (message.getDestination().getIp()
					.equals(message.getSource().getIp())
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
			case UDP:
				GrizzlySession tcpSession = getTcpSession(destination);
				tcpSession.sendMessage(message);
				break;
			}
		}
	};

	final void deliverMessage(Message message, Transport protocol) {
		message.setProtocol(protocol);
		logger.debug(
				"Delivering message {} from {} to {} protocol {}",
				new Object[] { message.toString(), message.getSource(),
						message.getDestination(), message.getProtocol() });

		trigger(message, net);

		GrizzlyListener.delivered(message);
	}

	private GrizzlySession getTcpSession(Address destination) {
		InetSocketAddress socketAddress = address2SocketAddress(destination);
		synchronized (tcpSession) {
			GrizzlySession session = tcpSession.get(socketAddress);
			if (session == null) {
				session = new GrizzlySession(transport, Transport.TCP,
						address2SocketAddress(destination), this);
				session.connectInit();
				tcpSession.put(socketAddress, session);
			}
			return session;
		}
	}

	final void dropSession(GrizzlySession s) {
		synchronized (tcpSession) {
			tcpSession.remove(s.getRemoteAddress());
		}
		trigger(new NetworkConnectionRefused(s.getRemoteAddress(),
				Transport.TCP), netControl);
	}

	final void networkException(NetworkException event) {
		trigger(event, netControl);
	}

	final void networkSessionClosed(Connection<?> channel) {
		InetSocketAddress clientSocketAddress = (InetSocketAddress) channel
				.getPeerAddress();
		synchronized (tcpSession) {
			tcpSession.remove(clientSocketAddress);
		}

		trigger(new NetworkSessionClosed(clientSocketAddress, Transport.TCP),
				netControl);
	}

	final void networkSessionOpened(Connection<?> session) {
		InetSocketAddress clientSocketAddress = (InetSocketAddress) session
				.getPeerAddress();
		trigger(new NetworkSessionOpened(clientSocketAddress, Transport.TCP),
				netControl);
	}

	private final InetSocketAddress address2SocketAddress(Address address) {
		return new InetSocketAddress(address.getIp(), address.getPort());
	}

	Handler<ConnectionStatusRequest> handleStatusReq = new Handler<ConnectionStatusRequest>() {
		public void handle(ConnectionStatusRequest event) {
			HashSet<InetSocketAddress> tcp = null;
			synchronized (tcpSession) {
				tcp = new HashSet<InetSocketAddress>(tcpSession.keySet());
			}
			trigger(new ConnectionStatusResponse(event, tcp,
					new HashSet<InetSocketAddress>()), netControl);
		}
	};
}
