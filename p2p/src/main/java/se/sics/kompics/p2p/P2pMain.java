package se.sics.kompics.p2p;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.log4j.PropertyConfigurator;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Kompics;
import se.sics.kompics.network.events.NetworkDeliverEvent;
import se.sics.kompics.network.events.NetworkSendEvent;
import se.sics.kompics.timer.events.CancelPeriodicTimerEvent;
import se.sics.kompics.timer.events.CancelTimerEvent;
import se.sics.kompics.timer.events.SetPeriodicTimerEvent;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>P2pMain</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
public class P2pMain {

	// private static final Logger logger =
	// LoggerFactory.getLogger(P2pMain.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configureAndWatch(System
				.getProperty("log4j.properties"), 1000);

		Kompics kompics = new Kompics(1, 0);
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

		// create the PeerCluster component
		Component peerCluster = boot.createComponent(
				"se.sics.kompics.p2p.PeerCluster", faultChannel);
		peerCluster.initialize();

		// peerCluster.initialize(neighbourLinks, new Integer(1));
		// peerCluster.triggerEvent(new ApplicationStartEvent(command),
		// appStartChannel);

		System.out.println("DONE");
	}

}
