package se.sics.kompics.network;

import java.util.Properties;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.events.NetworkDeliverEvent;
import se.sics.kompics.network.events.NetworkSendEvent;

@ComponentType
public class NetworkComponent {

	private Component component;

	public NetworkComponent(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		component.subscribe(sendChannel, "handleNetworkSendEvent");
		component.bind(NetworkDeliverEvent.class, deliverChannel);
	}

	@ComponentInitializeMethod
	public void init(Properties properties) {
		;
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(NetworkDeliverEvent.class)
	public void handleNetworkSendEvent(NetworkSendEvent event) {
		System.out.println("NET@SEND "
				+ event.getNetworkDeliverEvent().getClass());

		NetworkDeliverEvent deliverEvent = event.getNetworkDeliverEvent();
		if (event.getDestination().equals(event.getSource())) {
			component.triggerEvent(deliverEvent);
		}

		// TODO check if connection exists
		// create connection if necessary
		// TODO send message
	}
}
