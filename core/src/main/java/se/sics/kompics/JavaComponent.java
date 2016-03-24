/**
 * This file is part of the Kompics component model runtime.
 * <p>
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 * <p>
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import se.sics.kompics.Fault.ResolveAction;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.ConfigUpdate;
import se.sics.kompics.config.ValueMerger;

/**
 * The <code>ComponentCore</code> class.
 * <p>
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id$
 */
public class JavaComponent extends ComponentCore {

    /*
     * outside ports
     */
    private HashMap<Class<? extends PortType>, JavaPort<? extends PortType>> positivePorts;
    private HashMap<Class<? extends PortType>, JavaPort<? extends PortType>> negativePorts;
    private JavaPort<ControlPort> positiveControl, negativeControl;
    ComponentDefinition component;

    /**
     * Instantiates a new component core.
     * <p>
     * @param componentDefinition the component definition
     */
    public JavaComponent(ComponentDefinition componentDefinition) {
        this.positivePorts = new HashMap<Class<? extends PortType>, JavaPort<? extends PortType>>();
        this.negativePorts = new HashMap<Class<? extends PortType>, JavaPort<? extends PortType>>();
        this.parent = parentThreadLocal.get();
        if (this.parent != null) {
            this.conf = parent.conf.copy(componentDefinition.separateConfigId());
        } else {
            this.conf = Kompics.getConfig().copy(componentDefinition.separateConfigId());
        }
        if (childUpdate.get() != null) {
            Config.Impl ci = (Config.Impl) this.conf;
            ci.apply(childUpdate.get(), ValueMerger.NONE);
            childUpdate.set(null);
        }
        this.component = componentDefinition;
        parentThreadLocal.set(null);
    }

//    public JavaComponent(JavaComponent other) {
//        this.positivePorts = other.positivePorts;
//        this.negativePorts = other.negativePorts;
//        this.parent = other.parent;
//        this.conf = other.conf;
//        this.component = other.component;
//        parentThreadLocal.set(null);
//    }

    /*
     * (non-Javadoc)
     *
     * @see se.sics.kompics.Component#getControl()
     */
    @Override
    public Positive<ControlPort> getControl() {
        return positiveControl;
    }

    @Override
    public Positive<ControlPort> control() {
        return positiveControl;
    }

