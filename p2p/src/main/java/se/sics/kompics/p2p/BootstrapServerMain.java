package se.sics.kompics.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Kompics;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.NetworkDeliverEvent;
import se.sics.kompics.network.events.NetworkSendEvent;
import se.sics.kompics.p2p.bootstrap.BootstrapServer;
import se.sics.kompics.p2p.bootstrap.events.StartBootstrapServer;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.p2p.network.topology.NeighbourLinks;
import se.sics.kompics.timer.events.CancelPeriodicTimerEvent;
import se.sics.kompics.timer.events.CancelTimerEvent;
import se.sics.kompics.timer.events.SetPeriodicTimerEvent;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>BootstrapServerMain</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public final class BootstrapServerMain {

	private static final Logger logger = LoggerFactory
			.getLogger(BootstrapServerMain.class);

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			InterruptedException, IOException {

		PropertyConfigurator.configureAndWatch(System
				.getProperty("log4j.properties"), 1000);

		Properties properties = new Properties();
		InputStream inputStream = BootstrapServer.class
				.getResourceAsStream("bootstrap.properties");
		properties.load(inputStream);

		InetAddress ip = InetAddress.getByName(properties.getProperty(
				"server.ip", ""));
		int port = Integer.parseInt(properties.getProperty("server.port",
				"8181"));
		long evictAfterSeconds = Long.parseLong(properties.getProperty(
				"evict.after", "600"));

		NeighbourLinks neighbourLinks = new NeighbourLinks(0);
		neighbourLinks.setLocalAddress(new Address(ip, port, BigInteger.ZERO));

		Kompics kompics = new Kompics(2, 0);
		Kompics.setGlobalKompics(kompics);

		Component boot = kompics.getBootstrapComponent();
		Channel faultChannel = boot.getFaultChannel();

		// create channels for the timer component
		Channel timerSetChannel = boot.createChannel(SetTimerEvent.class,
				SetPeriodicTimerEvent.class, CancelTimerEvent.class,
				CancelPeriodicTimerEvent.class);
		Channel timerSignalChannel = boot.createChannel(TimerSignalEvent.class);

		// create and share the timer component
		Component timerComponent = boot.createComponent(
				"se.sics.kompics.timer.TimerComponent", faultChannel,
				timerSetChannel, timerSignalChannel);
		timerComponent.share("se.sics.kompics.Timer");

		// create channels for the network component
		Channel networkSendChannel = boot.createChannel(NetworkSendEvent.class);
		Channel networkDeliverChannel = boot
				.createChannel(NetworkDeliverEvent.class);

		// create and share the network component
		Component networkComponent = boot.createComponent(
				"se.sics.kompics.network.NetworkComponent", faultChannel,
				networkSendChannel, networkDeliverChannel);
		Address localAddress = neighbourLinks.getLocalAddress();
		SocketAddress socketAddress = new InetSocketAddress(localAddress
				.getIp(), localAddress.getPort());
		networkComponent.initialize(socketAddress);
		networkComponent.share("se.sics.kompics.Network");

		// create channels for the PerfectNetwork component
		Channel pnSendChannel = boot
				.createChannel(PerfectNetworkSendEvent.class);
		Channel pnDeliverChannel = boot
				.createChannel(PerfectNetworkDeliverEvent.class);

		// create and share the PerfectNetwork component
		Component pnComponent = boot.createComponent(
				"se.sics.kompics.p2p.network.PerfectNetwork", faultChannel,
				pnSendChannel, pnDeliverChannel);
		pnComponent.initialize(neighbourLinks);
		pnComponent.share("se.sics.kompics.p2p.network.PerfectNetwork");

		// create channel for the BootstrapServer component
		Channel bsStartChannel = boot.createChannel(StartBootstrapServer.class);

		// create the BootstrapServer component
		Component bsComponent = boot.createComponent(
				"se.sics.kompics.p2p.bootstrap.BootstrapServer", faultChannel,
				bsStartChannel);
		bsComponent.initialize(evictAfterSeconds);

		bsComponent.triggerEvent(new StartBootstrapServer(), bsStartChannel);

		logger.info("BootstrapServer started");
	}
}
