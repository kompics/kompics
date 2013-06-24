/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The
 * <code>PortCore</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id$
 */
public class JavaPort<P extends PortType> extends PortCore<P> {

    private JavaPort<P> pair;
    private ReentrantReadWriteLock rwLock;
    private HashMap<Class<? extends Event>, ArrayList<Handler<?>>> subs;
    private ArrayList<ChannelCore<P>> allChannels;
    private ArrayList<ChannelCore<P>> unfilteredChannels;
    private ChannelFilterSet filteredChannels;
    private SpinlockQueue<Event> eventQueue = new SpinlockQueue<Event>();
    private HashMap<Port<P>, ChannelCore<P>> remotePorts;

    public JavaPort(JavaPort<P> other) {
        this.isPositive = other.isPositive;
        this.portType = other.portType;
        this.rwLock = other.rwLock;
        this.owner = other.owner;
        this.isControlPort = other.isControlPort;
    }

    JavaPort(boolean positive, P portType, ComponentCore owner) {
        this.isPositive = positive;
        this.portType = portType;
        this.rwLock = new ReentrantReadWriteLock();
        // this.subs = new HashMap<Class<? extends Event>,
        // ArrayList<Handler<?>>>();
        // this.allChannels = new ArrayList<ChannelCore<P>>();
        // this.unfilteredChannels = new ArrayList<ChannelCore<P>>();
        // this.filteredChannels = new ChannelFilterSet();
        // this.remotePorts = new HashMap<PortCore<P>, ChannelCore<P>>();
        this.owner = owner;
        this.isControlPort = (portType instanceof ControlPort);
    }

    @Override
    public void setPair(PortCore<P> pair) {
        if (pair instanceof JavaPort) {
            this.pair = (JavaPort<P>) pair;
        } else {
            throw new ConfigurationException("Can only pair up this port with another JavaPort instance");
        }
    }