    /*
     * (non-Javadoc)
     *
     * @see se.sics.kompics.Component#getNegative(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <P extends PortType> Negative<P> getNegative(Class<P> portType) {
        Negative<P> port = (Negative<P>) negativePorts.get(portType);
        if (port == null) {
            throw new RuntimeException(component + " has no negative "
                    + portType.getCanonicalName());
        }
        return port;
    }

    @Override
    public <P extends PortType> Negative<P> required(Class<P> portType) {
        return getNegative(portType);
    }

    /*
     * (non-Javadoc)
     *
     * @see se.sics.kompics.Component#getPositive(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <P extends PortType> Positive<P> getPositive(Class<P> portType) {
        Positive<P> port = (Positive<P>) positivePorts.get(portType);
        if (port == null) {
            throw new RuntimeException(component + " has no positive "
                    + portType.getCanonicalName());
        }
        return port;
    }

    @Override
    public <P extends PortType> Positive<P> provided(Class<P> portType) {
        return getPositive(portType);
    }

    @Override
    public <P extends PortType> Negative<P> createNegativePort(Class<P> portType) {
        JavaPort<P> negativePort = new JavaPort<P>(false,
                PortType.getPortType(portType), this);
        JavaPort<P> positivePort = new JavaPort<P>(true,
                PortType.getPortType(portType), parent);

        negativePort.setPair(positivePort);
        positivePort.setPair(negativePort);

        Positive<?> existing = positivePorts.put(portType, positivePort);
        if (existing != null) {
            throw new RuntimeException("Cannot create multiple negative "
                    + portType.getCanonicalName());
        }
        return negativePort;
    }

    @Override
    public <P extends PortType> Positive<P> createPositivePort(Class<P> portType) {
        JavaPort<P> negativePort = new JavaPort<P>(false,
                PortType.getPortType(portType), parent);
        JavaPort<P> positivePort = new JavaPort<P>(true,
                PortType.getPortType(portType), this);

        negativePort.setPair(positivePort);
        positivePort.setPair(negativePort);

        Negative<?> existing = negativePorts.put(portType, negativePort);
        if (existing != null) {
            throw new RuntimeException("Cannot create multiple positive "
                    + portType.getCanonicalName());
        }
        return positivePort;
    }

    @Override
    public Negative<ControlPort> createControlPort() {
        negativeControl = new JavaPort<ControlPort>(false,
                PortType.getPortType(ControlPort.class), this);
        positiveControl = new JavaPort<ControlPort>(true,
                PortType.getPortType(ControlPort.class), parent);

        positiveControl.setPair(negativeControl);
        negativeControl.setPair(positiveControl);

        negativeControl.doSubscribe(handleStart);
        negativeControl.doSubscribe(handleStop);
        negativeControl.doSubscribe(handleKill);

        negativeControl.doSubscribe(handleStarted);
        negativeControl.doSubscribe(handleStopped);
        negativeControl.doSubscribe(handleKilled);

        negativeControl.doInternalSubscribe(handleFault);

        negativeControl.doInternalSubscribe(configHandler);

        return negativeControl;
    }

    @Override
    protected void cleanPorts() {
        for (JavaPort<? extends PortType> port : negativePorts.values()) {
            port.cleanChannels();
        }
        for (JavaPort<? extends PortType> port : positivePorts.values()) {
            port.cleanChannels();
        }
    }

    @Override
    public <T extends ComponentDefinition> Component doCreate(Class<T> definition, Init<T> initEvent) {
        return doCreate(definition, initEvent, null);
    }

    @Override
    public <T extends ComponentDefinition> Component doCreate(Class<T> definition, Init<T> initEvent, ConfigUpdate update) {
        // create an instance of the implementing component type
        ComponentDefinition component;
        childrenLock.writeLock().lock();
        try {
            parentThreadLocal.set(this);
            childUpdate.set(update);
            component = createInstance(definition, initEvent);
            ComponentCore child = component.getComponentCore();

            //child.workCount.incrementAndGet();
            child.setScheduler(scheduler);

            children.add(child);

            return child;
        } catch (InstantiationException e) {
            throw new RuntimeException("Cannot create component "
                    + definition.getCanonicalName(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot create component "
                    + definition.getCanonicalName(), e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot create component "
                    + definition.getCanonicalName(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot create component "
                    + definition.getCanonicalName(), e);
        } finally {
            childrenLock.writeLock().unlock();
        }
    }

    
    private <T extends ComponentDefinition> T createInstance(Class<T> definition, Init<T> initEvent) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (initEvent == null) {
            return definition.newInstance();
        }
        // look for a constructor that takes a single parameter
        // and is assigment compatible with the given init event
        Constructor<T> constr = definition.getConstructor(initEvent.getClass());
        return constr.newInstance(initEvent);
//        Constructor[] constructors = definition.getConstructors();
//        for (Constructor constr : constructors) {
//            Class[] types = constr.getParameterTypes();
//            if (types.length == 1) {
//                Class type = types[0];
//                if (type.isInstance(initEvent)) {
//                    return constr.newInstance(initEvent);
//                }
//            }
//        }
    }

    @Override
    public void execute(int wid) {
        if ((state == State.DESTROYED) || (state == State.FAULTY)) {
            return; // don't schedule these components
        }
        this.wid = wid;
        //System.err.println("Executing " + wid);

//		New scheduling code: Run n and move to end of schedule
//		
        int n = Kompics.maxNumOfExecutedEvents.get();
        int count = 0;
        int wc = workCount.get();

        while ((count < n) && wc > 0) {
            if (state == State.FAULTY) { // state might have changed between iterations
                return;
            }
            KompicsEvent event;
            JavaPort<?> nextPort;
            if ((state == State.PASSIVE) || (state == State.STARTING)) {
                //System.err.println("non-active state " + wid);

                event = negativeControl.pickFirstEvent();
                nextPort = negativeControl;

                if (event == null) {
                    Kompics.logger.debug("Not scheduling component {} / State is {}", component, state);
                    // try again
                    if (wc > 0) {
                        if (scheduler == null) {
                            scheduler = Kompics.getScheduler();
                        }
                        scheduler.schedule(this, wid);
                    }
                    return; // Don't run anything else
                }
                readyPorts.remove(nextPort);
            } else {
                //System.err.println("active state " + wid);
                nextPort = (JavaPort<?>) readyPorts.poll();
                if (nextPort == null) {
                    wc = workCount.decrementAndGet();
                    count++;
                    continue;
                }
                event = nextPort.pickFirstEvent();
            }

            if (event == null) {
                Kompics.logger.debug("Couldn't find event to schedule: {} / {} / {}", new Object[]{component, state, wc});
                wc = workCount.decrementAndGet();
                count++;
                continue;
            }

            List<Handler<?>> handlers = nextPort.getSubscribedHandlers(event);

            if (handlers != null) {
                for (Handler h : handlers) {
                    if (executeEvent(event, h)) {
                        break; // state changed don't handle the rest of the event
                    }
                }
            }
            if (event instanceof PatternExtractor) {
                PatternExtractor pe = (PatternExtractor) event;
                List<MatchedHandler> mhandlers = nextPort.getSubscribedMatchers(pe);
                if (mhandlers != null) {
                    for (MatchedHandler mh : mhandlers) {
                        if (executeEvent(pe, mh)) {
                            break; // state changed don't handle the rest of the event
                        }
                    }
                }
            }
            wc = workCount.decrementAndGet();
            count++;
        }

        if (wc > 0) {
            if (scheduler == null) {
                scheduler = Kompics.getScheduler();
            }
            scheduler.schedule(this, wid);
        }

//		Classic scheduling code: Run once and move to end of schedule
//		
//		// 1. pick a port with a non-empty event queue
//		// 2. execute the first event
//		// 3. make component ready
//
//		JavaPort<?> nextPort = (JavaPort<?>) readyPorts.poll();
//
//		Event event = nextPort.pickFirstEvent();
//
//		ArrayList<Handler<?>> handlers = nextPort.getSubscribedHandlers(event);
//
//		if (handlers != null) {
//			for (int i = 0; i < handlers.size(); i++) {
//				executeEvent(event, handlers.get(i));
//			}
//		}
//
//		int wc = workCount.decrementAndGet();
//		if (wc > 0) {
//			if (scheduler == null)
//				scheduler = Kompics.getScheduler();
//			scheduler.schedule(this, wid);
//		}
    }

    @SuppressWarnings("unchecked")
    private boolean executeEvent(KompicsEvent event, Handler<?> handler) {
        try {
            ((Handler<KompicsEvent>) handler).handle(event);
            return false; // no state change
        } catch (Throwable throwable) {
            Kompics.logger.error("Handling an event caused a fault! Might be handled later...", throwable);
            markSubtreeAs(State.FAULTY);
            escalateFault(new Fault(throwable, this, event));
            return true; // state changed
        }
    }

    @SuppressWarnings("unchecked")
    private boolean executeEvent(PatternExtractor event, MatchedHandler handler) {
        try {
            handler.handle(event.extractValue(), event);
            return false; // no state change
        } catch (Throwable throwable) {
            Kompics.logger.error("Handling an event caused a fault! Might be handled later...", throwable);
            markSubtreeAs(State.FAULTY);
            escalateFault(new Fault(throwable, this, event));
            return true; // state changed
        }
    }

    @Override
    public void escalateFault(Fault fault) {
        if (parent != null) {
            parent.control().doTrigger(fault, wid, this);
        } else {
            // StackTraceElement[] stackTrace = throwable.getStackTrace();
            // System.err.println("Kompics isolated fault: "
            // + throwable.getMessage());
            // do {
            // for (int i = 0; i < stackTrace.length; i++) {
            // System.err.println("    " + stackTrace[i]);
            // }
            // throwable = throwable.getCause();
            // if (throwable != null) {
            // stackTrace = throwable.getStackTrace();
            // System.err.println("Caused by: " + throwable + ": "
            // + throwable.getMessage());
            // }
            // } while (throwable != null);
            Kompics.logger.error("A fault was escalated to the root component: \n{} \n\n", fault);
            Kompics.handleFault(fault);
            // System.exit(1);
        }
    }

    Handler<Fault> handleFault = new Handler<Fault>() {

        @Override
        public void handle(Fault event) {

            ResolveAction ra = component.handleFault(event);
            switch (ra) {
                case RESOLVED:
                    Kompics.logger.info("Fault {} was resolved by user.", event);
                    break;
                case IGNORE:
                    Kompics.logger.info("Fault {} was declared to be ignored by user. Resuming component...", event);
                    markSubtreeAtAs(event.source, State.PASSIVE);
                    event.source.control().doTrigger(Start.event, wid, JavaComponent.this);
                    break;
                case DESTROY:
                    Kompics.logger.info("User declared that Fault {} should destroy component tree...", event);
                    destroyTreeAtParentOf(event.source);
                    Kompics.logger.info("finished destroying the subtree.");
                    break;
                default:
                    escalateFault(event);
            }
        }
    };
    Handler<Update> configHandler = new Handler<Update>() {

        @Override
        public void handle(Update event) {
            UpdateAction action = JavaComponent.this.component.handleUpdate(event.update);
            switch (action.selfStrategy) {
                case ORIGINAL:
                    ((Config.Impl) conf).apply(event.update, action.merger);
                    break;
                case MAP:
                    ((Config.Impl) conf).apply(
                            action.selfMapper.map(
                                    event.update,
                                    event.update.modify(id())
                            ), action.merger
                    );
                    break;
                case SWALLOW:
                    break;
            }
            if ((parent != null) && (event.forwarder == parent.id())) { // downwards
                switch (action.downStrategy) {
                    case ORIGINAL: {
                        Update forwardedEvent = new Update(event.update, id());
                        for (Component child : children) {
                            ((PortCore<ControlPort>) child.getControl()).doTrigger(
                                    forwardedEvent, wid, component.getComponentCore());
                        }
                    }
                    break;
                    case MAP: {
                        ConfigUpdate mappedUpdate = action.downMapper.map(event.update, event.update.modify(id()));
                        Update forwardedEvent = new Update(mappedUpdate, id());
                        for (Component child : children) {
                            ((PortCore<ControlPort>) child.getControl()).doTrigger(
                                    forwardedEvent, wid, component.getComponentCore());
                        }
                    }
                    break;
                    case SWALLOW:
                        break;
                }
            } else { // upwards and to other children
                switch (action.downStrategy) {
                    case ORIGINAL: {
                        Update forwardedEvent = new Update(event.update, id());
                        for (Component child : children) {
                            if (child.id() != event.forwarder) {
                                ((PortCore<ControlPort>) child.getControl()).doTrigger(
                                        forwardedEvent, wid, component.getComponentCore());
                            }
                        }
                    }
                    break;
                    case MAP: {
                        ConfigUpdate mappedUpdate = action.downMapper.map(event.update, event.update.modify(id()));
                        Update forwardedEvent = new Update(mappedUpdate, id());
                        for (Component child : children) {
                            if (child.id() != event.forwarder) {
                                ((PortCore<ControlPort>) child.getControl()).doTrigger(
                                        forwardedEvent, wid, component.getComponentCore());
                            }
                        }
                    }
                    break;
                    case SWALLOW:
                        break;
                }
                if (parent != null) {
                    switch (action.upStrategy) {
                        case ORIGINAL: {
                            Update forwardedEvent = new Update(event.update, id());
                            ((PortCore<ControlPort>) parent.getControl()).doTrigger(
                                    forwardedEvent, wid, component.getComponentCore());
                        }
                        break;

                        case MAP: {
                            ConfigUpdate mappedUpdate = action.upMapper.map(event.update, event.update.modify(id()));
                            Update forwardedEvent = new Update(mappedUpdate, id());
                            ((PortCore<ControlPort>) parent.getControl()).doTrigger(
                                    forwardedEvent, wid, component.getComponentCore());
                        }
                        break;
                        case SWALLOW:
                            break;
                    }
                }
            }
            component.postUpdate();
        }
    };

    @Override
    public ComponentDefinition getComponent() {
        return component;
    }

    @Override
    void doConfigUpdate(ConfigUpdate update) {
        Config.Impl impl = (Config.Impl) conf;
        impl.apply(update, ValueMerger.NONE);
        Update forwardedEvent = new Update(update, id());
        // forward down
        for (Component child : children) {
            ((PortCore<ControlPort>) child.getControl()).doTrigger(
                    forwardedEvent, wid, this);
        }
        // forward up
        ((PortCore<ControlPort>) parent.getControl()).doTrigger(
                forwardedEvent, wid, this);
        component.postUpdate();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JavaComponent) {
            JavaComponent that = (JavaComponent) o;
            return this.id().equals(that.id());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.id());
        return hash;
    }
    /*
     * === LIFECYCLE ===
     */

