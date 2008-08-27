package se.sics.kompics.network;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Before;
import org.junit.Test;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Kompics;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.network.events.NetworkConnectionRefused;
import se.sics.kompics.network.events.NetworkException;
import se.sics.kompics.network.events.NetworkSessionClosed;
import se.sics.kompics.network.events.NetworkSessionOpened;

public class EchoTest {

	public static int echoed;

	int serverPort = 12345;

	int clientPort = 12346;

	Component echoClient, echoServer;
	SocketAddress serverSocketAddress, clientSocketAddress;
	Address serverAddress, clientAddress;

	@Before
	public void setUp() throws Exception {
		// BasicConfigurator.configure();

		InetAddress serverIp = InetAddress.getByName("127.0.0.1");
		InetAddress clientIp = InetAddress.getByName("127.0.0.1");

		serverSocketAddress = new InetSocketAddress(serverIp, serverPort);
		clientSocketAddress = new InetSocketAddress(clientIp, clientPort);

		serverAddress = new Address(serverIp, serverPort, BigInteger.ZERO);
		clientAddress = new Address(clientIp, clientPort, BigInteger.ONE);

		Kompics kompics = new Kompics(2, 0);
		Kompics.setGlobalKompics(kompics);

		Component boot = kompics.getBootstrapComponent();
		Channel faultChannel = boot.getFaultChannel();

		// create channels for the network component
		Channel serverNetworkSendChannel = boot.createChannel(Message.class);
		Channel serverNetworkDeliverChannel = boot.createChannel(Message.class,
				NetworkConnectionRefused.class, NetworkSessionClosed.class,
				NetworkSessionOpened.class, NetworkException.class);

		Channel clientNetworkSendChannel = boot.createChannel(Message.class);
		Channel clientNetworkDeliverChannel = boot.createChannel(Message.class);

		// create and share the network component
		Component serverNetworkComponent = boot.createComponent(
				"se.sics.kompics.network.Network", faultChannel,
				serverNetworkSendChannel, serverNetworkDeliverChannel);
		Component networkComponent1 = boot.createComponent(
				"se.sics.kompics.network.Network", faultChannel,
				clientNetworkSendChannel, clientNetworkDeliverChannel);

		serverNetworkComponent.initialize(serverSocketAddress);
		networkComponent1.initialize(clientSocketAddress);

		serverNetworkComponent.share("se.sics.kompics.Network0");
		networkComponent1.share("se.sics.kompics.Network1");

		echoServer = boot.createComponent("se.sics.kompics.network.EchoServer",
				faultChannel, serverNetworkSendChannel,
				serverNetworkDeliverChannel);
		// echoClient =
		// boot.createComponent("se.sics.kompics.network.EchoClient",
		// faultChannel, serverNetworkSendChannel,
		// serverNetworkDeliverChannel);
		echoClient = boot.createComponent("se.sics.kompics.network.EchoClient",
				faultChannel, clientNetworkSendChannel,
				clientNetworkDeliverChannel);
	}

	@Test
	public void testNetwork() throws InterruptedException {
		System.out.println("Hi");
		echoClient.initialize(10000, 0, clientAddress, serverAddress);

		Thread.sleep(6000);

		System.out.println("Echoed " + echoed);
	}

}
