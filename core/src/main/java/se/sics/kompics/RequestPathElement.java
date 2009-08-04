package se.sics.kompics;

public final class RequestPathElement {

	private final ChannelCore<?> channel;

	private final ComponentCore component;

	private final boolean isChannel;

	public RequestPathElement(ChannelCore<?> channel) {
		super();
		this.channel = channel;
		this.component = null;
		this.isChannel = true;
	}

	public RequestPathElement(ComponentCore component) {
		super();
		this.channel = null;
		this.component = component;
		this.isChannel = false;
	}

	public ChannelCore<?> getChannel() {
		return channel;
	}

	public ComponentCore getComponent() {
		return component;
	}

	public boolean isChannel() {
		return isChannel;
	}

	@Override
	public String toString() {
		if (isChannel) {
			return "Channel: " + channel;
		}
		return "Component: " + component.component;
	}
}
