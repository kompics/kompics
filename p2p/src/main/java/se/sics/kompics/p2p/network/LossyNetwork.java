package se.sics.kompics.p2p.network;

import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
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
import se.sics.kompics.p2p.network.topology.LinkDescriptor;
import se.sics.kompics.p2p.network.topology.NeighbourLinks;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>LossyNetwork</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public final class LossyNetwork {

	private static final Logger logger = LoggerFactory
			.getLogger(LossyNetwork.class);

	private Component component;

	// LossyNetwork channels
	private Channel sendChannel, deliverChannel;

	// Timer channels
	private Channel timerSetChannel, timerSignalChannel;

	// Network channels
	private Channel netSendChannel, netDeliverChannel;

	private NeighbourLinks neighbourLinks;

	private Address localAddress;

	private Random random;

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
		timerSignalChannel = timerMembrane.getChannel(TimerSignalEvent.class);

		// use shared network component
		ComponentMembrane networkMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Network");
		netSendChannel = networkMembrane.getChannel(NetworkSendEvent.class);
		netDeliverChannel = networkMembrane
				.getChannel(NetworkDeliverEvent.class);

		component.subscribe(timerSignalChannel, "handleFlp2pTimerSignalEvent");
		component
				.subscribe(netDeliverChannel, "handleFlp2pNetworkDeliverEvent");
		component.subscribe(this.sendChannel, "handleFlp2pSendEvent");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(LossyNetworkSendEvent.class, sendChannel);
		map.put(LossyNetworkDeliverEvent.class, deliverChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod("flp2p.properties")
	public void init(Properties properties, NeighbourLinks neighbourLinks) {
		this.neighbourLinks = neighbourLinks;
		this.localAddress = neighbourLinks.getLocalAddress();

		String lossSeed = properties.getProperty("loss.random.seed", "0");
		random = new Random(Long.parseLong(lossSeed));
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { SetTimerEvent.class, NetworkSendEvent.class })
	public void handleFlp2pSendEvent(LossyNetworkSendEvent event) {
		logger.debug("Handling send {} to {}.", event.getFlp2pDeliverEvent(),
				event.getDestination());

		Address destination = event.getDestination();

		if (destination.equals(localAddress)) {
			// deliver locally
			LossyNetworkDeliverEvent deliverEvent = event
					.getFlp2pDeliverEvent();
			deliverEvent.setSource(localAddress);
			deliverEvent.setDestination(destination);
			component.triggerEvent(deliverEvent, deliverChannel);
			return;
		}

		LinkDescriptor link = neighbourLinks.getLink(destination.getId());
		long latency = link.getLatency();
		double lossRate = link.getLossRate();

		if (random.nextDouble() < lossRate) {
			// drop the message according to the loss rate
			return;
		}

		// make a Flp2pNetworkDeliverEvent to be delivered at the destination
		LossyNetNetworkDeliverEvent flp2pNetworkDeliverEvent = new LossyNetNetworkDeliverEvent(
				event.getFlp2pDeliverEvent(), localAddress, destination);

		// create a NetworkSendEvent containing a Flp2pNetworkDeliverEvent
		NetworkSendEvent nse = new NetworkSendEvent(flp2pNetworkDeliverEvent,
				localAddress, destination, Transport.UDP);

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
	public void handleFlp2pTimerSignalEvent(LossyNetworkTimerSignalEvent event) {
		component.triggerEvent(event.getNetworkSendEvent(), netSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(LossyNetworkDeliverEvent.class)
	public void handleFlp2pNetworkDeliverEvent(LossyNetNetworkDeliverEvent event) {
		logger.debug("Handling delivery {} from {}.", event
				.getFlp2pDeliverEvent(), event.getSource());

		LossyNetworkDeliverEvent flp2pDeliverEvent = event
				.getFlp2pDeliverEvent();
		flp2pDeliverEvent.setSource(event.getSource());
		flp2pDeliverEvent.setDestination(event.getDestination());

		// trigger the encapsulated Flp2pDeliverEvent
		component.triggerEvent(flp2pDeliverEvent, deliverChannel);
	}
}
