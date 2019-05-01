/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.kompics.network.virtual;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.ChannelSelector;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortCore;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;

/**
 * A network channel that allows switching on a virtual node id.
 * 
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class VirtualNetworkChannel implements ChannelCore<Network> {

    private static Logger log = LoggerFactory.getLogger(VirtualNetworkChannel.class);
    private volatile boolean destroyed = false;
    private final PortCore<Network> sourcePort;
    // Use HashMap for now and switch to a more efficient
    // datastructure if necessary
    private Map<ByteBuffer, Set<Negative<Network>>> destinationPorts = new HashMap<ByteBuffer, Set<Negative<Network>>>();
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private Negative<Network> deadLetterBox;
    private Set<Negative<Network>> hostPorts = new HashSet<Negative<Network>>();

    private VirtualNetworkChannel(Positive<Network> sourcePort, Negative<Network> deadLetterBox) {
        this.sourcePort = (PortCore<Network>) sourcePort;
        this.deadLetterBox = deadLetterBox;
    }

    @Override
    public boolean hasPositivePort(Port<Network> port) {
        return port == sourcePort;
    }

    @Override
    public boolean hasNegativePort(Port<Network> port) {
        rwlock.readLock().lock();
        if (port == deadLetterBox) {
            return true;
        }
        try {
            for (Set<Negative<Network>> portSet : destinationPorts.values()) {
                if (portSet.contains(port)) {
                    return true;
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return false;
    }

    @Override
    public void disconnect() {
        rwlock.writeLock().lock();
        try {
            destroyed = true;
            // ATTENTION: Possible deadlock due to double locking (don't think it will
            // happen, but if it does the fault
            // is here!)
            sourcePort.removeChannel(this);
            deadLetterBox.removeChannel(this);
            for (Set<Negative<Network>> portSet : destinationPorts.values()) {
                for (Negative<Network> port : portSet) {
                    PortCore<Network> p = (PortCore<Network>) port;
                    p.removeChannel(this);
                }
            }
            destinationPorts.clear();
            for (Negative<Network> port : hostPorts) {
                PortCore<Network> p = (PortCore<Network>) port;
                p.removeChannel(this);
            }
            hostPorts.clear();
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort, ComponentProxy parent) {
        Component deadLetterBox = parent.create(DefaultDeadLetterComponent.class, Init.NONE);
        return connect(sourcePort, deadLetterBox.getNegative(Network.class));
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort, Negative<Network> deadLetterBox) {
        VirtualNetworkChannel vnc = new VirtualNetworkChannel(sourcePort, deadLetterBox);
        sourcePort.addChannel(vnc);
        deadLetterBox.addChannel(vnc);

        return vnc;
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort, ChannelSelector<?, ?> selector,
            ComponentProxy parent) {
        Component deadLetterBox = parent.create(DefaultDeadLetterComponent.class, Init.NONE);
        return connect(sourcePort, deadLetterBox.getNegative(Network.class), selector);
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort, Negative<Network> deadLetterBox,
            ChannelSelector<?, ?> selector) {
        VirtualNetworkChannel vnc = new VirtualNetworkChannel(sourcePort, deadLetterBox);
        sourcePort.addChannel(vnc, selector);
        deadLetterBox.addChannel(vnc);

        return vnc;
    }

    public void addConnection(byte[] id, Negative<Network> destinationPort) {
        rwlock.writeLock().lock();
        try {
            if (id == null) {
                hostPorts.add(destinationPort);
            } else {
                Set<Negative<Network>> ports = destinationPorts.get(ByteBuffer.wrap(id));
                if (ports != null) {
                    ports.add(destinationPort);
                } else {
                    ports = new HashSet<Negative<Network>>();
                    ports.add(destinationPort);
                    destinationPorts.put(ByteBuffer.wrap(id), ports);
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
        destinationPort.addChannel(this);
    }

    public void removeConnection(byte[] id, Negative<Network> destinationPort) {
        rwlock.writeLock().lock();
        try {
            if (id == null) {
                hostPorts.remove(destinationPort);
            } else {
                Set<Negative<Network>> ports = destinationPorts.get(ByteBuffer.wrap(id));
                if (ports != null) {
                    ports.remove(destinationPort);
                }
            }
        } finally {
            rwlock.writeLock().unlock();
        }
        destinationPort.removeChannel(this);
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    @Override
    public void forwardToNegative(KompicsEvent event, int wid) {
        if (destroyed) {
            return;
        }
        Msg<?, ?> msg = (Msg<?, ?>) event;
        se.sics.kompics.network.Header<?> h = msg.getHeader();
        if (h instanceof Header) {
            Header<?> vh = (Header<?>) h;
            byte[] id = vh.getDstId();

            rwlock.readLock().lock();
            try {
                if (destroyed) { // check again in case it changed
                    return;
                }
                if (id == null) {
                    for (Negative<Network> port : hostPorts) {
                        port.doTrigger(event, wid, this);
                    }
                    return;
                } else {
                    Set<Negative<Network>> ports = destinationPorts.get(ByteBuffer.wrap(id));
                    if (ports != null) {
                        for (Negative<Network> port : ports) {
                            port.doTrigger(event, wid, this);
                        }
                        return;
                    } else {
                        log.debug("No Port for id {}", id);
                    }
                    // log.debug("Couldn't find routing Id for event: " + id.toString() + " of type
                    // " +
                    // id.getClass().getSimpleName());
                }
            } finally {
                rwlock.readLock().unlock();
            }
        } else {
            log.debug("Message {} has wrong type of header ({}). Needs virtual node id.", msg, h.getClass());
        }

        this.deadLetterBox.doTrigger(event, wid, this);
    }

    @Override
    public void forwardToPositive(KompicsEvent event, int wid) {
        if (destroyed) {
            return;
        }
        // log.debug("Forwarding Message down: " + event.toString());
        sourcePort.doTrigger(event, wid, this);
    }

    @Override
    public Network getPortType() {
        return sourcePort.getPortType();
    }

    public static class DefaultDeadLetterComponent extends ComponentDefinition {

        Positive<Network> net = requires(Network.class);

        Handler<Msg<?, ?>> msgHandler = new Handler<Msg<?, ?>>() {

            @Override
            public void handle(Msg<?, ?> event) {
                Msg<?, ?> msg = (Msg<?, ?>) event;
                log.warn("Message from " + msg.getHeader().getSource() + " to " + msg.getHeader().getDestination()
                        + " was not delivered! \n    Message: " + msg.toString());

            }

        };

        {
            subscribe(msgHandler, net);
        }
    }
}
