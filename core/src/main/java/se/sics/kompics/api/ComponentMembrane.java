package se.sics.kompics.api;

import java.util.HashMap;

public class ComponentMembrane {

	private final Component component;

	private final HashMap<Class<? extends Event>, Channel> inboundChannels;

	private final HashMap<Class<? extends Event>, Channel> outboundChannels;

	private boolean sealed;

	public ComponentMembrane(Component component) {
		this.component = component;
		this.sealed = false;
		this.inboundChannels = new HashMap<Class<? extends Event>, Channel>();
		this.outboundChannels = new HashMap<Class<? extends Event>, Channel>();
	}

	public void inChannel(Class<? extends Event> eventType, Channel channel) {
		if (!sealed) {
			inboundChannels.put(eventType, channel);
		} else {
			throw new RuntimeException("ComponentMembrane already sealed.");
		}
	}

	public void outChannel(Class<? extends Event> eventType, Channel channel) {
		if (!sealed) {
			outboundChannels.put(eventType, channel);
		} else {
			throw new RuntimeException("ComponentMembrane already sealed.");
		}
	}

	public void seal() {
		sealed = true;
	}

	public Channel getChannelIn(Class<? extends Event> eventType) {
		return inboundChannels.get(eventType);
	}

	public Channel getChannelOut(Class<? extends Event> eventType) {
		return outboundChannels.get(eventType);
	}

	public Component getComponent() {
		return component;
	}
}
