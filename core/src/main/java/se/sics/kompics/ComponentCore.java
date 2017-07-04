/**
 * This file is part of the Kompics component model runtime.
 * <p>
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.ConfigUpdate;

/**
 * The <code>ComponentCore</code> class.
 * <p>
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @version $Id: $
 */
public abstract class ComponentCore implements Component {

    private final UUID id = UUID.randomUUID();
    protected ComponentCore parent;
    protected Config conf;
    public static ThreadLocal<ComponentCore> parentThreadLocal = new ThreadLocal<ComponentCore>();
    public static ThreadLocal<ConfigUpdate> childUpdate = new ThreadLocal<ConfigUpdate>();
    protected List<ComponentCore> children = new LinkedList<ComponentCore>();
    ;
    protected final ReentrantReadWriteLock childrenLock = new ReentrantReadWriteLock();
    protected Scheduler scheduler;
    protected int wid;

    protected abstract Logger logger();
    
    public ComponentCore getParent() {
        return parent;
    }

    public Config config() {
        return conf;
    }

//    public <P extends PortType> Channel<P> doConnect(Positive<P> positive,
//            Negative<P> negative) {
//        PortCore<P> positivePort = (PortCore<P>) positive;
//        PortCore<P> negativePort = (PortCore<P>) negative;
//        ChannelCore<P> channel = new ChannelCoreImpl<P>(positivePort, negativePort,
//                negativePort.getPortType());
//
//        positivePort.addChannel(channel);
//        negativePort.addChannel(channel);
//
//        return channel;
//    }
//
//    public <P extends PortType> Channel<P> doConnect(Positive<P> positive,
//            Negative<P> negative, ChannelSelector<?, ?> filter) {
//        PortCore<P> positivePort = (PortCore<P>) positive;
//        PortCore<P> negativePort = (PortCore<P>) negative;
//        ChannelCore<P> channel = new ChannelCoreImpl<P>(positivePort, negativePort,
//                negativePort.getPortType());
//
//        Class<? extends KompicsEvent> eventType = filter.getEventType();
//        P portType = positivePort.getPortType();
//        if (filter.isPositive()) {
//            if (!portType.hasPositive(eventType)) {
//                throw new RuntimeException("Port type " + portType
//                        + " has no positive " + eventType);
//            }
//            positivePort.addChannel(channel, filter);
//            negativePort.addChannel(channel);
//        } else {
//            if (!portType.hasNegative(eventType)) {
//                throw new RuntimeException("Port type " + portType
//                        + " has no negative " + eventType);
//            }
//            positivePort.addChannel(channel);
//            negativePort.addChannel(channel, filter);
//        }
//
//        return channel;
//    }
//
//    public <P extends PortType> void doDisconnect(Positive<P> positive,
//            Negative<P> negative) {
//        PortCore<P> positivePort = (PortCore<P>) positive;
//        PortCore<P> negativePort = (PortCore<P>) negative;
//
//        positivePort.removeChannelTo(negativePort);
//        negativePort.removeChannelTo(positivePort);
//    }
    protected abstract void cleanPorts();

    public abstract Negative<ControlPort> createControlPort();

    protected void doDestroy(Component component) {
        ComponentCore child = (ComponentCore) component;
        child.cleanPorts();
        if ((child.state != State.PASSIVE) && (child.state != State.FAULTY)) {
            logger().warn("Destroying a component before it has been stopped is not a good idea: {}", child.getComponent());
        }
        child.state = State.DESTROYED;
        try {
            childrenLock.writeLock().lock();
            
            children.remove(child);
        } finally {
            childrenLock.writeLock().unlock();
        }
    }

    protected void destroyTree(ComponentCore child) {
        try {
            childrenLock.writeLock().lock();
            child.childrenLock.writeLock().lock();
            for (ComponentCore grandchild : child.children) {
                child.destroyTree(grandchild);
            }
            child.getComponent().tearDown();
            doDestroy(child);
        } finally {
            child.childrenLock.writeLock().unlock();
            childrenLock.writeLock().unlock();
        }
    }

    protected abstract void setInactive(Component child);

    protected void markSubtreeAs(State s) {
        this.state = s;
        if (s == State.FAULTY || s == State.DESTROYED || s == State.PASSIVE) {
            if (parent != null) {
                parent.setInactive(this);
            }
        }
        try {
            childrenLock.readLock().lock();
            for (ComponentCore child : children) {
                child.markSubtreeAs(s);
            }
        } finally {
            childrenLock.readLock().unlock();
        }
    }

    abstract void doConfigUpdate(ConfigUpdate update);

    public abstract <T extends ComponentDefinition> Component doCreate(Class<T> definition, Init<T> initEvent);
    
    public abstract <T extends ComponentDefinition> Component doCreate(Class<T> definition, Init<T> initEvent, ConfigUpdate update);

    public abstract <P extends PortType> Negative<P> createNegativePort(Class<P> portType);

    public abstract <P extends PortType> Positive<P> createPositivePort(Class<P> portType);
    /*
     * === SCHEDULING ===
     */
    public AtomicInteger workCount = new AtomicInteger(0);
    protected SpinlockQueue<PortCore<?>> readyPorts = new SpinlockQueue<PortCore<?>>();

    /**
     * Sets the scheduler.
     * <p>
     * @param scheduler the new scheduler
     */
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void eventReceived(PortCore<?> port, KompicsEvent event, int wid) {
        //System.err.println("Received event " + event + " on " + port.getPortType().portTypeClass + " work " + workCount.get());
        port.enqueue(event);
        readyPorts.offer(port);
        int wc = workCount.getAndIncrement();
        if (wc == 0) {
            if (scheduler == null) {
                scheduler = Kompics.getScheduler();
            }
            //Kompics.logger.trace("Scheduling {} due to new event", this);
            scheduler.schedule(this, wid);
        }
    }

    public abstract void execute(int wid);

    @Override
    public UUID id() {
        return this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ComponentCore) {
            ComponentCore cc = (ComponentCore) obj;
            return this.id.equals(cc.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component(");
        sb.append(id);
        sb.append("):");
        sb.append(getComponent());
        return sb.toString();
    }
    /*
     * === LIFECYCLE ===
     */
    volatile protected Component.State state = Component.State.PASSIVE;

    @Override
    public Component.State state() {
        return state;
    }

    
    /*
    * === Relaying for package fields to Scala
    */
    
    protected void escalateFaultToKompics(Fault fault) {
        Kompics.handleFault(fault);
    }
    
    protected void markSubtreeAtAs(ComponentCore source, State s) {
        source.markSubtreeAs(s);
    }
    
    protected void destroyTreeAtParentOf(ComponentCore source) {
        source.parent.destroyTree(source);
    }
}
