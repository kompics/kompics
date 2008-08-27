package se.sics.kompics.p2p.network;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.EventAttributeFilter;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.events.NetworkDeliverEvent;
import se.sics.kompics.network.events.NetworkSendEvent;
import se.sics.kompics.p2p.network.events.LossyNetNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.LossyNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.LossyNetworkSendEvent;
import se.sics.kompics.p2p.network.events.LossyNetworkTimerSignalEvent;
import se.sics.kompics.p2p.network.topology.KingMatrix;
import se.sics.kompics.timer.events.SetTimerEvent;

/**
 * The <code>LossyNetwork</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public final class LossyNetwork {

	private Logger logger;

	private Component component;

	// LossyNetwork channels
	private Channel sendChannel, deliverChannel;

	// Timer channels
	private Channel timerSetChannel, timerSignalChannel;

	// Network channels
	private Channel netSendChannel, netDeliverChannel;

	private Address localAddress;

	private int[][] king = KingMatrix.KING;

	private int localKingId;

	private Random random;

	private double lossRate;

	public LossyNetwork(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		this.sendChannel = sendChannel;
		this.deliverChannel = deliverChannel;

		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		timerSetChannel = timerMembrane.getChannel(SetTimerEvent.class);

		// use a private channel for TimerSignal events
		timerSignalChannel = component
				.createChannel(LossyNetworkTimerSignalEvent.class);

		// use shared network component
		ComponentMembrane networkMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Network");
		netSendChannel = networkMembrane.getChannel(NetworkSendEvent.class);
		netDeliverChannel = networkMembrane
				.getChannel(NetworkDeliverEvent.class);

		component.subscribe(timerSignalChannel,
				"handleLossyNetworkTimerSignalEvent");
		component.subscribe(this.sendChannel, "handleLossyNetworkSendEvent");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(LossyNetworkSendEvent.class, sendChannel);
		map.put(LossyNetworkDeliverEvent.class, deliverChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod("lossynetwork.properties")
	public void init(Properties properties, Address localAddress) {
		this.localAddress = localAddress;

		logger = LoggerFactory.getLogger(LossyNetwork.class.getName() + "@"
				+ localAddress.getId());

		BigInteger kingId = localAddress.getId().mod(
				BigInteger.valueOf(KingMatrix.SIZE));

		this.localKingId = kingId.intValue();

		String lossSeed = properties.getProperty("loss.random.seed", "0");
		random = new Random(Long.parseLong(lossSeed));

		lossRate = Double.parseDouble(properties.getProperty(
				"global.loss.rate", "0.0"));

		EventAttributeFilter destinationFilter = new EventAttributeFilter(
				"destination", localAddress);
		component.subscribe(netDeliverChannel,
				"handleLossyNetNetworkDeliverEvent", destinationFilter);
		logger.debug("Subscribed for LossyNetNetDeliver with destination {}",
				localAddress);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { SetTimerEvent.class, NetworkSendEvent.class })
	public void handleLossyNetworkSendEvent(LossyNetworkSendEvent event) {
		logger.debug("Handling send {} to {}.", event
				.getLossyNetworkDeliverEvent(), event.getDestination());

		Address destination = event.getDestination();

		if (destination.equals(localAddress)) {
			// deliver locally
			LossyNetworkDeliverEvent deliverEvent = event
					.getLossyNetworkDeliverEvent();
			deliverEvent.setSource(localAddress);
			deliverEvent.setDestination(destination);
			component.triggerEvent(deliverEvent, deliverChannel);
			return;
		}

		if (random.nextDouble() < lossRate) {
			// drop the message according to the loss rate
			return;
		}

		// make a LossyNetNetworkDeliverEvent to be delivered at the destination
		LossyNetNetworkDeliverEvent lnNetworkDeliverEvent = new LossyNetNetworkDeliverEvent(
				event.getLossyNetworkDeliverEvent(), localAddress, destination);

		// create a NetworkSendEvent containing a LossyNetNetworkDeliverEvent
		NetworkSendEvent nse = new NetworkSendEvent(lnNetworkDeliverEvent,
				localAddress, destination, Transport.TCP);

		long latency = king[localKingId][destination.getId().mod(
				BigInteger.valueOf(KingMatrix.SIZE)).intValue()];
		if (latency > 0) {
			// delay the sending according to the latency
			LossyNetworkTimerSignalEvent tse = new LossyNetworkTimerSignalEvent(
					nse);
			SetTimerEvent ste = new SetTimerEvent(0, tse, timerSignalChannel,
					component, latency);
			component.triggerEvent(ste, timerSetChannel, Priority.HIGH);
		} else {
			// send immediately
			component.triggerEvent(nse, netSendChannel);
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(NetworkSendEvent.class)
	public void handleLossyNetworkTimerSignalEvent(
			LossyNetworkTimerSignalEvent event) {
		component.triggerEvent(event.getNetworkSendEvent(), netSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(LossyNetworkDeliverEvent.class)
	public void handleLossyNetNetworkDeliverEvent(
			LossyNetNetworkDeliverEvent event) {
		logger.debug("Handling delivery {} from {}.", event
				.getLossyNetworkDeliverEvent(), event.getSource());

		LossyNetworkDeliverEvent lnDeliverEvent = event
				.getLossyNetworkDeliverEvent();
		lnDeliverEvent.setSource(event.getSource());
		lnDeliverEvent.setDestination(event.getDestination());

		// trigger the encapsulated LossyNetworkDeliverEvent
		component.triggerEvent(lnDeliverEvent, deliverChannel);
	}
}
