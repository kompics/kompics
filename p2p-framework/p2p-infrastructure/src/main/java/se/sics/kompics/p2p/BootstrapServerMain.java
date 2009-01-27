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
import se.sics.kompics.network.events.Message;
import se.sics.kompics.network.events.NetworkConnectionRefused;
import se.sics.kompics.network.events.NetworkException;
import se.sics.kompics.network.events.NetworkSessionClosed;
import se.sics.kompics.network.events.NetworkSessionOpened;
import se.sics.kompics.p2p.bootstrap.BootstrapServer;
import se.sics.kompics.p2p.bootstrap.events.StartBootstrapServer;
import se.sics.kompics.timer.events.CancelPeriodicTimeout;
import se.sics.kompics.timer.events.CancelTimeout;
import se.sics.kompics.timer.events.SchedulePeriodicTimeout;
import se.sics.kompics.timer.events.ScheduleTimeout;
import se.sics.kompics.timer.events.Timeout;
import se.sics.kompics.web.events.WebRequest;
import se.sics.kompics.web.events.WebResponse;

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
				"bootstrap.server.ip", ""));
		int port = Integer.parseInt(properties.getProperty(
				"bootstrap.server.port", "20002"));
		long evictAfterSeconds = Long.parseLong(properties.getProperty(
				"cache.evict.after", "600"));
		String webIp = properties.getProperty("bootstrap.web.ip", "");
		int webPort = Integer.parseInt(properties.getProperty(
				"bootstrap.web.port", "40002"));

		SocketAddress localSocketAddress = new InetSocketAddress(ip, port);
		Address localAddress = new Address(ip, port, BigInteger.ZERO);

		Kompics kompics = new Kompics(2, 0);
		Kompics.setGlobalKompics(kompics);

		Component boot = kompics.getBootstrapComponent();
		Channel faultChannel = boot.getFaultChannel();

		// create channels for the timer component
		Channel timerSetChannel = boot.createChannel(ScheduleTimeout.class,
				SchedulePeriodicTimeout.class, CancelTimeout.class,
				CancelPeriodicTimeout.class);
		Channel timerSignalChannel = boot.createChannel(Timeout.class);

		// create and share the timer component
		Component timerComponent = boot.createComponent(
				"se.sics.kompics.timer.Timer", faultChannel, timerSetChannel,
				timerSignalChannel);
		timerComponent.share("se.sics.kompics.Timer");

		// create channels for the network component
		Channel networkSendChannel = boot.createChannel(Message.class);
		Channel networkDeliverChannel = boot.createChannel(Message.class,
				NetworkException.class, NetworkSessionClosed.class,
				NetworkSessionOpened.class, NetworkConnectionRefused.class);

		// create and share the network component
		Component networkComponent = boot.createComponent(
				"se.sics.kompics.network.Network", faultChannel,
				networkSendChannel, networkDeliverChannel);
		networkComponent.initialize(localSocketAddress);
		networkComponent.share("se.sics.kompics.Network");

		// create channels for the PerfectNetwork component
		Channel pnSendChannel = boot.createChannel(Message.class);
		Channel pnDeliverChannel = boot.createChannel(Message.class);

		// create and share the PerfectNetwork component
		Component pnComponent = boot.createComponent(
				"se.sics.kompics.p2p.network.PerfectNetwork", faultChannel,
				pnSendChannel, pnDeliverChannel);
		pnComponent.initialize(localAddress);
		pnComponent.share("se.sics.kompics.p2p.network.PerfectNetwork");

		// create channels for the web component
		Channel webRequestChannel = boot.createChannel(WebRequest.class);
		Channel webResponseChannel = boot.createChannel(WebResponse.class);

		// create and share the web component
		Component webComponent = boot.createComponent(
				"se.sics.kompics.web.WebServer", faultChannel,
				webRequestChannel, webResponseChannel);

		webComponent.initialize(webIp, webPort, 5000);
		webComponent.share("se.sics.kompics.Web");

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
