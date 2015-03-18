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
package se.sics.kompics.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.udt.UdtChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;

/**
 *
 * @author lkroll
 */
public class ChannelManager {

    private final ConcurrentMap<InetSocketAddress, SocketChannel> tcpChannels = new ConcurrentHashMap<InetSocketAddress, SocketChannel>();
    private final ConcurrentMap<InetSocketAddress, UdtChannel> udtChannels = new ConcurrentHashMap<InetSocketAddress, UdtChannel>();

    private final Map<InetSocketAddress, SocketChannel> tcpChannelsByRemote = new HashMap<InetSocketAddress, SocketChannel>();
    private final Map<InetSocketAddress, UdtChannel> udtChannelsByRemote = new HashMap<InetSocketAddress, UdtChannel>();

    private final Map<InetSocketAddress, InetSocketAddress> address4Remote = new HashMap<InetSocketAddress, InetSocketAddress>();

    private final Map<InetSocketAddress, InetSocketAddress> udtBoundPorts = new HashMap<InetSocketAddress, InetSocketAddress>();

    private final Map<InetSocketAddress, ChannelFuture> udtIncompleteChannels = new HashMap<InetSocketAddress, ChannelFuture>();
    private final Map<InetSocketAddress, ChannelFuture> tcpIncompleteChannels = new HashMap<InetSocketAddress, ChannelFuture>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final NettyNetwork component;

    public ChannelManager(NettyNetwork comp) {
        this.component = comp;
    }

