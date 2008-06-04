package se.sics.kompics.p2p.network;

import java.util.HashMap;

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
import se.sics.kompics.p2p.network.events.PerfectNetNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkSendEvent;
import se.sics.kompics.p2p.network.events.PerfectNetworkTimerSignalEvent;
import se.sics.kompics.p2p.network.topology.LinkDescriptor;
import se.sics.kompics.p2p.network.topology.NeighbourLinks;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>PeerMonitor</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public final class PerfectNetwork {

	private static final Logger logger = LoggerFactory
			.getLogger(PerfectNetwork.class);

	private Component component;

	// PerfectNetwork channels
	private Channel sendChannel, deliverChannel;

	// timer channels
	private Channel timerSetChannel, timerSignalChannel;

	// network channels
	private Channel netSendChannel, netDeliverChannel;

	private NeighbourLinks neighbourLinks;

	private Address localAddress;

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
		timerSetChannel = timerMembrane.getChannel(SetTimerEvent.class);
		timerSignalChannel = timerMembrane.getChannel(TimerSignalEvent.class);

		// use shared network component
		ComponentMembrane networkMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Network");
		netSendChannel = networkMembrane.getChannel(NetworkSendEvent.class);
		netDeliverChannel = networkMembrane
				.getChannel(NetworkDeliverEvent.class);

		component.subscribe(timerSignalChannel, "handlePp2pTimerSignalEvent");
		component.subscribe(netDeliverChannel, "handlePp2pNetworkDeliverEvent");
		component.subscribe(this.sendChannel, "handlePp2pSendEvent");
	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(PerfectNetworkSendEvent.class, sendChannel);
		map.put(PerfectNetworkDeliverEvent.class, deliverChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod
	public void init(NeighbourLinks neighbourLinks) {
		this.neighbourLinks = neighbourLinks;
		this.localAddress = neighbourLinks.getLocalAddress();
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { SetTimerEvent.class, NetworkSendEvent.class })
	public void handlePp2pSendEvent(PerfectNetworkSendEvent event) {
		logger.debug("Handling send1 {} to {}.", event.getPp2pDeliverEvent(),
				event.getDestination());

		Address destination = event.getDestination();

		if (destination.equals(localAddress)) {
			// deliver locally
			PerfectNetworkDeliverEvent deliverEvent = event
					.getPp2pDeliverEvent();
			deliverEvent.setSource(localAddress);
			deliverEvent.setDestination(destination);
			component.triggerEvent(deliverEvent, deliverChannel);
			return;
		}

		// make a Pp2pNetworkDeliverEvent to be delivered at the destination
		PerfectNetNetworkDeliverEvent pp2pNetworkDeliverEvent = new PerfectNetNetworkDeliverEvent(
				event.getPp2pDeliverEvent(), localAddress, destination);

		// create a NetworkSendEvent containing a Pp2pNetworkDeliverEvent
		NetworkSendEvent nse = new NetworkSendEvent(pp2pNetworkDeliverEvent,
				localAddress, destination, Transport.TCP);

		LinkDescriptor link = neighbourLinks.getLink(destination.getId());
		long latency = link.getLatency();
		if (latency > 0) {
			// delay the sending according to the latency
			PerfectNetworkTimerSignalEvent tse = new PerfectNetworkTimerSignalEvent(
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
	public void handlePp2pTimerSignalEvent(PerfectNetworkTimerSignalEvent event) {
		logger.debug("Handling send2 {} to {}.",
				((PerfectNetNetworkDeliverEvent) event.getNetworkSendEvent()
						.getNetworkDeliverEvent()).getPp2pDeliverEvent(), event
						.getNetworkSendEvent().getDestination());

		component.triggerEvent(event.getNetworkSendEvent(), netSendChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(PerfectNetworkDeliverEvent.class)
	public void handlePp2pNetworkDeliverEvent(
			PerfectNetNetworkDeliverEvent event) {
		logger.debug("Handling delivery {} from {}.", event
				.getPp2pDeliverEvent(), event.getSource());

		PerfectNetworkDeliverEvent pp2pDeliverEvent = event
				.getPp2pDeliverEvent();
		pp2pDeliverEvent.setSource(event.getSource());
		pp2pDeliverEvent.setDestination(event.getDestination());

		// trigger the encapsulated Pp2pDeliverEvent
		component.triggerEvent(pp2pDeliverEvent, deliverChannel);
	}
}
