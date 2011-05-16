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
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibEncoder;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
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

/**
 * The <code>NettyNetwork</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class NettyNetwork extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(NettyNetwork.class);

	/** The "implemented" Network port. */
	Negative<Network> net = negative(Network.class);

	/** The "implemented" NetworkControl port. */
	Negative<NetworkControl> netControl = negative(NetworkControl.class);

	ServerBootstrap serverBootstrap;
	ClientBootstrap clientBootstrap;
	private HashMap<InetSocketAddress, NettySession> tcpSession;
	private NettyHandler tcpHandler;
	private InetSocketAddress localSocketAddress;
	int connectRetries;

	/**
	 * Instantiates a new Netty network component.
	 */
	public NettyNetwork() {
		serverBootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		clientBootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		tcpSession = new HashMap<InetSocketAddress, NettySession>();
		tcpHandler = new NettyHandler(this, Transport.TCP);

		subscribe(handleInit, control);
		subscribe(handleMessage, net);

		subscribe(handleStatusReq, netControl);
	}

	/** The handle init. */
	Handler<NettyNetworkInit> handleInit = new Handler<NettyNetworkInit>() {
		public void handle(final NettyNetworkInit init) {
			connectRetries = init.getConnectRetries();
			localSocketAddress = new InetSocketAddress(init.getSelf().getIp(),
					init.getSelf().getPort());

			final int compressionLevel = init.getCompressionLevel();

			serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				@Override
				public ChannelPipeline getPipeline() throws Exception {
					ChannelPipeline pipeline = Channels.pipeline();

					if (compressionLevel > 0) {
						pipeline.addLast("deflater", new ZlibEncoder(
								compressionLevel));
						pipeline.addLast("inflater", new ZlibDecoder());
					}

					pipeline.addLast("decoder", new ObjectDecoder());
					pipeline.addLast("encoder", new ObjectEncoder());

					// pipeline.addLast("logger", new LoggingHandler("Netty"));

					pipeline.addLast("handler", tcpHandler);
					return pipeline;
				}
			});
			serverBootstrap.setOption("child.tcpNoDelay", true);
			serverBootstrap.setOption("child.keepAlive", true);
			serverBootstrap.setOption("child.reuseAddress", true);
			serverBootstrap.setOption("child.connectTimeoutMillis", 5000);

			serverBootstrap.bind(localSocketAddress);

			clientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				public ChannelPipeline getPipeline() throws Exception {
					ChannelPipeline pipeline = Channels.pipeline();

					if (compressionLevel > 0) {
						pipeline.addLast("deflater", new ZlibEncoder(
								compressionLevel));
						pipeline.addLast("inflater", new ZlibDecoder());
					}

					pipeline.addLast("decoder", new ObjectDecoder());
					pipeline.addLast("encoder", new ObjectEncoder());

					// pipeline.addLast("logger", new LoggingHandler("Netty"));

					pipeline.addLast("handler", tcpHandler);
					return pipeline;
				}
			});
			clientBootstrap.setOption("tcpNoDelay", true);
			clientBootstrap.setOption("keepAlive", true);
			clientBootstrap.setOption("reuseAddress", true);
			clientBootstrap.setOption("connectTimeoutMillis", 5000);
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
				NettySession tcpSession = getTcpSession(destination);
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
	}

	private NettySession getTcpSession(Address destination) {
		InetSocketAddress socketAddress = address2SocketAddress(destination);
		synchronized (tcpSession) {
			NettySession session = tcpSession.get(socketAddress);
			if (session == null) {
				session = new NettySession(clientBootstrap, Transport.TCP,
						address2SocketAddress(destination), this);
				session.connectInit();
				tcpSession.put(socketAddress, session);
			}
			return session;
		}
	}

	final void dropSession(NettySession s) {
		synchronized (tcpSession) {
			tcpSession.remove(s.getRemoteAddress());
		}
		trigger(new NetworkConnectionRefused(s.getRemoteAddress(),
				Transport.TCP), netControl);
	}

	final void networkException(NetworkException event) {
		trigger(event, netControl);
	}

	final void networkSessionClosed(Channel channel) {
		InetSocketAddress clientSocketAddress = (InetSocketAddress) channel
				.getRemoteAddress();
		synchronized (tcpSession) {
			tcpSession.remove(clientSocketAddress);
		}

		trigger(new NetworkSessionClosed(clientSocketAddress, Transport.TCP),
				netControl);
	}

	final void networkSessionOpened(Channel session) {
		InetSocketAddress clientSocketAddress = (InetSocketAddress) session
				.getRemoteAddress();
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