    @Override
    void setInactive(Component child) {
        activeSet.remove(child);
    }
    private Set<Component> activeSet = new HashSet<Component>();
    Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            if (state != Component.State.PASSIVE) {
                throw new KompicsException(JavaComponent.this + " received a Start event while in " + state + " state. "
                        + "Duplicate Start events are not allowed!");
            }
            try {
                childrenLock.readLock().lock();
                if (!children.isEmpty()) {
                    Kompics.logger.debug(JavaComponent.this + " starting");
                    state = Component.State.STARTING;
                    for (ComponentCore child : children) {
                        Kompics.logger.debug("Sending Start to child: " + child);
                        // start child
                        ((PortCore<ControlPort>) child.getControl()).doTrigger(
                                Start.event, wid, component.getComponentCore());
                    }
                } else {
                    Kompics.logger.debug(JavaComponent.this + " started");
                    state = Component.State.ACTIVE;
                    if (parent != null) {
                        ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Started(component.getComponentCore()), wid, component.getComponentCore());
                    }
                }
            } finally {
                childrenLock.readLock().unlock();
            }
        }

        @Override
        public java.lang.Class<Start> getEventType() {
            return Start.class;
        }
    ;
    };
    
    Handler<Stop> handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            if (state != Component.State.ACTIVE) {
                throw new KompicsException(JavaComponent.this + " received a Stop event while in " + state + " state. "
                        + "Duplicate Stop events are not allowed!");
            }
            try {
                childrenLock.readLock().lock();
                if (!children.isEmpty()) {
                    Kompics.logger.debug(JavaComponent.this + " stopping");
                    state = Component.State.STOPPING;
                    for (ComponentCore child : children) {
                        if (child.state() != Component.State.ACTIVE) {
                            continue; // don't send stop events to already stopping components
                        }
                        Kompics.logger.debug("Sending Stop to child: " + child);
                        // stop child
                        ((PortCore<ControlPort>) child.getControl()).doTrigger(
                                Stop.event, wid, component.getComponentCore());
                    }
                } else {
                    Kompics.logger.debug(JavaComponent.this + " stopped");
                    state = Component.State.PASSIVE;
                    component.tearDown();
                    if (parent != null) {
                        ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Stopped(component.getComponentCore()), wid, component.getComponentCore());
                    } else {
                        synchronized (component.getComponentCore()) {
                            component.getComponentCore().notifyAll();
                        }
                    }
                }
            } finally {
                childrenLock.readLock().unlock();
            }
        }

        @Override
        public java.lang.Class<Stop> getEventType() {
            return Stop.class;
        }
    ;
    };
    
    Handler<Kill> handleKill = new Handler<Kill>() {

        @Override
        public void handle(Kill event) {
            if (state != Component.State.ACTIVE) {
                throw new KompicsException(JavaComponent.this + " received a Kill event while in " + state + " state. "
                        + "Duplicate Kill events are not allowed!");
            }
            try {
                childrenLock.readLock().lock();
                if (!children.isEmpty()) {
                    Kompics.logger.debug(JavaComponent.this + " slowly dying");
                    state = Component.State.STOPPING;
                    ((PortCore<ControlPort>) getControl().getPair()).cleanEvents(); // if multiple kills are queued up just ignore everything
                    for (ComponentCore child : children) {
                        if (child.state() != Component.State.ACTIVE) {
                            continue; // don't send stop events to already stopping components
                        }
                        Kompics.logger.debug("Sending Kill to child: " + child);
                        // stop child
                        ((PortCore<ControlPort>) child.getControl()).doTrigger(
                                Kill.event, wid, component.getComponentCore());
                    }
                } else {
                    Kompics.logger.debug(JavaComponent.this + " dying");
                    state = Component.State.PASSIVE;
                    ((PortCore<ControlPort>) getControl().getPair()).cleanEvents(); // if multiple kills are queued up just ignore everything
                    component.tearDown();
                    if (parent != null) {
                        ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Killed(component.getComponentCore()), wid, component.getComponentCore());
                    } else {
                        synchronized (component.getComponentCore()) {
                            component.getComponentCore().notifyAll();
                        }
                    }
                }
            } finally {
                childrenLock.readLock().unlock();
            }
        }

        @Override
        public java.lang.Class<Kill> getEventType() {
            return Kill.class;
        }

    };

    Handler<Killed> handleKilled = new Handler<Killed>() {

        @Override
        public void handle(Killed event) {
            Kompics.logger.debug(JavaComponent.this + " got Killed event from " + event.component);

            activeSet.remove(event.component);
            doDestroy(event.component);
            Kompics.logger.debug(JavaComponent.this + " active set has " + activeSet.size() + " members");
            if (activeSet.isEmpty() && (state == Component.State.STOPPING)) {
                Kompics.logger.debug(JavaComponent.this + " stopped");
                state = Component.State.PASSIVE;
                component.tearDown();
                if (parent != null) {
                    ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Killed(component.getComponentCore()), wid, component.getComponentCore());
                } else {
                    synchronized (component.getComponentCore()) {
                        component.getComponentCore().notifyAll();
                    }
                }
            }
        }

        @Override
        public java.lang.Class<Killed> getEventType() {
            return Killed.class;
        }
    };

    Handler<Started> handleStarted = new Handler<Started>() {
        @Override
        public void handle(Started event) {
            Kompics.logger.debug(JavaComponent.this + " got Started event from " + event.component);
            activeSet.add(event.component);
            Kompics.logger.debug(JavaComponent.this + " active set has " + activeSet.size() + " members");
            try {
                childrenLock.readLock().lock();
                if ((activeSet.size() == children.size()) && (state == Component.State.STARTING)) {
                    Kompics.logger.debug(JavaComponent.this + " started");
                    state = Component.State.ACTIVE;
                    if (parent != null) {
                        ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Started(component.getComponentCore()), wid, component.getComponentCore());
                    }
                }
            } finally {
                childrenLock.readLock().unlock();
            }

        }

        @Override
        public java.lang.Class<Started> getEventType() {
            return Started.class;
        }
    ;
    };

    Handler<Stopped> handleStopped = new Handler<Stopped>() {
        @Override
        public void handle(Stopped event) {
            Kompics.logger.debug(JavaComponent.this + " got Stopped event from " + event.component);

            activeSet.remove(event.component);
            Kompics.logger.debug(JavaComponent.this + " active set has " + activeSet.size() + " members");
            if (activeSet.isEmpty() && (state == Component.State.STOPPING)) {
                Kompics.logger.debug(JavaComponent.this + " stopped");
                state = Component.State.PASSIVE;
                component.tearDown();
                if (parent != null) {
                    ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Stopped(component.getComponentCore()), wid, component.getComponentCore());
                } else {
                    synchronized (component.getComponentCore()) {
                        component.getComponentCore().notifyAll();
                    }
                }
            }

        }

        @Override
        public java.lang.Class<Stopped> getEventType() {
            return Stopped.class;
        }
    ;

};

}
