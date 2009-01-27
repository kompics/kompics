package se.sics.kompics;

public abstract class Event {

	void forwardedBy(Channel<?> channel) {
	}

	Channel<?> getTopChannel() {
		return null;
	}
}
