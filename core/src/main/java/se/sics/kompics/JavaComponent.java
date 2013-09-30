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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The
 * <code>ComponentCore</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id$
 */
public class JavaComponent extends ComponentCore {

    /* outside ports */
    private HashMap<Class<? extends PortType>, JavaPort<? extends PortType>> positivePorts;
    private HashMap<Class<? extends PortType>, JavaPort<? extends PortType>> negativePorts;
    private JavaPort<ControlPort> positiveControl, negativeControl;
    ComponentDefinition component;

    /**
     * Instantiates a new component core.
     *
     * @param componentDefinition the component definition
     */
    public JavaComponent(ComponentDefinition componentDefinition) {
        this.positivePorts = new HashMap<Class<? extends PortType>, JavaPort<? extends PortType>>();
        this.negativePorts = new HashMap<Class<? extends PortType>, JavaPort<? extends PortType>>();
        this.parent = parentThreadLocal.get();
        this.component = componentDefinition;
        parentThreadLocal.set(null);
    }

    public JavaComponent(JavaComponent other) {
        this.positivePorts = other.positivePorts;
        this.negativePorts = other.negativePorts;
        this.parent = other.parent;
        this.component = other.component;
        parentThreadLocal.set(null);
    }

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

        negativeControl.doSubscribe(handleStarted);
        negativeControl.doSubscribe(handleStopped);

        return negativeControl;
    }

    @Override
    public <T extends ComponentDefinition> Component doCreate(Class<T> definition, Init<T> initEvent) {
        // create an instance of the implementing component type
        ComponentDefinition component;
        try {
            parentThreadLocal.set(this);
            component = createInstance(definition, initEvent);
            ComponentCore child = component.getComponentCore();


            //child.workCount.incrementAndGet();


            child.setScheduler(scheduler);
            if (children == null) {
                children = new LinkedList<ComponentCore>();
            }
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
        if (state == State.DESTROYED) {
            return;
        }
        this.wid = wid;
        //System.err.println("Executing " + wid);


//		New scheduling code: Run n and move to end of schedule
//		
        int n = Kompics.maxNumOfExecutedEvents.get();
        int count = 0;
        int wc = workCount.get();

        while ((count < n) && wc > 0) {
            Event event;
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
                event = nextPort.pickFirstEvent();
            }

            if (event == null) {
                Kompics.logger.debug("Couldn't find event to schedule: {} / {} / {}", new Object[]{component, state, wc});
            }

            ArrayList<Handler<?>> handlers = nextPort.getSubscribedHandlers(event);

            if (handlers != null) {
                for (int i = 0; i < handlers.size(); i++) {
                    executeEvent(event, handlers.get(i));
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
    private void executeEvent(Event event, Handler<?> handler) {
        try {
            ((Handler<Event>) handler).handle(event);
        } catch (Throwable throwable) {
            handleFault(throwable);
        }

    }

    @Override
    public void handleFault(Throwable throwable) {
        if (parent != null) {
            negativeControl.doTrigger(new Fault(throwable), wid, this);
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
            throw new RuntimeException("Kompics isolated fault ", throwable);
            // System.exit(1);
        }
    }
    // fault handler that swallows exceptions and logs them instead of printing
    // them to stderr and halting
    // void handleFault(Throwable throwable) {
    // if (parent != null) {
    // negativeControl.doTrigger(new Fault(throwable), wid, this);
    // } else {
    // StackTraceElement[] stackTrace = throwable.getStackTrace();
    // Kompics.logger.error("Kompics isolated fault: {}", throwable
    // .getMessage());
    // do {
    // for (int i = 0; i < stackTrace.length; i++) {
    // Kompics.logger.error("    {}", stackTrace[i]);
    // }
    // throwable = throwable.getCause();
    // if (throwable != null) {
    // stackTrace = throwable.getStackTrace();
    // Kompics.logger.error("Caused by: {}: {}", throwable,
    // throwable.getMessage());
    // }
    // } while (throwable != null);
    // }
    // }

    /* === LIFECYCLE === */
    private Set<Component> activeSet = new HashSet<Component>();
    Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            if (children != null) {
                Kompics.logger.debug(component + " starting");
                state = Component.State.STARTING;
                for (ComponentCore child : children) {
                    Kompics.logger.debug("Sending Start to child: " + child.getComponent());
                    // start child
                    ((PortCore<ControlPort>) child.getControl()).doTrigger(
                            Start.event, wid, component.getComponentCore());
                }
            } else {
                Kompics.logger.debug(component + " started");
                state = Component.State.ACTIVE;
                if (parent != null) {
                    ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Started(component.getComponentCore()), wid, component.getComponentCore());
                }
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

            if (children != null) {
                Kompics.logger.debug(component + " stopping");
                state = Component.State.STOPPING;
                for (ComponentCore child : children) {
                    Kompics.logger.debug("Sending Stop to child: " + child.getComponent());
                    // stop child
                    ((PortCore<ControlPort>) child.getControl()).doTrigger(
                            Stop.event, wid, component.getComponentCore());
                }
            } else {
                Kompics.logger.debug(component + " stopped");
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
        public java.lang.Class<Stop> getEventType() {
            return Stop.class;
        }
    ;
    };
        
    Handler<Started> handleStarted = new Handler<Started>() {
        @Override
        public void handle(Started event) {
            Kompics.logger.debug(component + " got Started event from " + event.component.getComponent());
            activeSet.add(event.component);
            Kompics.logger.debug(component + " active set has " + activeSet.size() + " members");
            if ((activeSet.size() == children.size()) && (state == Component.State.STARTING)) {
                Kompics.logger.debug(component + " started");
                state = Component.State.ACTIVE;
                if (parent != null) {
                    ((PortCore<ControlPort>) parent.getControl()).doTrigger(new Started(component.getComponentCore()), wid, component.getComponentCore());
                }
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
            Kompics.logger.debug(component + " got Stopped event from " + event.component.getComponent());

            activeSet.remove(event.component);
            Kompics.logger.debug(component + " active set has " + activeSet.size() + " members");
            if (activeSet.isEmpty() && (state == Component.State.STOPPING)) {
                Kompics.logger.debug(component + " stopped");
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

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public ComponentDefinition getComponent() {
        return component;
    }
}
