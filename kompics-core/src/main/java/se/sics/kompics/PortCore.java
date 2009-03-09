/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// TODO: Auto-generated Javadoc
/**
 * The <code>PortCore</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class PortCore<P extends PortType> implements Positive<P>, Negative<P> {

	private boolean positive;

	private P portType;

	private PortCore<P> pair;

	private ComponentCore owner;

	private ReentrantReadWriteLock rwLock;

	private HashMap<Class<? extends Event>, ArrayList<Handler<?>>> subs;

	private ArrayList<ChannelCore<?>> allChannels;
	private ArrayList<ChannelCore<?>> unfilteredChannels;
	private ChannelFilterSet filteredChannels;

	private SpinlockQueue<Event> eventQueue;

	private Set<PortCore<P>> remotePorts;

	PortCore(boolean positive, P portType, ComponentCore owner) {
		this.positive = positive;
		this.portType = portType;
		this.rwLock = new ReentrantReadWriteLock();
		this.subs = new HashMap<Class<? extends Event>, ArrayList<Handler<?>>>();
		this.allChannels = new ArrayList<ChannelCore<?>>();
		this.unfilteredChannels = new ArrayList<ChannelCore<?>>();
		this.filteredChannels = new ChannelFilterSet();
		this.remotePorts = new HashSet<PortCore<P>>();
		this.owner = owner;
	}

	void setPair(PortCore<P> pair) {
		this.pair = pair;
	}

	void addChannel(ChannelCore<P> channel) {
		PortCore<P> remotePort = (positive ? channel.getNegativePort()
				: channel.getPositivePort());

		if (remotePorts.contains(remotePort)) {
			throw new RuntimeException((positive ? "Positive " : "Negative ")
					+ portType.getClass().getCanonicalName() + " of "
					+ pair.owner.component + " is already connected to "
					+ (!positive ? "positive " : "negative ")
					+ portType.getClass().getCanonicalName() + " of "
					+ remotePort.pair.owner.component);
		}

		rwLock.writeLock().lock();
		try {
			allChannels.add(channel);
			unfilteredChannels.add(channel);
			remotePorts.add(remotePort);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	void addChannelFilter(ChannelCore<P> channel, ChannelFilter<?, ?> filter) {
		rwLock.writeLock().lock();
		try {
			// channels.contains(channel)
			unfilteredChannels.remove(channel);
			filteredChannels.addChannelFilter(channel, filter);
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	// delivers the event to the connected channels (called holding read lock)
	private boolean deliverToChannels(Event event, int wid) {
		boolean delivered = false;
		for (ChannelCore<?> channel : unfilteredChannels) {
			if (positive) {
				channel.forwardToNegative(event, wid);
			} else {
				channel.forwardToPositive(event, wid);
			}
			delivered = true;
		}

		ArrayList<ChannelCore<?>> channels = filteredChannels.get(event);
		if (channels != null) {
			for (int i = 0; i < channels.size(); i++) {
				if (positive) {
					channels.get(i).forwardToNegative(event, wid);
				} else {
					channels.get(i).forwardToPositive(event, wid);
				}
				delivered = true;
			}
		}
		return delivered;
	}

	<E extends Event> void doSubscribe(Handler<E> handler) {
		Class<E> eventType = handler.getEventType();
		if (eventType == null) {
			eventType = reflectHandlerEventType(handler);
			handler.setEventType(eventType);
		}

		// check that the port type carries the event type in this direction
		if (!portType.hasEvent(positive, eventType)) {
			throw new RuntimeException("Cannot subscribe handler " + handler
					+ " to " + (positive ? "positive " : "negative ")
					+ portType.getClass().getCanonicalName() + " for "
					+ eventType.getCanonicalName() + " events.");
		}

		rwLock.writeLock().lock();
		try {
			ArrayList<Handler<?>> handlers = subs.get(eventType);
			if (handlers == null) {
				handlers = new ArrayList<Handler<?>>();
				subs.put(eventType, handlers);
			}
			handlers.add(handler);

			if (eventQueue == null) {
				eventQueue = new SpinlockQueue<Event>();
			}
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	<E extends Event> void doUnsubscribe(Handler<E> handler) {
		Class<E> eventType = handler.getEventType();
		if (eventType == null) {
			eventType = reflectHandlerEventType(handler);
			handler.setEventType(eventType);
		}

		rwLock.writeLock().lock();
		try {
			ArrayList<Handler<?>> handlers = subs.get(eventType);
			if (handlers == null) {
				throw new RuntimeException("Handler " + handler
						+ " is not subscribed to "
						+ (positive ? "positive " : "negative ")
						+ portType.getClass().getCanonicalName() + " for "
						+ eventType.getCanonicalName() + " events.");
			}

			if (!handlers.remove(handler)) {
				throw new RuntimeException("Handler " + handler
						+ " is not subscribed to "
						+ (positive ? "positive " : "negative ")
						+ portType.getClass().getCanonicalName() + " for "
						+ eventType.getCanonicalName() + " events.");
			}

			if (handlers.size() == 0) {
				subs.remove(eventQueue);
			}
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	ArrayList<Handler<?>> getSubscribedHandlers(Event event) {
		Class<? extends Event> eventType = event.getClass();
		ArrayList<Handler<?>> ret = new ArrayList<Handler<?>>();

		for (Class<? extends Event> eType : subs.keySet()) {
			if (eType.isAssignableFrom(eventType)) {
				ArrayList<Handler<?>> handlers = subs.get(eType);
				if (handlers != null) {
					ret.addAll(handlers);
				}
			}
		}
		return ret;
	}

	// TODO optimize trigger/subscribe

	void doTrigger(Event event, int wid) {
		pair.deliver(event, wid);
	}

	private void deliver(Event event, int wid) {
		Class<? extends Event> eventType = event.getClass();
		boolean delivered = false;

		rwLock.readLock().lock();
		try {
			delivered = deliverToSubscribers(event, wid, eventType);

			ChannelCore<?> caller = (ChannelCore<?>) event.getTopChannel();
			if (caller != null) {
				deliverToCallerChannel(event, wid, caller);
				delivered = true;
			} else {
				delivered |= deliverToChannels(event, wid);
			}
		} finally {
			rwLock.readLock().unlock();
		}

		if (!delivered) {
			if (portType.hasEvent(positive, eventType)) {
				if (event instanceof Fault) {
					// forward fault to parent component
					if (owner.parent != null) {
						((PortCore<?>) owner.component.control).doTrigger(
								event, wid);
					} else {
						owner.handleFault(((Fault) event).getFault());
					}
				} else {
					// warning, dropped event
					Kompics.logger.warn("Warning: {} event dropped by {} {} in"
							+ " component {}", new Object[] {
							eventType.getCanonicalName(),
							(positive ? "positive " : "negative "),
							portType.getClass().getCanonicalName(),
							owner.component });
				}
			} else {
				// error, event type doesn't flow on this port in this direction
				throw new RuntimeException(eventType.getCanonicalName()
						+ " events cannot be triggered on "
						+ (!positive ? "positive " : "negative ")
						+ portType.getClass().getCanonicalName());
			}
		}
	}

	// delivers this response event to the channel through which the
	// corresponding request event came (called holding read lock)
	private void deliverToCallerChannel(Event event, int wid,
			ChannelCore<?> caller) {
		Kompics.logger.debug("Caller +{}-{} in {} fwd {}", new Object[] {
				caller.getPositivePort().pair.owner.component,
				caller.getNegativePort().pair.owner.component,
				caller.getNegativePort().owner.component, event });
		if (positive) {
			caller.forwardToNegative(event, wid);
		} else {
			caller.forwardToPositive(event, wid);
		}
	}

	// deliver event to the local component (called holding read lock)
	private boolean deliverToSubscribers(Event event, int wid,
			Class<? extends Event> eventType) {
		for (Class<? extends Event> eType : subs.keySet()) {
			if (eType.isAssignableFrom(eventType)) {
				// there is at least one subscription
				doDeliver(event, wid);
				return true;
			}
		}
		return false;
	}

	private void doDeliver(Event event, int wid) {
		eventQueue.offer(event);
		owner.eventReceived(this, wid);
	}

	Event pickFirstEvent() {
		return eventQueue.poll();
	}

	@SuppressWarnings("unchecked")
	private <E extends Event> Class<E> reflectHandlerEventType(
			Handler<E> handler) {
		Class<E> eventType = null;
		try {
			Method ms[] = handler.getClass().getDeclaredMethods(), m = null;
			for (Method m1 : ms) {
				if (m1.getName().equals("handle")) {
					m = m1;
					break;
				}
			}
			eventType = (Class<E>) m.getParameterTypes()[0];
		} catch (Exception e) {
			throw new RuntimeException("Cannot reflect handler event type for "
					+ "handler " + handler + ". Please specify it "
					+ "as an argument to the handler constructor.", e);
		} finally {
			if (eventType == null)
				throw new RuntimeException(
						"Cannot reflect handler event type for handler "
								+ handler + ". Please specify it "
								+ "as an argument to the handler constructor.");
		}
		return eventType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Port#getPortType()
	 */
	public P getPortType() {
		return portType;
	}
}
