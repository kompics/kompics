package se.sics.kompics.p2p;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Kompics;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.NetworkDeliverEvent;
import se.sics.kompics.network.events.NetworkSendEvent;
import se.sics.kompics.p2p.application.events.StartApplication;
import se.sics.kompics.p2p.network.topology.NeighbourLinks;
import se.sics.kompics.p2p.peer.events.FailPeer;
import se.sics.kompics.p2p.peer.events.JoinPeer;
import se.sics.kompics.p2p.peer.events.LeavePeer;
import se.sics.kompics.timer.events.CancelPeriodicTimerEvent;
import se.sics.kompics.timer.events.CancelTimerEvent;
import se.sics.kompics.timer.events.SetPeriodicTimerEvent;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;
import se.sics.kompics.web.events.WebRequestEvent;
import se.sics.kompics.web.events.WebResponseEvent;

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
		PropertyConfigurator.configureAndWatch(System
				.getProperty("log4j.properties"), 1000);

		if (args.length != 1) {
			logger.error("usage: ProtocolsMain <command>");
			return;
		}
		String command = args[0];

		Kompics kompics = new Kompics(3, 0);
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

		SocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 9090);

		networkComponent.initialize(socketAddress);
		networkComponent.share("se.sics.kompics.Network");

		// create channels for the web component
		Channel webRequestChannel = boot.createChannel(WebRequestEvent.class);
		Channel webResponseChannel = boot.createChannel(WebResponseEvent.class);

		// create and share the web component
		Component webComponent = boot.createComponent(
				"se.sics.kompics.web.WebComponent", faultChannel,
				webRequestChannel, webResponseChannel);
		webComponent.initialize("127.0.0.1", 8080, 5000);
		webComponent.share("se.sics.kompics.Web");

		// create channel for the PeerCluster component
		Channel peerClusterChannel = boot.createChannel(JoinPeer.class,
				LeavePeer.class, FailPeer.class);

		// create the PeerCluster component
		Component peerCluster = boot.createComponent(
				"se.sics.kompics.p2p.peer.PeerCluster", faultChannel,
				peerClusterChannel);

		NeighbourLinks neighbourLinks = new NeighbourLinks(0);
		neighbourLinks.setLocalAddress(new Address(InetAddress
				.getByName("127.0.0.1"), 11111, BigInteger.ZERO));
		peerCluster.initialize(neighbourLinks);

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
