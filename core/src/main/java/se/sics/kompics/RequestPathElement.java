package se.sics.kompics;

import java.lang.ref.WeakReference;

public final class RequestPathElement {

	private final WeakReference<ChannelCore<?>> channel;

	private final WeakReference<ComponentCore> component;

	private final boolean isChannel;

	public RequestPathElement(ChannelCore<?> channel) {
		super();
		this.channel = new WeakReference<ChannelCore<?>>(channel);
		this.component = null;
		this.isChannel = true;
	}

	public RequestPathElement(ComponentCore component) {
		super();
		this.channel = null;
		this.component = new WeakReference<ComponentCore>(component);
		this.isChannel = false;
	}

	public ChannelCore<?> getChannel() {
		return channel.get();
	}

	public ComponentCore getComponent() {
		return component.get();
	}

	public boolean isChannel() {
		return isChannel;
	}

	@Override
	public String toString() {
		if (isChannel) {
			return "Channel: " + channel.get();
		}
		ComponentCore c = component.get();
		return "Component: " + (c == null ? null : c.component);
	}
}
