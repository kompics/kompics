package se.sics.kompics.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.mina.core.ExceptionMonitor;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.EventHandler;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.network.events.NetworkConnectionRefused;
import se.sics.kompics.network.events.NetworkException;
import se.sics.kompics.network.events.NetworkSessionClosed;
import se.sics.kompics.network.events.NetworkSessionOpened;

@ComponentSpecification
public class Network {

	private static final Logger logger = LoggerFactory.getLogger(Network.class);

	private Component component;

	private Channel sendChannel, deliverChannel;

	/* Acceptors and connectors */
	private NioDatagramAcceptor udpAcceptor;

	private NioDatagramConnector udpConnector;

	private NioSocketAcceptor tcpAcceptor;

	private NioSocketConnector tcpConnector;

	/* Sessions maps */

	private HashMap<InetSocketAddress, Session> tcpSession;

	private HashMap<InetSocketAddress, Session> udpSession;

	private NetworkHandler networkHandler;

	private SocketAddress localSocketAddress;

	int connectRetries;

	public Network(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		this.sendChannel = sendChannel;
		this.deliverChannel = deliverChannel;

		component.subscribe(sendChannel, handleMessage);

		tcpSession = new HashMap<InetSocketAddress, Session>();
		udpSession = new HashMap<InetSocketAddress, Session>();

		networkHandler = new NetworkHandler(this);

	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		ComponentMembrane membrane = new ComponentMembrane(component);
		membrane.inChannel(Message.class, sendChannel);
		membrane.outChannel(Message.class, deliverChannel);
		membrane.outChannel(NetworkException.class, deliverChannel);
		membrane.outChannel(NetworkSessionOpened.class, deliverChannel);
		membrane.outChannel(NetworkSessionClosed.class, deliverChannel);
		membrane.outChannel(NetworkConnectionRefused.class, deliverChannel);
		membrane.seal();
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod("network.properties")
	public void init(Properties properties, SocketAddress socketAddress)
			throws IOException {

		connectRetries = Integer.parseInt(properties
				.getProperty("connect.retries"));

		// local address
		localSocketAddress = socketAddress;

		// UDP Acceptor
		udpAcceptor = new NioDatagramAcceptor();
		udpAcceptor.setHandler(networkHandler);
		DefaultIoFilterChainBuilder udpAcceptorChain = udpAcceptor
				.getFilterChain();
		udpAcceptorChain.addLast("protocol", new ProtocolCodecFilter(
				new ObjectSerializationCodecFactory()));
		udpAcceptor.getSessionConfig().setReuseAddress(true);
		udpAcceptor.bind(localSocketAddress);

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
//		tcpAcceptor.setReuseAddress(true);
		tcpAcceptor.bind(localSocketAddress);

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
				logger.debug("Exception caught:", throwable);
			}
		});
	}

	@ComponentDestroyMethod
	public void destroy() {
		for (Map.Entry<InetSocketAddress, Session> entry : tcpSession
				.entrySet()) {
			entry.getValue().closeInit();
		}
		for (Map.Entry<InetSocketAddress, Session> entry : udpSession
				.entrySet()) {
			entry.getValue().closeInit();
		}
		for (Map.Entry<InetSocketAddress, Session> entry : udpSession
				.entrySet()) {
			entry.getValue().closeWait();
		}
		for (Map.Entry<InetSocketAddress, Session> entry : tcpSession
				.entrySet()) {
			entry.getValue().closeWait();
		}

		tcpConnector.dispose();
		udpConnector.dispose();
		tcpAcceptor.unbind();
		tcpAcceptor.dispose();
		udpAcceptor.unbind();
		udpAcceptor.dispose();
	}

	@MayTriggerEventTypes(Message.class)
	private EventHandler<Message> handleMessage = new EventHandler<Message>() {
		public void handle(Message message) {
			logger.debug("Handling Message {} from {} to {} protocol {}",
					new Object[] { message, message.getSource(),
							message.getDestination(), message.getProtocol() });

			if (message.getDestination().getIp().equals(
					message.getSource().getIp())
					&& message.getDestination().getPort() == message
							.getSource().getPort()) {
				// deliver locally
				component.triggerEvent(message, deliverChannel);
				return;
			}

			Transport protocol = message.getProtocol();
			Address destination = message.getDestination();

			Session session = protocol.equals(Transport.TCP) ? getTcpSession(destination)
					: getUdpSession(destination);

			session.sendMessage(message);
		}
	};

	private Session getTcpSession(Address destination) {
		InetSocketAddress destinationSocket = address2SocketAddress(destination);
		synchronized (tcpSession) {
			Session session = tcpSession.get(destinationSocket);

			if (session == null) {
				session = new Session(tcpConnector, Transport.TCP,
						destinationSocket, this);
				session.connectInit();

				tcpSession.put(destinationSocket, session);
			}
			return session;
		}
	}

	private Session getUdpSession(Address destination) {
		InetSocketAddress destinationSocket = address2SocketAddress(destination);
		Session session = udpSession.get(destinationSocket);
		synchronized (udpSession) {
			if (session == null) {
				session = new Session(udpConnector, Transport.UDP,
						destinationSocket, this);
				session.connectInit();

				udpSession.put(destinationSocket, session);
			}
			return session;
		}
	}

	void dropSession(Session s) {
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

		component.triggerEvent(new NetworkConnectionRefused(s
				.getRemoteAddress()), deliverChannel);
	}

	void deliverMessage(Message message, Transport protocol) {
		logger.debug("Delivering message {} from {} to {} protocol {}",
				new Object[] { message.toString(), message.getSource(),
						message.getDestination(), message.getProtocol() });

		message.setProtocol(protocol);
		component.triggerEvent(message, deliverChannel);
	}

	void networkException(NetworkException event) {
		component.triggerEvent(event, deliverChannel);
	}

	void networkSessionClosed(NetworkSessionClosed event) {
		component.triggerEvent(event, deliverChannel);
	}

	void networkSessionOpened(NetworkSessionOpened event) {
		component.triggerEvent(event, deliverChannel);
	}

	private InetSocketAddress address2SocketAddress(Address address) {
		return new InetSocketAddress(address.getIp(), address.getPort());
	}
}
