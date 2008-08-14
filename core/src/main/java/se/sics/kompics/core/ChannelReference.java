package se.sics.kompics.core;

import java.util.EnumSet;
import java.util.Set;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.capability.CapabilityNotSupportedException;
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
	private ComponentUUID capable;

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
			ComponentUUID capable) {
		this(channelCore, channelCapabilities);
		this.capable = capable;
	}

	void revoke() {
		channelCore = null;
	}

	ChannelCore getChannelCore() {
		return channelCore;
	}

	public boolean hasEventType(Class<? extends Event> eventType) {
		return channelCore.hasEventType(eventType);
	}

	public Set<Class<? extends Event>> getEventTypes() {
		return channelCore.getEventTypes();
	}

	ChannelCore addSubscription(Subscription subscription) {
		if (channelCapabilities.contains(ChannelCapabilityFlags.SUBSCRIBE)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			channelCore.addSubscription(subscription);
			return channelCore;
		}
		throw new CapabilityNotSupportedException(
				ChannelCapabilityFlags.SUBSCRIBE);
	}

	ChannelCore removeSubscription(Subscription subscription) {
		if (channelCapabilities.contains(ChannelCapabilityFlags.UNSUBSCRIBE)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			channelCore.removeSubscription(subscription);
			return channelCore;
		}
		throw new CapabilityNotSupportedException(
				ChannelCapabilityFlags.UNSUBSCRIBE);
	}

	void publishEventCore(EventCore eventCore) {
		if (channelCapabilities.contains(ChannelCapabilityFlags.PUBLISH)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			channelCore.publishEventCore(eventCore);
			return;
		}
		throw new CapabilityNotSupportedException(
				ChannelCapabilityFlags.PUBLISH);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((channelCore == null) ? 0 : channelCore.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChannelReference other = (ChannelReference) obj;
		if (channelCore == null) {
			if (other.channelCore != null)
				return false;
		} else if (channelCore != other.channelCore)
			return false;
		return true;
	}

}
