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

import com.google.common.collect.ArrayListMultimap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The <code>PortCore</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id$
 */
public class JavaPort<P extends PortType> extends PortCore<P> {

    private JavaPort<P> pair;
    private ReentrantReadWriteLock rwLock;
    private final ArrayListMultimap<Class<? extends KompicsEvent>, Handler<?>> subs = ArrayListMultimap.create();
    private final HashMap<Class<? extends PatternExtractor>, ArrayListMultimap<Object, MatchedHandler>> matchers = new HashMap<Class<? extends PatternExtractor>, ArrayListMultimap<Object, MatchedHandler>>();
    private ArrayList<ChannelCore<P>> normalChannels = new ArrayList<ChannelCore<P>>();
    private ChannelSelectorSet selectorChannels = new ChannelSelectorSet();
    ;
    private SpinlockQueue<KompicsEvent> eventQueue = new SpinlockQueue<KompicsEvent>();

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
        // this.subs = new HashMap<Class<? extends KompicsEvent>,
        // ArrayList<Handler<?>>>();
        // this.allChannels = new ArrayList<ChannelCore<P>>();
        // this.normalChannels = new ArrayList<ChannelCore<P>>();
        // this.selectorChannels = new ChannelSelectorSet();
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
        rwLock.writeLock().lock();
        try {
            normalChannels.add(channel);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void addChannel(ChannelCore<P> channel, ChannelSelector<?, ?> filter) {
        rwLock.writeLock().lock();
        try {
            selectorChannels.addChannelFilter(channel, filter);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void removeChannel(ChannelCore<P> channel) {
        rwLock.writeLock().lock();
        try {
            selectorChannels.removeChannel(channel);
            normalChannels.remove(channel);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // delivers the event to the connected channels (called holding read lock)
    private boolean deliverToChannels(KompicsEvent event, int wid) {
        //Kompics.logger.debug("{}: trying to deliver {} to channels...", owner.getComponent(), event);
        boolean delivered = false;
        if (normalChannels != null) {
            for (ChannelCore<?> channel : normalChannels) {
                if (isPositive) {
                    channel.forwardToNegative(event, wid);
                } else {
                    channel.forwardToPositive(event, wid);
                }
                delivered = true;
            }
        }
        if (selectorChannels != null) {
            ArrayList<ChannelCore<?>> channels = selectorChannels.get(event);
            if (channels != null) {
                for (ChannelCore channel : channels) {
                    if (isPositive) {
                        channel.forwardToNegative(event, wid);
                    } else {
                        channel.forwardToPositive(event, wid);
                    }
                    delivered = true;
                }
            }
        }
        //Kompics.logger.debug("{}: {}", owner.getComponent(), delivered ? "succeeded" : "failed");
        return delivered;
    }

    @Override
    public <E extends KompicsEvent> void doSubscribe(Handler<E> handler) {
        Class<E> eventType = handler.getEventType();
        if (eventType == null) {
            eventType = reflectHandlerEventType(handler);
            if (Fault.class.isAssignableFrom(eventType)) {
                throw new RuntimeException("Custom Fault handlers are not support anymore! Please override ComponentDefinition.handleFault() instead, for custom Fault handling.");
            }
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
            subs.put(handler.getEventType(), handler);

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void doSubscribe(MatchedHandler handler) {
        if (handler instanceof ClassMatchedHandler) {
            ClassMatchedHandler cmh = (ClassMatchedHandler) handler;
            reflectCMHType(cmh);
        }
        Class cxtType = handler.getCxtType();
        if (cxtType == null) {
            cxtType = reflectHandlerCxtType(handler);
            handler.setCxtType(cxtType);
        }

        // check that the port type carries the event type in this direction
        if (!portType.hasEvent(isPositive, cxtType)) {
            throw new RuntimeException("Cannot subscribe handler " + handler
                    + " to " + (isPositive ? "positive " : "negative ")
                    + portType.getClass().getCanonicalName() + " for "
                    + cxtType.getCanonicalName() + " events.");
        }

        rwLock.writeLock().lock();
        try {
            ArrayListMultimap<Object, MatchedHandler> patterns = matchers.get(handler.getCxtType());
            if (patterns == null) {
                patterns = ArrayListMultimap.create();
                matchers.put(cxtType, patterns);
            }
            patterns.put(handler.pattern(), handler);

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    <E extends KompicsEvent> void doInternalSubscribe(Handler<E> handler) {
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
            subs.put(handler.getEventType(), handler);

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    <E extends KompicsEvent> void doUnsubscribe(Handler<E> handler) {
        Class<E> eventType = handler.getEventType();
        if (eventType == null) {
            eventType = reflectHandlerEventType(handler);
            handler.setEventType(eventType);
        }

        rwLock.writeLock().lock();
        try {
            if (!subs.remove(handler.getEventType(), handler)) {
                throw new RuntimeException("Handler " + handler
                        + " is not subscribed to "
                        + (isPositive ? "positive " : "negative ")
                        + portType.getClass().getCanonicalName() + " for "
                        + eventType.getCanonicalName() + " events.");
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    void doUnsubscribe(MatchedHandler handler) {
        Class cxtType = handler.getCxtType();
        if (cxtType == null) {
            cxtType = reflectHandlerCxtType(handler);
            handler.setCxtType(cxtType);
        }

        rwLock.writeLock().lock();
        try {
            ArrayListMultimap<Object, MatchedHandler> patterns = matchers.get(handler.getCxtType());
            if (patterns == null) {
                throw new RuntimeException("Handler " + handler
                        + " is not subscribed to "
                        + (isPositive ? "positive " : "negative ")
                        + portType.getClass().getCanonicalName() + " for "
                        + cxtType.getCanonicalName() + " events with pattern "
                        + handler.pattern() + ".");
            }
            if (!patterns.remove(handler.pattern(), handler)) {
                throw new RuntimeException("Handler " + handler
                        + " is not subscribed to "
                        + (isPositive ? "positive " : "negative ")
                        + portType.getClass().getCanonicalName() + " for "
                        + cxtType.getCanonicalName() + " events with pattern "
                        + handler.pattern() + ".");
            } else {
                if (patterns.isEmpty()) {
                    matchers.remove(handler.getCxtType());
                }
            }

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    List<Handler<?>> getSubscribedHandlers(KompicsEvent event) {

        Class<? extends KompicsEvent> eventType = event.getClass();
        List<Handler<?>> ret = new LinkedList<Handler<?>>();

        for (Class<? extends KompicsEvent> eType : subs.keySet()) {
            if (eType.isAssignableFrom(eventType)) {
                List<Handler<?>> handlers = subs.get(eType);
                if (handlers != null) {
                    ret.addAll(handlers);
                }
            }
        }
        return ret;
    }

    List<MatchedHandler> getSubscribedMatchers(PatternExtractor event) {
        Class<? extends KompicsEvent> eventType = event.getClass();
        List<MatchedHandler> ret = new LinkedList<MatchedHandler>();

        for (Class<? extends KompicsEvent> eType : matchers.keySet()) {
            if (eType.isAssignableFrom(eventType)) {
                ArrayListMultimap<Object, MatchedHandler> patterns = matchers.get(eType);
                if (patterns == null) {
                    continue;
                }
                List<MatchedHandler> handlers = patterns.get(event.extractPattern());
                if (handlers != null) {
                    ret.addAll(handlers);
                }
            }
        }
        return ret;
    }

    // TODO optimize trigger/subscribe
    @Override
    public void doTrigger(KompicsEvent event, int wid, ChannelCore<?> channel) {
        //System.out.println(this.getClass()+": "+event+" triggert from "+channel);
        if (event instanceof Request) {
            Request request = (Request) event;
            request.pushPathElement(channel);
        }
        pair.deliver(event, wid);
    }

    @Override
    public void doTrigger(KompicsEvent event, int wid, ComponentCore component) {
        //System.out.println(this.getClass()+": "+event+" triggert from "+component);
        if (event instanceof Request) {
            Request request = (Request) event;
            request.pushPathElement(component);
        }
        pair.deliver(event, wid);
    }

    private void deliver(KompicsEvent event, int wid) {
        Class<? extends KompicsEvent> eventType = event.getClass();
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
            } else if (event instanceof Direct.Response) {
                delivered = deliverToSubscribers(event, wid, eventType);
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
//                if (event instanceof Fault) {
//                    // forward fault to parent component
//                    if (owner.parent != null) {
//                        ((PortCore<?>) owner.getComponent().control).doTrigger(
//                                event, wid, owner.getComponent().getComponentCore());
//                    } else {
//                        owner.escalateFault(((Fault) event));
//                    }
//                } else {
//                    // warning, dropped event
//                    // Kompics.logger.warn("Warning: {} event dropped by {} {} in"
//                    // + " component {}", new Object[] {
//                    // eventType.getCanonicalName(),
//                    // (positive ? "positive " : "negative "),
//                    // portType.getClass().getCanonicalName(),
//                    // owner.getComponent() });
//                }
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
    private boolean deliverToCallerChannel(KompicsEvent event, int wid,
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
    private boolean deliverToSubscribers(KompicsEvent event, int wid,
            Class<? extends KompicsEvent> eventType) {
        //Kompics.logger.debug("{}: trying to deliver {} to subscribers...", owner, event);

        for (Class<? extends KompicsEvent> eType : subs.keySet()) {
            if (eType.isAssignableFrom(eventType)) {
                // there is at least one subscription
                doDeliver(event, wid);
                //Kompics.logger.debug("{}: Delivered {} to subscribers", owner.getComponent(), event);
                return true;
            }
        }
        if (event instanceof PatternExtractor) {
            PatternExtractor pe = (PatternExtractor) event;
            for (Class<? extends KompicsEvent> eType : matchers.keySet()) {
                if (eType.isAssignableFrom(eventType)) {
                    ArrayListMultimap<Object, MatchedHandler> patterns = matchers.get(eType);
                    if (patterns == null) {
                        continue;
                    }
                    List<MatchedHandler> handlers = patterns.get(pe.extractPattern());
                    if (!handlers.isEmpty()) {
                        // there is at least one subscription
                        doDeliver(event, wid);
                        //Kompics.logger.debug("{}: Delivered {} to subscribers", owner.getComponent(), event);
                        return true;
                    }
                }
            }
        }
        //Kompics.logger.debug("{}: Couldn't deliver {}, no matching subscribers", owner.getComponent(), event);
        return false;
    }

    private void doDeliver(KompicsEvent event, int wid) {
        owner.eventReceived(this, event, wid);
    }

    @Override
    public void enqueue(KompicsEvent event) {
        eventQueue.offer(event);
    }

    KompicsEvent pickFirstEvent() {
        return eventQueue.poll();
    }

    boolean hasEvent() {
        return !eventQueue.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private <E extends KompicsEvent> Class<E> reflectEventType(Class handlerC, int parameter) {
        Class<E> eventType = null;
        try {
            Method declared[] = handlerC.getDeclaredMethods();
            // The JVM in Java 7 wrongly reflects the "handle" methods for some 
            // handlers: e.g. both `handle(Event e)` and `handle(Message m)` are
            // reflected as "declared" methods when only the second is actually
            // declared in the handler. A workaround is to reflect all `handle`
            // methods and pick the one with the most specific event type.
            // This sorted set stores the event types of all reflected handler
            // methods topologically ordered by the event type relationships.
            TreeSet<Class<? extends KompicsEvent>> relevant
                    = new TreeSet<Class<? extends KompicsEvent>>(
                            new Comparator<Class<? extends KompicsEvent>>() {
                                @Override
                                public int compare(Class<? extends KompicsEvent> e1,
                                        Class<? extends KompicsEvent> e2) {
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
                            (Class<? extends KompicsEvent>) m.getParameterTypes()[parameter]);
                }
            }
            eventType = (Class<E>) relevant.first();
        } catch (Exception e) {
            throw new RuntimeException("Cannot reflect handler event type for "
                    + "handler " + handlerC + ". Please specify it "
                    + "as an argument to the handler constructor.", e);
        } finally {
            if (eventType == null) {
                throw new RuntimeException(
                        "Cannot reflect handler event type for handler "
                        + handlerC + ". Please specify it "
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

    @Override
    public void cleanChannels() {
        rwLock.writeLock().lock();
        try {
            selectorChannels.clear();
            normalChannels.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void cleanEvents() {
        eventQueue.clear();
    }

    private void reflectCMHType(ClassMatchedHandler cmh) {
        Class cmhType = reflectEventType(cmh.getClass(), 0);
        cmh.setPattern(cmhType);
    }

    private Class reflectHandlerCxtType(MatchedHandler handler) {
        return reflectEventType(handler.getClass(), 1);
    }

    private <E extends Object & KompicsEvent> Class<E> reflectHandlerEventType(Handler<E> handler) {
        return reflectEventType(handler.getClass(), 0);
    }

    @Override
    public List<Channel<P>> findChannelsTo(PortCore<P> port) {
        List<Channel<P>> channels = new ArrayList<Channel<P>>();
        for (ChannelCore<P> c : normalChannels) {
            if (this.isPositive) {
                if (c.hasNegativePort(port)) {
                    channels.add(c);
                }
            } else {
                if (c.hasPositivePort(port)) {
                    channels.add(c);
                }
            }
        }
        for (ChannelCore<?> cnt : selectorChannels) {
            ChannelCore<P> c = (ChannelCore<P>) cnt; // must be right type...just got lost in the ChannelSelector
            if (this.isPositive) {
                if (c.hasNegativePort(port)) {
                    channels.add(c);
                }
            } else {
                if (c.hasPositivePort(port)) {
                    channels.add(c);
                }
            }
        }
        return channels;
    }
}
