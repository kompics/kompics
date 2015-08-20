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

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.OptionUDT;
import com.barchart.udt.SocketUDT;
import com.google.common.collect.HashMultimap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 *
 * @author lkroll
 */
public class ChannelManager {

    private final ConcurrentMap<InetSocketAddress, SocketChannel> tcpActiveChannels = new ConcurrentHashMap<InetSocketAddress, SocketChannel>();
    private final ConcurrentMap<InetSocketAddress, UdtChannel> udtActiveChannels = new ConcurrentHashMap<InetSocketAddress, UdtChannel>();

    private final HashMultimap<InetSocketAddress, SocketChannel> tcpChannels = HashMultimap.create();
    private final HashMultimap<InetSocketAddress, UdtChannel> udtChannels = HashMultimap.create();

    private final Map<InetSocketAddress, SocketChannel> tcpChannelsByRemote = new HashMap<InetSocketAddress, SocketChannel>();
    private final Map<InetSocketAddress, UdtChannel> udtChannelsByRemote = new HashMap<InetSocketAddress, UdtChannel>();

    private final Map<InetSocketAddress, InetSocketAddress> address4Remote = new HashMap<InetSocketAddress, InetSocketAddress>();

    private final Map<InetSocketAddress, InetSocketAddress> udtBoundPorts = new HashMap<InetSocketAddress, InetSocketAddress>();

    private final Map<InetSocketAddress, ChannelFuture> udtIncompleteChannels = new HashMap<InetSocketAddress, ChannelFuture>();
    private final Map<InetSocketAddress, ChannelFuture> tcpIncompleteChannels = new HashMap<InetSocketAddress, ChannelFuture>();

    private final NettyNetwork component;

    public ChannelManager(NettyNetwork comp) {
        this.component = comp;
    }

    void disambiguate(DisambiguateConnection msg, Channel c) {
        synchronized (this) {
            if (c.isActive()) { // might have been closed by the time we get the lock?

                if (c instanceof SocketChannel) {
                    SocketChannel sc = (SocketChannel) c;
                    address4Remote.put(sc.remoteAddress(), msg.getSource().asSocket());
                    tcpChannels.put(msg.getSource().asSocket(), sc);
                } else if (c instanceof UdtChannel) {
                    UdtChannel uc = (UdtChannel) c;
                    address4Remote.put(uc.remoteAddress(), msg.getSource().asSocket());
                    udtChannels.put(msg.getSource().asSocket(), uc);
                }
                if (!tcpChannels.get(msg.getSource().asSocket()).isEmpty()) { // don't add if we don't have a TCP channel since host is most likely dead
                    udtBoundPorts.put(msg.getSource().asSocket(), new InetSocketAddress(msg.getSource().getIp(), msg.udtPort));
                }
                component.trigger(SendDelayed.event);
            }
        }
    }

