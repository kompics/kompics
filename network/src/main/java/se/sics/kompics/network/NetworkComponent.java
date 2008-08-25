package se.sics.kompics.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

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
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.events.NetworkDeliverEvent;
import se.sics.kompics.network.events.NetworkException;
import se.sics.kompics.network.events.NetworkSendEvent;
import se.sics.kompics.network.events.NetworkSessionClosed;
import se.sics.kompics.network.events.NetworkSessionOpened;

@ComponentSpecification
public class NetworkComponent {

	private static final Logger logger = LoggerFactory
			.getLogger(NetworkComponent.class);

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

	public NetworkComponent(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		this.sendChannel = sendChannel;
		this.deliverChannel = deliverChannel;

		component.subscribe(sendChannel, "handleNetworkSendEvent");

		tcpSession = new HashMap<InetSocketAddress, Session>();
		udpSession = new HashMap<InetSocketAddress, Session>();

		networkHandler = new NetworkHandler(this);

	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(NetworkSendEvent.class, sendChannel);
		map.put(NetworkDeliverEvent.class, deliverChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod
	public void init(SocketAddress socketAddress) throws IOException {
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

	@EventHandlerMethod
	@MayTriggerEventTypes(NetworkDeliverEvent.class)
	public void handleNetworkSendEvent(NetworkSendEvent event) {
		logger.debug("Handling NetSendEvent {} from {} to {} protocol {}",
				new Object[] { event.getNetworkDeliverEvent(),
						event.getSource(), event.getDestination(),
						event.getProtocol() });

		NetworkDeliverEvent deliverEvent = event.getNetworkDeliverEvent();
		if (event.getDestination().getIp().equals(event.getSource().getIp())
				&& event.getDestination().getPort() == event.getSource()
						.getPort()) {
			// deliver locally
			component.triggerEvent(deliverEvent, deliverChannel);
			return;
		}

		Transport protocol = event.getProtocol();
		Address destination = event.getDestination();

		Session session = protocol.equals(Transport.TCP) ? getTcpSession(destination)
				: getUdpSession(destination);

		session.sendMessage(deliverEvent);
	}

	private Session getTcpSession(Address destination) {
		InetSocketAddress destinationSocket = address2SocketAddress(destination);
		Session session = tcpSession.get(destinationSocket);

		if (session == null) {
			session = new Session(tcpConnector, Transport.TCP,
					destinationSocket);
			session.connectInit();

			tcpSession.put(destinationSocket, session);
		}

		return session;
	}

	private Session getUdpSession(Address destination) {
		InetSocketAddress destinationSocket = address2SocketAddress(destination);
		Session session = udpSession.get(destinationSocket);

		if (session == null) {
			session = new Session(udpConnector, Transport.UDP,
					destinationSocket);
			session.connectInit();

			udpSession.put(destinationSocket, session);
		}

		return session;
	}

	void deliverMessage(NetworkDeliverEvent message, Transport protocol) {
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
