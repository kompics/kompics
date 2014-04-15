/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import se.sics.kompics.ChannelCore;
import se.sics.kompics.ChannelFilter;
import se.sics.kompics.ComponentCore;
import se.sics.kompics.Event;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.PortCore;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class VirtualNetworkChannel implements ChannelCore<Network> {

    private static Logger log = Logger.getLogger(VirtualNetworkChannel.class);
    private boolean destroyed = false;
    private PortCore<Network> sourcePort;
    // Use HashMap for now and switch to a more efficient 
    // datastructure if necessary
    private Map<ByteBuffer, Set<Negative<Network>>> destinationPorts = new HashMap<ByteBuffer, Set<Negative<Network>>>();
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private Negative<Network> decoyPort;
    private Negative<Network> deadLetterBox;
    private Set<Negative<Network>> hostPorts = new HashSet<Negative<Network>>();

    private VirtualNetworkChannel(Positive<Network> sourcePort, Negative<Network> deadLetterBox) {
        this.sourcePort = (PortCore<Network>) sourcePort;
        this.deadLetterBox = deadLetterBox;
        this.decoyPort = new DecoyPort();
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort) {
        return connect(sourcePort, new DefaultDeadLetterBox());
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort, Negative<Network> deadLetterBox) {
        VirtualNetworkChannel vnc = new VirtualNetworkChannel(sourcePort, deadLetterBox);
        sourcePort.addChannel(vnc);
        deadLetterBox.addChannel(vnc);

        return vnc;
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort, ChannelFilter<?, ?> filter) {
        return connect(sourcePort, new DefaultDeadLetterBox(), filter);
    }

    public static VirtualNetworkChannel connect(Positive<Network> sourcePort, Negative<Network> deadLetterBox, ChannelFilter<?, ?> filter) {
        VirtualNetworkChannel vnc = new VirtualNetworkChannel(sourcePort, deadLetterBox);
        sourcePort.addChannel(vnc, filter);
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
        destinationPort.removeChannelTo(sourcePort);
    }

    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    @Override
    public void destroy() {
        this.destroyed = true;
    }

    @Override
    public Positive<Network> getPositivePort() {
        return this.sourcePort;
    }

    @Override
    public Negative<Network> getNegativePort() {
        return this.decoyPort;
    }

    @Override
    public void forwardToNegative(Event event, int wid) {
        Message msg = (Message) event;
        byte[] id = msg.getDestination().getId();
        rwlock.readLock().lock();
        try {
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
                    log.debug("No Port for id " + id);
                }
                //log.debug("Couldn't find routing Id for event: " + id.toString() + " of type " + id.getClass().getSimpleName());
            }
        } finally {
            rwlock.readLock().unlock();
        }

        this.deadLetterBox.doTrigger(event, wid, this);
    }

    @Override
    public void forwardToPositive(Event event, int wid) {
        //log.debug("Forwarding Message down: " + event.toString());
        sourcePort.doTrigger(event, wid, this);
    }

    @Override
    public void hold() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unplug() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void plug() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Network getPortType() {
        return sourcePort.getPortType();
    }

    private class DecoyPort extends PortCore<Network> {

        public DecoyPort() {
            this.isPositive = true;
            this.portType = PortType.getPortType(Network.class);
            this.owner = null;
            this.isControlPort = false;
        }

        @Override
        public void doTrigger(Event event, int wid, ChannelCore<?> channel) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void doTrigger(Event event, int wid, ComponentCore component) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PortCore<Network> getPair() {
            return this;
        }

        @Override
        public void setPair(PortCore<Network> port) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <E extends Event> void doSubscribe(Handler<E> handler) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addChannel(ChannelCore<Network> channel) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addChannel(ChannelCore<Network> channel, ChannelFilter<?, ?> filter) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeChannelTo(PortCore<Network> remotePort) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void enqueue(Event event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static class DefaultDeadLetterBox implements Negative<Network> {

        @Override
        public Network getPortType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void doTrigger(Event event, int wid, ChannelCore<?> channel) {
            if (event instanceof Message) {
                Message msg = (Message) event;
                log.warn("Message from " + msg.getSource() + " to " + msg.getDestination() + " was not delivered! \n    Message: " + msg.toString());
                return;
            }
            log.warn("Unkown event of type " + event.getClass().getCanonicalName());
        }

        @Override
        public void doTrigger(Event event, int wid, ComponentCore component) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ComponentCore getOwner() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public PortCore<Network> getPair() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPair(PortCore<Network> port) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public <E extends Event> void doSubscribe(Handler<E> handler) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void addChannel(ChannelCore<Network> channel) {
            // Do nothing
        }

        @Override
        public void addChannel(ChannelCore<Network> channel, ChannelFilter<?, ?> filter) {
            // Do nothing
        }

        @Override
        public void removeChannelTo(PortCore<Network> remotePort) {
            // Do nothing
        }

        @Override
        public void enqueue(Event event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