    void checkActive(CheckChannelActive msg, Channel c) {
        synchronized (this) {
            if (c instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) c;
                SocketChannel activeC = tcpActiveChannels.get(msg.getSource().asSocket());
                tcpChannels.put(msg.getSource().asSocket(), sc);
                if (!activeC.equals(sc)) {
                    tcpActiveChannels.put(msg.getSource().asSocket(), sc);
                } else {
                    for (SocketChannel channel : tcpChannels.get(msg.getSource().asSocket())) {
                        if (!channel.equals(activeC)) {
                            component.LOG.warn("Preparing to close duplicate TCP channel between {} and {}: local {}, remote {}",
                                    new Object[]{msg.getSource(), msg.getDestination(), channel.localAddress(), channel.remoteAddress()});
                            channel.writeAndFlush(new CloseChannel(component.self, msg.getSource(), Transport.TCP));
                        }
                    }
                }
            } else if (c instanceof UdtChannel) {
                UdtChannel uc = (UdtChannel) c;
                UdtChannel activeC = udtActiveChannels.get(msg.getSource().asSocket());
                udtChannels.put(msg.getSource().asSocket(), uc);
                if (!activeC.equals(uc)) {
                    udtActiveChannels.put(msg.getSource().asSocket(), uc);
                } else {
                    for (UdtChannel channel : udtChannels.get(msg.getSource().asSocket())) {
                        if (!channel.equals(activeC)) {
                            component.LOG.warn("Preparing to close duplicate UDT channel between {} and {}: local {}, remote {}",
                                    new Object[]{msg.getSource(), msg.getDestination(), channel.localAddress(), channel.remoteAddress()});
                            channel.writeAndFlush(new CloseChannel(component.self, msg.getSource(), Transport.UDT));
                        }
                    }
                }
            }
        }
    }

    void flushAndClose(CloseChannel msg, Channel c) {
        synchronized (this) {
            if (c instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) c;
                SocketChannel activeC = tcpActiveChannels.get(msg.getSource().asSocket());
                tcpChannels.put(msg.getSource().asSocket(), sc); // just to make sure
                Set<SocketChannel> channels = tcpChannels.get(msg.getSource().asSocket());
                if (channels.size() < 2) {
                    component.LOG.warn("Can't close TCP channel between {} and {}: local {}, remote {} -- it's the only channel!",
                            new Object[]{msg.getSource(), msg.getDestination(), sc.localAddress(), sc.remoteAddress()});
                    tcpActiveChannels.put(msg.getSource().asSocket(), sc);
                    sc.writeAndFlush(new CheckChannelActive(component.self, msg.getSource(), Transport.TCP));
                } else {
                    if (activeC.equals(sc)) { // pick any channel as active
                        for (SocketChannel channel : channels) {
                            if (!channel.equals(sc)) {
                                tcpActiveChannels.put(msg.getSource().asSocket(), channel);
                                activeC = channel;
                            }
                        }
                    }
                    ChannelFuture f = sc.writeAndFlush(new ChannelClosed(component.self, msg.getSource(), Transport.TCP));
                    f.addListener(ChannelFutureListener.CLOSE);
                    component.LOG.info("Closing duplicate TCP channel between {} and {}: local {}, remote {}",
                            new Object[]{msg.getSource(), msg.getDestination(), sc.localAddress(), sc.remoteAddress()});

                }
            } else if (c instanceof UdtChannel) {
                UdtChannel uc = (UdtChannel) c;
                UdtChannel activeC = udtActiveChannels.get(msg.getSource().asSocket());
                udtChannels.put(msg.getSource().asSocket(), uc); // just to make sure
                Set<UdtChannel> channels = udtChannels.get(msg.getSource().asSocket());
                if (channels.size() < 2) {
                    component.LOG.warn("Can't close UDT channel between {} and {}: local {}, remote {} -- it's the only channel!",
                            new Object[]{msg.getSource(), msg.getDestination(), uc.localAddress(), uc.remoteAddress()});
                    udtActiveChannels.put(msg.getSource().asSocket(), uc);
                    uc.writeAndFlush(new CheckChannelActive(component.self, msg.getSource(), Transport.UDT));
                } else {
                    if (activeC.equals(uc)) { // pick any channel as active
                        for (UdtChannel channel : channels) {
                            if (!channel.equals(uc)) {
                                udtActiveChannels.put(msg.getSource().asSocket(), channel);
                                activeC = channel;
                            }
                        }
                    }
                    ChannelFuture f = uc.writeAndFlush(new ChannelClosed(component.self, msg.getSource(), Transport.UDT));
                    f.addListener(ChannelFutureListener.CLOSE);
                    component.LOG.info("Closing duplicate UDT channel between {} and {}: local {}, remote {}",
                            new Object[]{msg.getSource(), msg.getDestination(), uc.localAddress(), uc.remoteAddress()});

                }
            }
        }
    }

    void checkTCPChannel(Msg msg, SocketChannel c) {
        // Ignore some messages
        if (msg instanceof CheckChannelActive) {
            return;
        }
        if (msg instanceof CloseChannel) {
            return;
        }
        if (msg instanceof ChannelClosed) {
            return;
        }
        if (!c.equals(tcpActiveChannels.get(msg.getSource().asSocket()))) {
            synchronized (this) {
                SocketChannel activeC = tcpActiveChannels.get(msg.getSource().asSocket());
                tcpActiveChannels.put(msg.getSource().asSocket(), c);
                tcpChannels.put(msg.getSource().asSocket(), c);
                if (activeC != null && !activeC.equals(c)) {
                    component.LOG.warn("Duplicate TCP channel between {} and {}: local {}, remote {}",
                            new Object[]{msg.getSource(), msg.getDestination(), c.localAddress(), c.remoteAddress()});

                    SocketChannel minsc = minChannel(tcpChannels.get(msg.getSource().asSocket()));

                    minsc.writeAndFlush(new CheckChannelActive(component.self, msg.getSource(), Transport.TCP));

                }
            }
            component.trigger(SendDelayed.event);
        }
    }

    void checkUDTChannel(Msg msg, UdtChannel c) {
        // Ignore some messages
        if (msg instanceof CheckChannelActive) {
            return;
        }
        if (msg instanceof CloseChannel) {
            return;
        }
        if (msg instanceof ChannelClosed) {
            return;
        }
        if (!c.equals(udtActiveChannels.get(msg.getSource().asSocket()))) {
            synchronized (this) {
                UdtChannel activeC = udtActiveChannels.get(msg.getSource().asSocket());

                udtActiveChannels.put(msg.getSource().asSocket(), c);
                udtChannels.put(msg.getSource().asSocket(), c);
                if (activeC != null && !activeC.equals(c)) {
                    component.LOG.warn("Duplicate TCP channel between {} and {}: local {}, remote {}",
                            new Object[]{msg.getSource(), msg.getDestination(), c.localAddress(), c.remoteAddress()});

                    UdtChannel minsc = minChannel(udtChannels.get(msg.getSource().asSocket()));

                    minsc.writeAndFlush(new CheckChannelActive(component.self, msg.getSource(), Transport.UDT));

                }
            }
            component.trigger(SendDelayed.event);
        }
    }

    private <C extends Channel> C minChannel(Set<C> channels) {
        C min = null;
        for (C channel : channels) {
            if ((min == null)) {
                min = channel;
            } else if (channel2Id(channel) < channel2Id(min)) {
                min = channel;
            }
        }
        return min;
    }

    private int channel2Id(Channel c) {
        if (c instanceof SocketChannel) {
            return channel2Id((SocketChannel) c);
        }
        if (c instanceof UdtChannel) {
            return channel2Id((UdtChannel) c);
        }
        throw new NotImplementedException();
    }

    private int channel2Id(SocketChannel c) {
        return c.localAddress().getPort() + c.remoteAddress().getPort();
    }

    private int channel2Id(UdtChannel c) {
        return c.localAddress().getPort() + c.remoteAddress().getPort();
    }

    SocketChannel getTCPChannel(Address destination) {
        return tcpActiveChannels.get(destination.asSocket());
    }

    UdtChannel getUDTChannel(Address destination) {
        return udtActiveChannels.get(destination.asSocket());
    }

    SocketChannel createTCPChannel(final Address destination, final Bootstrap bootstrapTCPClient) {
        synchronized (this) {
            SocketChannel c = tcpActiveChannels.get(destination.asSocket()); // check if there's already one by now
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
                    synchronized (ChannelManager.this) {
                        tcpIncompleteChannels.remove(destination.asSocket());
                        if (future.isSuccess()) {
                            SocketChannel sc = (SocketChannel) future.channel();
                            tcpActiveChannels.put(destination.asSocket(), sc);
                            tcpChannels.put(destination.asSocket(), sc);
                            tcpChannelsByRemote.put(sc.remoteAddress(), sc);
                            address4Remote.put(sc.remoteAddress(), destination.asSocket());
                            component.trigger(SendDelayed.event);
                            component.LOG.trace("New TCP channel to {} was created!.", destination.asSocket());
                        } else {
                            component.LOG.error("Error while trying to connect to {}! Error was {}", destination, future.cause());
                            component.trigger(new DropDelayed(destination.asSocket(), Transport.TCP));
                        }
                    }
                }
            });

        }
        return null;
    }

    UdtChannel createUDTChannel(final Address destination, final Bootstrap bootstrapUDTClient) {
        synchronized (this) {
            UdtChannel c = udtActiveChannels.get(destination.asSocket());
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
                DisambiguateConnection r = new DisambiguateConnection(component.self, new NettyAddress(destination), Transport.TCP, component.boundUDTPort, true);
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
                    synchronized (ChannelManager.this) {
                        udtIncompleteChannels.remove(destination.asSocket());
                        if (future.isSuccess()) {
                            UdtChannel sc = (UdtChannel) future.channel();
                            udtActiveChannels.put(destination.asSocket(), sc);
                            udtChannels.put(destination.asSocket(), sc);
                            udtChannelsByRemote.put(sc.remoteAddress(), sc);
                            address4Remote.put(sc.remoteAddress(), destination.asSocket());
                            component.trigger(SendDelayed.event);
                            SocketUDT socket = NioUdtProvider.socketUDT(sc);
//                            if (component.udtBufferSizes > 0) {
//                                socket.setSendBufferSize(component.udtBufferSizes);
//                                socket.setReceiveBufferSize(component.udtBufferSizes);
//                            }
                            if (component.udtMSS > 0) {
                                socket.setOption(OptionUDT.Maximum_Transfer_Unit, component.udtMSS);
                            }
                            component.LOG.debug("New UDT channel to {} was created! Properties: \n {} \n {}",
                                    new Object[]{destination.asSocket(), socket.toStringOptions(), socket.toStringMonitor()});
                        } else {
                            component.LOG.error("Error while trying to connect to {}! Error was {}", destination, future.cause());
                            component.trigger(new DropDelayed(destination.asSocket(), Transport.UDT));
                        }
                    }
                }
            });

        }
        return null;
    }

    void channelInactive(ChannelHandlerContext ctx, Transport protocol) {

        synchronized (this) {
            SocketAddress addr = ctx.channel().remoteAddress();
            Channel c = ctx.channel();

            if (addr instanceof InetSocketAddress) {
                InetSocketAddress remoteAddress = (InetSocketAddress) addr;
                InetSocketAddress realAddress = address4Remote.remove(remoteAddress);
                switch (protocol) {
                    case TCP:
                        if (realAddress != null) {
                            tcpChannels.remove(realAddress, c);
                            SocketChannel curChannel = tcpActiveChannels.get(realAddress);
                            if ((curChannel != null) && curChannel.equals(c)) {
                                SocketChannel newActive = minChannel(tcpChannels.get(realAddress));
                                if (newActive != null) {
                                    tcpActiveChannels.put(realAddress, newActive);
                                } else {
                                    tcpActiveChannels.remove(realAddress);
                                }
                            }
                            tcpChannelsByRemote.remove(remoteAddress);
                            component.LOG.debug("TCP Channel {} ({}) closed: {}", new Object[]{realAddress, remoteAddress, c});
                            printStuff();
                            if (tcpChannels.get(realAddress).isEmpty()) {
                                component.LOG.info("Last TCP Channel to {} dropped. "
                                        + "Also dropping all UDT channels under "
                                        + "the assumption that the host is dead.", realAddress);
                                UdtChannel uac = udtActiveChannels.remove(realAddress);
                                for (UdtChannel uc : udtChannels.get(realAddress)) {
                                    InetSocketAddress udtRealAddr = address4Remote.remove(uc.remoteAddress());
                                    udtChannelsByRemote.remove(uc.remoteAddress());
                                    uc.close();
                                    component.LOG.debug("   UDT Channel {} ({}) closed.", udtRealAddr, uc.remoteAddress());
                                }
                                udtChannels.removeAll(realAddress);

                                udtBoundPorts.remove(realAddress);
                            } else {
                                component.LOG.trace("There are still {} TCP channel(s) remaining: [", tcpChannels.get(realAddress).size(), realAddress);
                                for (SocketChannel sc : tcpChannels.get(realAddress)) {
                                    component.LOG.trace("TCP channel: {}", sc);
                                }
                                component.LOG.trace("]. Not closing UDT channels for {}", tcpChannels.get(realAddress).size(), realAddress);

                            }
                            printStuff();
                        } else {
                            tcpChannelsByRemote.remove(remoteAddress);
                            component.LOG.debug("TCP Channel {} was already closed.", remoteAddress);
                            printStuff();
                        }
                        return;
                    case UDT:
                        if (realAddress != null) {
                            udtChannels.remove(realAddress, c);
                            UdtChannel curChannel = udtActiveChannels.get(realAddress);
                            if ((curChannel != null) && curChannel.equals(c)) {
                                UdtChannel newActive = minChannel(udtChannels.get(realAddress));
                                if (newActive != null) {
                                    udtActiveChannels.put(realAddress, newActive);
                                } else {
                                    udtActiveChannels.remove(realAddress);
                                }
                            }
                            udtChannelsByRemote.remove(remoteAddress);
                            component.LOG.debug("UDT Channel {} ({}) closed.", realAddress, remoteAddress);
                            printStuff();
                        } else {
                            udtChannelsByRemote.remove(remoteAddress);
                            component.LOG.debug("UDT Channel {} was already closed.\n", remoteAddress);
                            printStuff();
                        }
                        return;
                    default:
                        component.LOG.error("Was supposed to close channel {}, but don't know transport {}", remoteAddress, protocol);
                }
            }
        }

    }

    private void printStuff() {
        List<Map.Entry<Address, InetSocketAddress>> udtstuff = new LinkedList(udtBoundPorts.entrySet());
        StringBuilder sb = new StringBuilder();
        sb.append("ChannelManagerState:\n");
        sb.append("udtPortMap{\n");
        for (Entry<Address, InetSocketAddress> e : udtstuff) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        sb.append("tcpActiveChannels{\n");
        for (Entry<InetSocketAddress, SocketChannel> e : tcpActiveChannels.entrySet()) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        sb.append("udtActiveChannels{\n");
        for (Entry<InetSocketAddress, UdtChannel> e : udtActiveChannels.entrySet()) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        sb.append("tcpChannels{\n");
        for (Entry<InetSocketAddress, SocketChannel> e : tcpChannels.entries()) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        sb.append("udtChannels{\n");
        for (Entry<InetSocketAddress, UdtChannel> e : udtChannels.entries()) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        sb.append("tcpChannelsByRemote{\n");
        for (Entry<InetSocketAddress, SocketChannel> e : tcpChannelsByRemote.entrySet()) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        sb.append("udtChannelsByRemote{\n");
        for (Entry<InetSocketAddress, UdtChannel> e : udtChannelsByRemote.entrySet()) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        sb.append("address4Remote{\n");
        for (Entry<InetSocketAddress, InetSocketAddress> e : address4Remote.entrySet()) {
            sb.append(e.getKey());
            sb.append(" -> ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        sb.append("}\n");
        component.LOG.trace("{}", sb.toString());
    }

    void monitor() {
        component.LOG.debug("Monitoring UDT channels:");
        for (UdtChannel c : udtChannelsByRemote.values()) {
            SocketUDT socket = NioUdtProvider.socketUDT(c);
            if (socket != null) {
                component.LOG.debug("UDT Stats: \n {} \n {}",
                        new Object[]{socket.toStringMonitor(), socket.toStringOptions()});
                try {
                    socket.updateMonitor(true); // reset statistics
                } catch (ExceptionUDT ex) {
                    component.LOG.warn("Couldn't reset UDT monitoring stats.");
                }
            }
        }
    }

    void clearConnections() {
        // clear these early to try avoid sending messages on them while closing
        tcpActiveChannels.clear();
        udtActiveChannels.clear();

        List<ChannelFuture> futures = new LinkedList<ChannelFuture>();

        synchronized (this) {
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

            tcpActiveChannels.clear(); // clear them again just to be sure
            tcpChannels.clear();
            tcpChannelsByRemote.clear();

            for (UdtChannel c : udtChannelsByRemote.values()) {
                futures.add(c.close());
            }

            udtActiveChannels.clear();
            udtChannels.clear();
            udtChannelsByRemote.clear();

            udtBoundPorts.clear();

            udtIncompleteChannels.clear();
            tcpIncompleteChannels.clear();
        }
        for (ChannelFuture cf : futures) {
            cf.syncUninterruptibly();
        }
    }

    void addLocalSocket(UdtChannel channel) {
        synchronized (this) {
            udtChannelsByRemote.put(channel.remoteAddress(), channel);
        }
    }

    void addLocalSocket(SocketChannel channel) {
        synchronized (this) {
            tcpChannelsByRemote.put(channel.remoteAddress(), channel);
        }
    }
}