    void disambiguate(DisambiguateConnection msg, Channel c) {
        lock.writeLock().lock();
        try {
            udtBoundPorts.put(msg.src.asSocket(), new InetSocketAddress(msg.src.getIp(), msg.udtPort));

            if (c instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) c;
                address4Remote.put(sc.remoteAddress(), msg.src.asSocket());
                if (tcpChannels.containsKey(msg.src.asSocket())) {
                    SocketChannel oldsc = tcpChannels.get(msg.src.asSocket());
                    if (!oldsc.equals(sc)) {
                        component.LOG.warn("Duplicate TCP channel between {} and {}: local {}, remote {}",
                                new Object[]{msg.src, msg.dst, sc.localAddress(), sc.remoteAddress()});
                        //oldsc.close();
                        //TODO agree on channel to use and close the other
                    }
                }
                tcpChannels.put(msg.src.asSocket(), sc); // always use newer channel
                component.trigger(SendDelayed.event);
            } else if (c instanceof UdtChannel) {
                UdtChannel uc = (UdtChannel) c;
                address4Remote.put(uc.remoteAddress(), msg.src.asSocket());
                if (udtChannels.containsKey(msg.src.asSocket())) {
                    UdtChannel olduc = udtChannels.get(msg.src.asSocket());
                    if (!olduc.equals(uc)) {
                        component.LOG.warn("Duplicate UDT channel between {} and {}: local {}, remote {}",
                                new Object[]{msg.src, msg.dst, uc.localAddress(), uc.remoteAddress()});
                        //olduc.close();
                        //TODO agree on channel to use and close the other
                    }
                }
                udtChannels.put(msg.src.asSocket(), uc); // always use newer channel
                component.trigger(SendDelayed.event);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    void checkTCPChannel(Msg msg, SocketChannel c) {
        if (!c.equals(tcpChannels.get(msg.getSource().asSocket()))) {
            tcpChannels.put(msg.getSource().asSocket(), c);
            component.trigger(SendDelayed.event);
        }
    }
    
    void checkUDTChannel(Msg msg, UdtChannel c) {
        if (!c.equals(udtChannels.get(msg.getSource().asSocket()))) {
            udtChannels.put(msg.getSource().asSocket(), c);
            component.trigger(SendDelayed.event);
        }
    }

    SocketChannel getTCPChannel(Address destination) {
        return tcpChannels.get(destination.asSocket());
    }

    UdtChannel getUDTChannel(Address destination) {
        return udtChannels.get(destination.asSocket());
    }

    SocketChannel createTCPChannel(final Address destination, final Bootstrap bootstrapTCPClient) {
        lock.writeLock().lock();
        try {
            SocketChannel c = tcpChannels.get(destination.asSocket()); // check if there's already one by now
            if (c != null) {
                return c;
            }
            ChannelFuture f = tcpIncompleteChannels.get(destination.asSocket()); // check if it's already beging created
            if (f != null) {
                component.LOG.trace("TCP channel to {} is already being created.", destination.asSocket());
                return null; // already establishing connection but not done, yet
            }
            component.LOG.trace("Creating new TCP channel to {}.", destination.asSocket());
            f = bootstrapTCPClient.connect(destination.asSocket());
            tcpIncompleteChannels.put(destination.asSocket(), f);
            f.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    lock.writeLock().lock();
                    try {
                        tcpIncompleteChannels.remove(destination.asSocket());
                        if (future.isSuccess()) {
                            SocketChannel sc = (SocketChannel) future.channel();
                            tcpChannels.put(destination.asSocket(), sc);
                            tcpChannelsByRemote.put(sc.remoteAddress(), sc);
                            address4Remote.put(sc.remoteAddress(), destination.asSocket());
                            component.trigger(SendDelayed.event);
                            component.LOG.trace("New TCP channel to {} was created!.", destination.asSocket());
                        } else {
                            component.LOG.error("Error while trying to connect to {}! Error was {}", destination, future.cause());
                            component.trigger(new DropDelayed(destination.asSocket(), Transport.TCP));
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            });

        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    UdtChannel createUDTChannel(final Address destination, final Bootstrap bootstrapUDTClient) {
        lock.writeLock().lock();
        try {
            UdtChannel c = udtChannels.get(destination.asSocket());
            if (c != null) {
                return c;
            }
            ChannelFuture f = udtIncompleteChannels.get(destination.asSocket());
            if (f != null) {
                component.LOG.trace("UDT channel to {} is already being created.", destination.asSocket());
                return null; // already establishing connection but not done, yet
            }
            InetSocketAddress isa = udtBoundPorts.get(destination.asSocket());
            if (isa == null) { // We have to ask for the UDT port first, since it's random
                DisambiguateConnection r = new DisambiguateConnection(component.self, destination.hostAddress(), Transport.TCP, component.boundUDTPort, true);
                component.trigger(r);
                component.LOG.trace("Need to find UDT port at {} before creating channel.", destination.asSocket());
                return null;
            }
            component.LOG.trace("Creating new UDT channel to {}.", destination.asSocket());
            f = bootstrapUDTClient.connect(isa);
            udtIncompleteChannels.put(destination.asSocket(), f);
            f.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    lock.writeLock().lock();
                    try {
                        udtIncompleteChannels.remove(destination.asSocket());
                        if (future.isSuccess()) {
                            UdtChannel sc = (UdtChannel) future.channel();
                            udtChannels.put(destination.asSocket(), sc);
                            udtChannelsByRemote.put(sc.remoteAddress(), sc);
                            address4Remote.put(sc.remoteAddress(), destination.asSocket());
                            component.trigger(SendDelayed.event);
                            component.LOG.trace("New UDT channel to {} was created!.", destination.asSocket());
                        } else {
                            component.LOG.error("Error while trying to connect to {}! Error was {}", destination, future.cause());
                            component.trigger(new DropDelayed(destination.asSocket(), Transport.UDT));
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            });

        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    void channelInactive(ChannelHandlerContext ctx, Transport protocol) {

        lock.writeLock().lock();
        try {
            SocketAddress addr = ctx.channel().remoteAddress();

            if (addr instanceof InetSocketAddress) {
                InetSocketAddress remoteAddress = (InetSocketAddress) addr;
                InetSocketAddress realAddress = address4Remote.remove(remoteAddress);
                switch (protocol) {
                    case TCP:
                        if (realAddress != null) {
                            SocketChannel curChannel = tcpChannels.get(realAddress);
                            if ((curChannel != null) && curChannel.equals(ctx.channel())) {
                                tcpChannels.remove(realAddress);
                            }
                            tcpChannelsByRemote.remove(remoteAddress);
                            component.LOG.debug("TCP Channel {} ({}) closed.", realAddress, remoteAddress);
                        } else {
                            component.LOG.debug("TCP Channel {} was already closed.", remoteAddress);
                        }
                        return;
                    case UDT:
                        if (realAddress != null) {
                            UdtChannel curChannel = udtChannels.get(realAddress);
                            if ((curChannel != null) && curChannel.equals(ctx.channel())) {
                                udtChannels.remove(realAddress);
                            }
                            udtChannelsByRemote.remove(remoteAddress);
                            component.LOG.debug("UDT Channel {} ({}) closed.", realAddress, remoteAddress);
                        } else {
                            component.LOG.debug("UDT Channel {} was already closed.", remoteAddress);
                        }
                        return;
                    default:
                        component.LOG.error("Was supposed to close channel {}, but don't know transport {}", remoteAddress, protocol);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    private void printStuff() {
        List<Map.Entry<Address, InetSocketAddress>> udtstuff = new LinkedList(udtBoundPorts.entrySet());
        StringBuilder sb = new StringBuilder();
        sb.append("udtPortMap{\n");
        for (Map.Entry<Address, InetSocketAddress> e : udtstuff) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        component.LOG.trace("{}", sb.toString());
    }

    void clearConnections() {
        // clear these early to try avoid sending messages on them while closing
        tcpChannels.clear();
        udtChannels.clear();

        List<ChannelFuture> futures = new LinkedList<ChannelFuture>();

        lock.writeLock().lock();
        try {
            component.LOG.info("Closing all connections...");
            for (ChannelFuture f : udtIncompleteChannels.values()) {
                f.cancel(false);
            }
            for (ChannelFuture f : tcpIncompleteChannels.values()) {
                f.cancel(false);
            }

            for (SocketChannel c : tcpChannelsByRemote.values()) {
                futures.add(c.close());
            }

            tcpChannels.clear(); // clear them again just to be sure
            tcpChannelsByRemote.clear();

            for (UdtChannel c : udtChannelsByRemote.values()) {
                futures.add(c.close());
            }

            udtChannels.clear();
            tcpChannelsByRemote.clear();

            udtBoundPorts.clear();

            udtIncompleteChannels.clear();
            tcpIncompleteChannels.clear();
        } finally {
            lock.writeLock().unlock();
        }
        for (ChannelFuture cf : futures) {
            cf.syncUninterruptibly();
        }
    }

    void addLocalSocket(UdtChannel channel) {
        lock.writeLock().lock();
        try {
            udtChannelsByRemote.put(channel.remoteAddress(), channel);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void addLocalSocket(SocketChannel channel) {
        lock.writeLock().lock();
        try {
            tcpChannelsByRemote.put(channel.remoteAddress(), channel);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
