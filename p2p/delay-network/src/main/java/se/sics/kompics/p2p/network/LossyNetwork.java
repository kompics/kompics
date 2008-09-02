package se.sics.kompics.p2p.network;

import java.math.BigInteger;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.EventAttributeFilter;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.events.Message;
import se.sics.kompics.p2p.network.events.LossyNetworMessage;
import se.sics.kompics.p2p.network.events.LossyNetworkAlarm;
import se.sics.kompics.p2p.network.topology.KingMatrix;
import se.sics.kompics.timer.events.ScheduleTimeout;

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
		timerSetChannel = timerMembrane.getChannelIn(ScheduleTimeout.class);

		// use a private channel for TimerSignal events
		timerSignalChannel = component.createChannel(LossyNetworkAlarm.class);

		// use shared network component
		ComponentMembrane networkMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Network");
		netSendChannel = networkMembrane.getChannelIn(Message.class);
		netDeliverChannel = networkMembrane.getChannelOut(Message.class);

		component.subscribe(timerSignalChannel, "handleLossyNetworkAlarm");
		component.subscribe(this.sendChannel, "handleMessage");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		ComponentMembrane membrane = new ComponentMembrane(component);
		membrane.inChannel(Message.class, sendChannel);
		membrane.outChannel(Message.class, deliverChannel);
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
		component.subscribe(netDeliverChannel, "handleLossyNetworkMessage",
				destinationFilter);
		logger.debug("Subscribed for LossyNetNetDeliver with destination {}",
				localAddress);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { ScheduleTimeout.class, Message.class })
	public void handleMessage(Message event) {
		logger.debug("Handling send {} to {}.", event, event.getDestination());

		event.setSource(localAddress);
		Address destination = event.getDestination();

		if (destination.equals(localAddress)) {
			// deliver locally
			// LossyNetworkDeliverEvent deliverEvent = event
			// .getLossyNetworkDeliverEvent();
			// deliverEvent.setSource(localAddress);
			// deliverEvent.setDestination(destination);
			component.triggerEvent(event, deliverChannel);
			return;
		}

		if (random.nextDouble() < lossRate) {
			// drop the message according to the loss rate
			return;
		}

		// make a LossyNetNetworkDeliverEvent to be delivered at the destination
		LossyNetworMessage lnMessage = new LossyNetworMessage(event,
				localAddress, destination);

		long latency = king[localKingId][destination.getId().mod(
				BigInteger.valueOf(KingMatrix.SIZE)).intValue()];
		if (latency > 0) {
			// delay the sending according to the latency
			LossyNetworkAlarm tse = new LossyNetworkAlarm(lnMessage);
			ScheduleTimeout ste = new ScheduleTimeout(0, tse,
					timerSignalChannel, component, latency);
			component.triggerEvent(ste, timerSetChannel, Priority.HIGH);
		} else {
			// send immediately
			component.triggerEvent(lnMessage, netSendChannel);
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(Message.class)
	public void handleLossyNetworkAlarm(LossyNetworkAlarm event) {
		component.triggerEvent(event.getMessage(), netSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(Message.class)
	public void handleLossyNetworkMessage(LossyNetworMessage event) {
		logger.debug("Handling delivery {} from {}.", event.getMessage(), event
				.getSource());

		Message lnDeliverEvent = event.getMessage();
		// lnDeliverEvent.setSource(event.getSource());
		// lnDeliverEvent.setDestination(event.getDestination());

		// trigger the encapsulated LossyNetworkDeliverEvent
		component.triggerEvent(lnDeliverEvent, deliverChannel);
	}
}
