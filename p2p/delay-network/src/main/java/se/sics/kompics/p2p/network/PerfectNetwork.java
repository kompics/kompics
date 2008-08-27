package se.sics.kompics.p2p.network;

import java.math.BigInteger;

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
import se.sics.kompics.p2p.network.events.PerfectNetNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkTimerSignalEvent;
import se.sics.kompics.p2p.network.topology.KingMatrix;
import se.sics.kompics.timer.events.SetTimerEvent;

/**
 * The <code>PeerMonitor</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public final class PerfectNetwork {

	private Logger logger;

	private Component component;

	// PerfectNetwork channels
	private Channel sendChannel, deliverChannel;

	// timer channels
	private Channel timerSetChannel, timerSignalChannel;

	// network channels
	private Channel netSendChannel, netDeliverChannel;

	private Address localAddress;

	private int[][] king = KingMatrix.KING;

	private int localKingId;

	public PerfectNetwork(Component component) {
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		this.sendChannel = sendChannel;
		this.deliverChannel = deliverChannel;

		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		timerSetChannel = timerMembrane.getChannelIn(SetTimerEvent.class);

		// use a private channel for TimerSignal events
		timerSignalChannel = component
				.createChannel(PerfectNetworkTimerSignalEvent.class);

		// use shared network component
		ComponentMembrane networkMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Network");
		netSendChannel = networkMembrane.getChannelIn(Message.class);
		netDeliverChannel = networkMembrane.getChannelOut(Message.class);

		component.subscribe(timerSignalChannel,
				"handlePerfectNetworkTimerSignalEvent");
		component.subscribe(this.sendChannel, "handlePerfectNetworkSendEvent");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		ComponentMembrane membrane = new ComponentMembrane(component);
		membrane.inChannel(PerfectNetworkSendEvent.class, sendChannel);
		membrane.outChannel(PerfectNetworkDeliverEvent.class, deliverChannel);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod
	public void init(Address localAddress) {
		this.localAddress = localAddress;

		logger = LoggerFactory.getLogger(PerfectNetwork.class.getName() + "@"
				+ localAddress.getId());

		BigInteger kingId = localAddress.getId().mod(
				BigInteger.valueOf(KingMatrix.SIZE));

		this.localKingId = kingId.intValue();

		EventAttributeFilter destinationFilter = new EventAttributeFilter(
				"destination", localAddress);

		component.subscribe(netDeliverChannel,
				"handlePerfectNetNetworkDeliverEvent", destinationFilter);
		logger.debug("Subscribed for PerfectNetNetDeliver with destination {}",
				localAddress);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { SetTimerEvent.class, Message.class })
	public void handlePerfectNetworkSendEvent(PerfectNetworkSendEvent event) {
		logger.debug("Handling send1 {} to {}.", event
				.getPerfectNetworkDeliverEvent(), event.getDestination());

		Address destination = event.getDestination();

		if (destination.equals(localAddress)) {
			// deliver locally
			PerfectNetworkDeliverEvent deliverEvent = event
					.getPerfectNetworkDeliverEvent();
			deliverEvent.setSource(localAddress);
			deliverEvent.setDestination(destination);
			component.triggerEvent(deliverEvent, deliverChannel);
			return;
		}

		// make a PerfectNetNetworkDeliverEvent to be delivered at the
		// destination
		PerfectNetNetworkDeliverEvent pnMessage = new PerfectNetNetworkDeliverEvent(
				event.getPerfectNetworkDeliverEvent(), localAddress,
				destination);

		long latency = king[localKingId][destination.getId().mod(
				BigInteger.valueOf(KingMatrix.SIZE)).intValue()];
		if (latency > 0) {
			// delay the sending according to the latency
			PerfectNetworkTimerSignalEvent tse = new PerfectNetworkTimerSignalEvent(
					pnMessage);
			SetTimerEvent ste = new SetTimerEvent(0, tse, timerSignalChannel,
					component, latency);
			component.triggerEvent(ste, timerSetChannel, Priority.HIGH);
		} else {
			// send immediately
			component.triggerEvent(pnMessage, netSendChannel);
		}
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(Message.class)
	public void handlePerfectNetworkTimerSignalEvent(
			PerfectNetworkTimerSignalEvent event) {
		logger.debug("Handling send2 {} to {}.",
				((PerfectNetNetworkDeliverEvent) event.getNetworkSendEvent())
						.getPerfectNetworkDeliverEvent(), event
						.getNetworkSendEvent().getDestination());

		component.triggerEvent(event.getNetworkSendEvent(), netSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkDeliverEvent.class)
	public void handlePerfectNetNetworkDeliverEvent(
			PerfectNetNetworkDeliverEvent event) {
		logger.debug("Handling delivery {} from {}.", event
				.getPerfectNetworkDeliverEvent(), event.getSource());

		PerfectNetworkDeliverEvent pnDeliverEvent = event
				.getPerfectNetworkDeliverEvent();
		pnDeliverEvent.setSource(event.getSource());
		pnDeliverEvent.setDestination(event.getDestination());

		// trigger the encapsulated PerfectNetworkDeliverEvent
		component.triggerEvent(pnDeliverEvent, deliverChannel);
	}
}
