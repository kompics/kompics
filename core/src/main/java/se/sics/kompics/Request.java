package se.sics.kompics;

import java.util.ArrayDeque;

public abstract class Request extends Event {

	ArrayDeque<Channel<?>> channelStack = new ArrayDeque<Channel<?>>();

	@Override
	void forwardedBy(Channel<?> channel) {
		channelStack.push(channel);
	}

	@Override
	Channel<?> getTopChannel() {
		return null;
	}
}
