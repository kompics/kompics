package se.sics.kompics.core;

import java.util.EnumSet;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.capability.ChannelCapabilityFlags;

public class ChannelReference implements Channel {

	/**
	 * The channel referenced by this channel reference.
	 */
	private ChannelCore channelCore;

	/**
	 * Set of capabilities offered on the referenced channel.
	 */
	private EnumSet<ChannelCapabilityFlags> channelCapabilities;

	/**
	 * The identifier of the component that is capable to use this reference. If
	 * <code>null</code> any component is capable to use this reference.
	 */
	private ComponentIdentifier capable;

	/**
	 * Constructs a channel reference. This constructor is only visible in the
	 * se.sics.kompics.core package.
	 * 
	 * @param channelCore
	 * @param channelCapabilities
	 */
	ChannelReference(ChannelCore channelCore,
			EnumSet<ChannelCapabilityFlags> channelCapabilities) {
		super();

		if (channelCore == null) {
			throw new RuntimeException(
					"Cannot create a channel reference to a null channel.");
		}

		this.channelCore = channelCore;

		if (channelCapabilities == null) {
			this.channelCapabilities = EnumSet
					.noneOf(ChannelCapabilityFlags.class);
		} else {
			this.channelCapabilities = channelCapabilities;
		}
	}

	/**
	 * Constructs a channel reference. This constructor is visible only in the
	 * se.sics.kompics.core package.
	 * 
	 * @param channelCore
	 * @param channelCapabilities
	 * @param capable
	 */
	ChannelReference(ChannelCore channelCore,
			EnumSet<ChannelCapabilityFlags> channelCapabilities,
			ComponentIdentifier capable) {
		this(channelCore, channelCapabilities);
		this.capable = capable;
	}

	void revokeCapability() {
		;
	}

	ChannelCore getChannelCore() {
		return channelCore;
	}

	public void addEventType(Class<? extends Event> eventType) {
		// TODO capability add event type
		channelCore.addEventType(eventType);
	}

	public boolean hasEventType(Class<? extends Event> eventType) {
		return channelCore.hasEventType(eventType);
	}

	public void removeEventType(Class<? extends Event> eventType) {
		// TODO capability remove event type
		channelCore.removeEventType(eventType);
	}

	ChannelCore addSubscription(Subscription subscription) {
		// TODO capability add subscription
		channelCore.addSubscription(subscription);
		return channelCore;
	}

	void addBinding(Binding binding) {
		// TODO capability add binding
		channelCore.addBinding(binding);
	}

	void publishEventCore(EventCore eventCore) {
		// TODO capability publish
		channelCore.publishEventCore(eventCore);
	}
}