    @Override
    public void addChannel(ChannelCore<P> channel) {
        Port<P> remotePort = (isPositive ? channel.getNegativePort()
                : channel.getPositivePort());

        if (remotePorts != null && remotePorts.containsKey(remotePort)) {
            throw new RuntimeException((isPositive ? "Positive " : "Negative ")
                    + portType.getClass().getCanonicalName() + " of "
                    + pair.getOwner().getComponent() + " is already connected to "
                    + (!isPositive ? "positive " : "negative ")
                    + portType.getClass().getCanonicalName() + " of "
                    + remotePort.getPair().getOwner().getComponent());
        }

        rwLock.writeLock().lock();
        try {
            if (remotePorts == null) {
                remotePorts = new HashMap<Port<P>, ChannelCore<P>>();
            }
            if (allChannels == null) {
                allChannels = new ArrayList<ChannelCore<P>>();
            }
            if (unfilteredChannels == null) {
                unfilteredChannels = new ArrayList<ChannelCore<P>>();
            }
            allChannels.add(channel);
            unfilteredChannels.add(channel);
            remotePorts.put(remotePort, channel);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void addChannel(ChannelCore<P> channel, ChannelFilter<?, ?> filter) {
        Port<P> remotePort = (isPositive ? channel.getNegativePort()
                : channel.getPositivePort());

        if (remotePorts != null && remotePorts.containsKey(remotePort)) {
            throw new RuntimeException((isPositive ? "Positive " : "Negative ")
                    + portType.getClass().getCanonicalName() + " of "
                    + pair.getOwner().getComponent() + " is already connected to "
                    + (!isPositive ? "positive " : "negative ")
                    + portType.getClass().getCanonicalName() + " of "
                    + remotePort.getPair().getOwner().getComponent());
        }

        rwLock.writeLock().lock();
        try {
            if (remotePorts == null) {
                remotePorts = new HashMap<Port<P>, ChannelCore<P>>();
            }
            if (allChannels == null) {
                allChannels = new ArrayList<ChannelCore<P>>();
            }
            if (filteredChannels == null) {
                filteredChannels = new ChannelFilterSet();
            }
            allChannels.add(channel);
            filteredChannels.addChannelFilter(channel, filter);
            remotePorts.put(remotePort, channel);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void removeChannelTo(PortCore<P> remotePort) {
        if (remotePorts == null || !remotePorts.containsKey(remotePort)) {
            throw new RuntimeException((isPositive ? "Positive " : "Negative ")
                    + portType.getClass().getCanonicalName() + " of "
                    + pair.getOwner().getComponent() + " is not connected to "
                    + (!isPositive ? "positive " : "negative ")
                    + portType.getClass().getCanonicalName() + " of "
                    + remotePort.getPair().getOwner().getComponent());
        }

        rwLock.writeLock().lock();
        try {
            ChannelCore<P> channel = remotePorts.remove(remotePort); // ! null
            channel.destroy();
            if (remotePorts.isEmpty()) {
                remotePorts = null;
            }

            if (filteredChannels != null) {
                filteredChannels.removeChannel(channel);
                if (filteredChannels.isEmpty()) {
                    filteredChannels = null;
                }
            }
            if (unfilteredChannels != null) {
                unfilteredChannels.remove(channel);
                if (unfilteredChannels.isEmpty()) {
                    unfilteredChannels = null;
                }
            }
            if (allChannels != null) {
                allChannels.remove(channel);
                if (allChannels.isEmpty()) {
                    allChannels = null;
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // delivers the event to the connected channels (called holding read lock)
    private boolean deliverToChannels(Event event, int wid) {
        //Kompics.logger.debug("{}: trying to deliver {} to channels...", owner.getComponent(), event);
        boolean delivered = false;
        if (unfilteredChannels != null) {
            for (ChannelCore<?> channel : unfilteredChannels) {
                if (isPositive) {
                    channel.forwardToNegative(event, wid);
                } else {
                    channel.forwardToPositive(event, wid);
                }
                delivered = true;
            }
        }
        if (filteredChannels != null) {
            ArrayList<ChannelCore<?>> channels = filteredChannels.get(event);
            if (channels != null) {
                for (int i = 0; i < channels.size(); i++) {
                    if (isPositive) {
                        channels.get(i).forwardToNegative(event, wid);
                    } else {
                        channels.get(i).forwardToPositive(event, wid);
                    }
                    delivered = true;
                }
            }
        }
        //Kompics.logger.debug("{}: {}", owner.getComponent(), delivered ? "succeeded" : "failed");
        return delivered;
    }

    @Override
    public <E extends Event> void doSubscribe(Handler<E> handler) {
        Class<E> eventType = handler.getEventType();
        if (eventType == null) {
            eventType = reflectHandlerEventType(handler);
            handler.setEventType(eventType);
        }

        // check that the port type carries the event type in this direction
        if (!portType.hasEvent(isPositive, eventType)) {
            throw new RuntimeException("Cannot subscribe handler " + handler
                    + " to " + (isPositive ? "positive " : "negative ")
                    + portType.getClass().getCanonicalName() + " for "
                    + eventType.getCanonicalName() + " events.");
        }

        rwLock.writeLock().lock();
        try {
            if (subs == null) {
                subs = new HashMap<Class<? extends Event>, ArrayList<Handler<?>>>();
            }
            ArrayList<Handler<?>> handlers = subs.get(eventType);
            if (handlers == null) {
                handlers = new ArrayList<Handler<?>>();
                subs.put(eventType, handlers);
            }
            handlers.add(handler);

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
            if (subs == null) {
                throw new RuntimeException("Handler " + handler
                        + " is not subscribed to "
                        + (isPositive ? "positive " : "negative ")
                        + portType.getClass().getCanonicalName() + " for "
                        + eventType.getCanonicalName() + " events.");
            }
            ArrayList<Handler<?>> handlers = subs.get(eventType);
            if (handlers == null) {
                throw new RuntimeException("Handler " + handler
                        + " is not subscribed to "
                        + (isPositive ? "positive " : "negative ")
                        + portType.getClass().getCanonicalName() + " for "
                        + eventType.getCanonicalName() + " events.");
            }

            if (!handlers.remove(handler)) {
                throw new RuntimeException("Handler " + handler
                        + " is not subscribed to "
                        + (isPositive ? "positive " : "negative ")
                        + portType.getClass().getCanonicalName() + " for "
                        + eventType.getCanonicalName() + " events.");
            }

            if (handlers.isEmpty()) {
                subs.remove(handlers);
                if (subs.isEmpty()) {
                    subs = null;
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    ArrayList<Handler<?>> getSubscribedHandlers(Event event) {
        if (subs == null) {
            return null;
        }

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
    @Override
    public void doTrigger(Event event, int wid, ChannelCore<?> channel) {
        //System.out.println(this.getClass()+": "+event+" triggert from "+channel);
        if (event instanceof Request) {
            Request request = (Request) event;
            request.pushPathElement(channel);
        }
        pair.deliver(event, wid);
    }

    @Override
    public void doTrigger(Event event, int wid, ComponentCore component) {
        //System.out.println(this.getClass()+": "+event+" triggert from "+component);
        if (event instanceof Request) {
            Request request = (Request) event;
            request.pushPathElement(component);
        }
        pair.deliver(event, wid);
    }

    private void deliver(Event event, int wid) {
        Class<? extends Event> eventType = event.getClass();
        boolean delivered = false;

        rwLock.readLock().lock();
        try {
            if (event instanceof Response) {
                Response response = (Response) event;
                RequestPathElement pe = response.getTopPathElement();
                if (pe != null) {
                    if (pe.isChannel()) {
                        ChannelCore<?> caller = (ChannelCore<?>) pe
                                .getChannel();
                        if (caller != null) {
                            // caller can be null since it is a WeakReference
                            delivered = deliverToCallerChannel(event, wid,
                                    caller);
                        }
                    } else {
                        ComponentCore component = pe.getComponent();
                        if (component == owner) {
                            delivered = deliverToSubscribers(event, wid,
                                    eventType);
                        } else {
                            throw new RuntimeException(
                                    "Response path invalid: expected to arrive to component "
                                    + component.getComponent()
                                    + " but instead arrived at "
                                    + owner.getComponent());
                        }
                    }
                } else {
                    // response event has arrived to request origin and was
                    // triggered further. We treat it as a regular event
                    delivered = deliverToSubscribers(event, wid, eventType);
                    delivered |= deliverToChannels(event, wid);
                }
            } else {
                // event is not a response event
                delivered = deliverToSubscribers(event, wid, eventType);
                delivered |= deliverToChannels(event, wid);
            }
        } finally {
            rwLock.readLock().unlock();
        }

        if (!delivered) {
            if (portType.hasEvent(isPositive, eventType)) {
                if (event instanceof Fault) {
                    // forward fault to parent component
                    if (owner.parent != null) {
                        ((PortCore<?>) owner.getComponent().control).doTrigger(
                                event, wid, owner.getComponent().getComponentCore());
                    } else {
                        owner.handleFault(((Fault) event).getFault());
                    }
                } else {
                    // warning, dropped event
                    // Kompics.logger.warn("Warning: {} event dropped by {} {} in"
                    // + " component {}", new Object[] {
                    // eventType.getCanonicalName(),
                    // (positive ? "positive " : "negative "),
                    // portType.getClass().getCanonicalName(),
                    // owner.getComponent() });
                }
            } else {
                // error, event type doesn't flow on this port in this direction
                throw new RuntimeException(eventType.getCanonicalName()
                        + " events cannot be triggered on "
                        + (!isPositive ? "positive " : "negative ")
                        + portType.getClass().getCanonicalName());
            }
        }
    }

    // delivers this response event to the channel through which the
    // corresponding request event came (called holding read lock)
    private boolean deliverToCallerChannel(Event event, int wid,
            ChannelCore<?> caller) {
        // Kompics.logger.debug("Caller +{}-{} in {} fwd {}", new Object[] {
        // caller.getPositivePort().pair.owner.getComponent(),
        // caller.getNegativePort().pair.owner.getComponent(),
        // caller.getNegativePort().owner.getComponent(), event });

        // do not deliver if this channel was disconnected
        // if (allChannels == null || !allChannels.contains(caller)) {
        // return false;
        // }

        if (isPositive) {
            caller.forwardToNegative(event, wid);
        } else {
            caller.forwardToPositive(event, wid);
        }
        return true;
    }

    // deliver event to the local component (called holding read lock)
    private boolean deliverToSubscribers(Event event, int wid,
            Class<? extends Event> eventType) {
        //Kompics.logger.debug("{}: trying to deliver {} to subscribers...", owner, event);
        if (subs == null) {
            //Kompics.logger.debug("{}: Couldn't deliver {}, no subscribers", owner.getComponent(), event);
            return false;
        }
        for (Class<? extends Event> eType : subs.keySet()) {
            if (eType.isAssignableFrom(eventType)) {
                // there is at least one subscription
                doDeliver(event, wid);
                //Kompics.logger.debug("{}: Delivered {} to subscribers", owner.getComponent(), event);
                return true;
            }
        }
        //Kompics.logger.debug("{}: Couldn't deliver {}, no matching subscribers", owner.getComponent(), event);
        return false;
    }

    private void doDeliver(Event event, int wid) {
        owner.eventReceived(this, event, wid);
    }

    @Override
    public void enqueue(Event event) {
        eventQueue.offer(event);
    }

    Event pickFirstEvent() {
        return eventQueue.poll();
    }

    boolean hasEvent() {
        return !eventQueue.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private <E extends Event> Class<E> reflectHandlerEventType(
            Handler<E> handler) {
        Class<E> eventType = null;
        try {
            Method declared[] = handler.getClass().getDeclaredMethods();
            // The JVM in Java 7 wrongly reflects the "handle" methods for some 
            // handlers: e.g. both `handle(Event e)` and `handle(Message m)` are
            // reflected as "declared" methods when only the second is actually
            // declared in the handler. A workaround is to reflect all `handle`
            // methods and pick the one with the most specific event type.
            // This sorted set stores the event types of all reflected handler
            // methods topologically ordered by the event type relationships.
            TreeSet<Class<? extends Event>> relevant =
                    new TreeSet<Class<? extends Event>>(
                    new Comparator<Class<? extends Event>>() {
                        @Override
                        public int compare(Class<? extends Event> e1,
                                Class<? extends Event> e2) {
                            if (e1.isAssignableFrom(e2)) {
                                return 1;
                            } else if (e2.isAssignableFrom(e1)) {
                                return -1;
                            }
                            return 0;
                        }
                    });
            for (Method m : declared) {
                if (m.getName().equals("handle")) {
                    relevant.add(
                            (Class<? extends Event>) m.getParameterTypes()[0]);
                }
            }
            eventType = (Class<E>) relevant.first();
        } catch (Exception e) {
            throw new RuntimeException("Cannot reflect handler event type for "
                    + "handler " + handler + ". Please specify it "
                    + "as an argument to the handler constructor.", e);
        } finally {
            if (eventType == null) {
                throw new RuntimeException(
                        "Cannot reflect handler event type for handler "
                        + handler + ". Please specify it "
                        + "as an argument to the handler constructor.");
            }
        }
        return eventType;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public PortCore<P> getPair() {
        return pair;
    }
}
