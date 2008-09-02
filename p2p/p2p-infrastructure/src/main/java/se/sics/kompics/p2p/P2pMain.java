package se.sics.kompics.p2p;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Kompics;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.network.events.NetworkConnectionRefused;
import se.sics.kompics.network.events.NetworkException;
import se.sics.kompics.network.events.NetworkSessionClosed;
import se.sics.kompics.network.events.NetworkSessionOpened;
import se.sics.kompics.p2p.application.events.StartApplication;
import se.sics.kompics.p2p.peer.events.FailPeer;
import se.sics.kompics.p2p.peer.events.JoinPeer;
import se.sics.kompics.p2p.peer.events.LeavePeer;
import se.sics.kompics.timer.events.CancelPeriodicTimeout;
import se.sics.kompics.timer.events.CancelTimeout;
import se.sics.kompics.timer.events.SchedulePeriodicTimeout;
import se.sics.kompics.timer.events.ScheduleTimeout;
import se.sics.kompics.timer.events.Timeout;
import se.sics.kompics.web.events.WebRequest;
import se.sics.kompics.web.events.WebResponse;

/**
 * The <code>P2pMain</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class P2pMain {

	private static final Logger logger = LoggerFactory.getLogger(P2pMain.class);

	/**
	 * @param args
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) throws UnknownHostException {

		System.out.println(System.getProperty("java.class.path"));

		PropertyConfigurator.configureAndWatch(System
				.getProperty("log4j.properties"), 1000);

		if (args.length != 3) {
			logger.error("usage: ProtocolsMain <networkAddress>"
					+ " <webAddress> <command>");
			return;
		}
		String networkAddress = args[0];
		String webAddress = args[1];
		String command = args[2];

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

		String netAddr[] = networkAddress.split(":");
		InetSocketAddress socketAddress = new InetSocketAddress(netAddr[0],
				Integer.parseInt(netAddr[1]));

		networkComponent.initialize(socketAddress);
		networkComponent.share("se.sics.kompics.Network");

		// create channels for the web component
		Channel webRequestChannel = boot.createChannel(WebRequest.class);
		Channel webResponseChannel = boot.createChannel(WebResponse.class);

		// create and share the web component
		Component webComponent = boot.createComponent(
				"se.sics.kompics.web.WebServer", faultChannel,
				webRequestChannel, webResponseChannel);

		String webAddr[] = webAddress.split(":");

		webComponent.initialize(webAddr[0], Integer.parseInt(webAddr[1]), 5000);
		webComponent.share("se.sics.kompics.Web");

		// create channel for the PeerCluster component
		Channel peerClusterChannel = boot.createChannel(JoinPeer.class,
				LeavePeer.class, FailPeer.class);

		// create the PeerCluster component
		Component peerCluster = boot.createComponent(
				"se.sics.kompics.p2p.peer.PeerCluster", faultChannel,
				peerClusterChannel);

		peerCluster.initialize(socketAddress);

		// create channel for the Application component
		Channel appStartChannel = boot.createChannel(StartApplication.class);

		// create the Application component
		Component application = boot.createComponent(
				"se.sics.kompics.p2p.application.Application", faultChannel,
				appStartChannel, peerClusterChannel);
		application.initialize();

		boot.triggerEvent(new StartApplication(command), appStartChannel);
	}
}
