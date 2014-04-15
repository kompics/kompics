/* 
 * This file is part of the CaracalDB distributed storage system.
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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.udt.UdtChannel;
import io.netty.channel.udt.nio.NioUdtProvider;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkControl;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.NetworkSessionOpened;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class NettyNetwork extends ComponentDefinition {

    public static final Logger LOG = LoggerFactory.getLogger(NettyNetwork.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    static final int RECV_BUFFER_SIZE = 65536;
    static final int SEND_BUFFER_SIZE = 65536;
    static final int INITIAL_BUFFER_SIZE = 512;
    // Ports
    Negative<Network> net = provides(Network.class);
    Negative<NetworkControl> netC = provides(NetworkControl.class);
    // Network
    private ServerBootstrap bootstrapTCP;
    private Bootstrap bootstrapTCPClient;
    private Bootstrap bootstrapUDP;
    private ServerBootstrap bootstrapUDT;
    private Bootstrap bootstrapUDTClient;
    private final ConcurrentMap<InetSocketAddress, SocketChannel> tcpChannels = new ConcurrentHashMap<InetSocketAddress, SocketChannel>();
    private final ConcurrentMap<InetSocketAddress, DatagramChannel> udpChannels = new ConcurrentHashMap<InetSocketAddress, DatagramChannel>();
    private final ConcurrentMap<InetSocketAddress, UdtChannel> udtChannels = new ConcurrentHashMap<InetSocketAddress, UdtChannel>();
    private final ConcurrentMap<Integer, Integer> udtPortMap = new ConcurrentSkipListMap<Integer, Integer>();
    private final ConcurrentLinkedQueue<Message> delayedMessages = new ConcurrentLinkedQueue<Message>();
    // Info
    final Address self;
    private final int boundPort;
    volatile int boundUDTPort = -1; // Unbound

    public NettyNetwork(NettyInit init) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        self = init.self;
        boundPort = self.getPort();

        // Prepare Bootstraps
        bootstrapTCPClient = new Bootstrap();
        bootstrapTCPClient.group(new NioEventLoopGroup()).channel(NioSocketChannel.class)
                .handler(new NettyInitializer<SocketChannel>(new StreamHandler(this, Transport.TCP)))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .option(ChannelOption.SO_REUSEADDR, true);
        bootstrapUDTClient = new Bootstrap();
        NioEventLoopGroup groupUDT = new NioEventLoopGroup(1, Executors.defaultThreadFactory(),
                NioUdtProvider.BYTE_PROVIDER);
        bootstrapUDTClient.group(groupUDT).channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                .handler(new NettyInitializer<SocketChannel>(new StreamHandler(this, Transport.UDT)))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .option(ChannelOption.SO_REUSEADDR, true);

        subscribe(startHandler, control);
        subscribe(stopHandler, control);
        subscribe(msgHandler, net);
    }

    Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            // Prepare listening sockets
            bindPort(self.getIp(), self.getPort(), Transport.TCP);
            bindPort(self.getIp(), self.getPort(), Transport.UDT);
            bindPort(self.getIp(), self.getPort(), Transport.UDP);
        }

    };

    Handler<Stop> stopHandler = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            LOG.info("Closing all connections...");
            List<ChannelFuture> futures = new LinkedList<ChannelFuture>();
            for (SocketChannel c : tcpChannels.values()) {
                futures.add(c.close());
            }
            tcpChannels.clear();
            for (DatagramChannel c : udpChannels.values()) {
                futures.add(c.close());
            }
            udpChannels.clear();
            for (UdtChannel c : udtChannels.values()) {
                futures.add(c.close());
            }
            udtChannels.clear();

            for (ChannelFuture cf : futures) {
                cf.syncUninterruptibly();
            }
            LOG.info("Shutting down handler groups...");
            bootstrapUDTClient.group().shutdownGracefully();
            bootstrapTCP.childGroup().shutdownGracefully();
            bootstrapTCP.group().shutdownGracefully();
            bootstrapTCPClient.group().shutdownGracefully();
            bootstrapUDP.group().shutdownGracefully();
            bootstrapUDT.childGroup().shutdownGracefully();
            bootstrapUDT.group().shutdownGracefully();
            LOG.info("Netty shutdown complete.");
        }
    };

    Handler<Message> msgHandler = new Handler<Message>() {

        @Override
        public void handle(Message event) {
            if (event.getDestination().sameHostAs(self)) {
                LOG.trace("Delivering message {} locally.", event);
                trigger(event, net);
                return;
            }
            sendMessage(event);
        }
    };

    private boolean bindPort(InetAddress addr, int port, Transport protocol) {
        switch (protocol) {
            case TCP:
                return bindTcpPort(addr, port);
            case UDP:
                return bindUdpPort(addr, port);
            case UDT:
                return bindUdtPort(addr); // bind to random port instead (bad netty UDT implementation -.-)
            default:
                throw new Error("Unknown Transport type");
        }
    }

    private boolean bindUdpPort(final InetAddress addr, final int port) {

        EventLoopGroup group = new NioEventLoopGroup();
        bootstrapUDP = new Bootstrap();
        bootstrapUDP.group(group).channel(NioDatagramChannel.class)
                .handler(new DatagramHandler(this, Transport.UDP));

        bootstrapUDP.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(1500, 1500, RECV_BUFFER_SIZE));
        bootstrapUDP.option(ChannelOption.SO_RCVBUF, RECV_BUFFER_SIZE);
        bootstrapUDP.option(ChannelOption.SO_SNDBUF, SEND_BUFFER_SIZE);
        // bootstrap.setOption("trafficClass", trafficClass);
        // bootstrap.setOption("soTimeout", soTimeout);
        // bootstrap.setOption("broadcast", broadcast);
        bootstrapUDP.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS);
        bootstrapUDP.option(ChannelOption.SO_REUSEADDR, true);

        try {
            InetSocketAddress iAddr = new InetSocketAddress(addr, port);
            DatagramChannel c = (DatagramChannel) bootstrapUDP.bind(iAddr).sync().channel();

            addLocalSocket(iAddr, c);
            LOG.info("Successfully bound to ip:port {}:{}", addr, port);
        } catch (InterruptedException e) {
            LOG.error("Problem when trying to bind to {}:{}", addr.getHostAddress(), port);
            return false;
        }

        return true;
    }

    private boolean bindTcpPort(InetAddress addr, int port) {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        TCPServerHandler handler = new TCPServerHandler(this);
        bootstrapTCP = new ServerBootstrap();
        bootstrapTCP.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler((new NettyInitializer<SocketChannel>(handler)))
                .option(ChannelOption.SO_REUSEADDR, true);

        try {
            bootstrapTCP.bind(new InetSocketAddress(addr, port)).sync();

            LOG.info("Successfully bound to ip:port {}:{}", addr, port);
        } catch (InterruptedException e) {
            LOG.error("Problem when trying to bind to {}:{}", addr, port);
            return false;
        }

        //InetSocketAddress iAddr = new InetSocketAddress(addr, port);
        return true;
    }

    private boolean bindUdtPort(InetAddress addr) {

        ThreadFactory bossFactory = Executors.defaultThreadFactory();
        ThreadFactory workerFactory = Executors.defaultThreadFactory();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1, bossFactory,
                NioUdtProvider.BYTE_PROVIDER);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1, workerFactory,
                NioUdtProvider.BYTE_PROVIDER);
        UDTServerHandler handler = new UDTServerHandler(this);
        bootstrapUDT = new ServerBootstrap();
        bootstrapUDT.group(bossGroup, workerGroup).channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                .childHandler(new NettyInitializer<UdtChannel>(handler))
                .option(ChannelOption.SO_REUSEADDR, true);

        try {
            Channel c = bootstrapUDT.bind(addr, 0).sync().channel();
            InetSocketAddress localAddress = (InetSocketAddress) c.localAddress(); // Should work
            boundUDTPort = localAddress.getPort();

            LOG.info("Successfully bound UDT to ip:port {}:{}", addr, boundUDTPort);
        } catch (InterruptedException e) {
            LOG.error("Problem when trying to bind UDT to {}", addr);
            return false;
        }

        return true;
    }

    protected void networkException(NetworkException networkException) {
        trigger(networkException, netC);
    }

    protected void deliverMessage(Message message) {
        if (message instanceof DisambiguateConnection.Req) {
            DisambiguateConnection.Req msg = (DisambiguateConnection.Req) message;
            udtPortMap.put(msg.getSource().getPort(), msg.udtPort);
            trigger(new DisambiguateConnection.Resp(self, msg.getSource(), msg.getProtocol(), msg.localPort, boundPort, boundUDTPort), net.getPair());
            sendDelayedMessages();
        }
        if (message instanceof DisambiguateConnection.Resp) {
            DisambiguateConnection.Resp msg = (DisambiguateConnection.Resp) message;
            udtPortMap.put(msg.getSource().getPort(), msg.udtPort);
            InetSocketAddress oldAddr = new InetSocketAddress(msg.getSource().getIp(), msg.localPort);
            InetSocketAddress newAddr = new InetSocketAddress(msg.getSource().getIp(), msg.boundPort);
            switch (msg.getProtocol()) {
                case TCP:
                    SocketChannel c = tcpChannels.remove(oldAddr);
                    if (c != null) { // Shouldn't be...but you never know
                        tcpChannels.put(newAddr, c);
                    }
                    break;
                case UDT:
                    UdtChannel c2 = udtChannels.remove(oldAddr);
                    if (c2 != null) { // Shouldn't be...but you never know
                        udtChannels.put(newAddr, c2);
                    }
                    break;
                default:
                    throw new Error("Unknown Transport type");
            }
            sendDelayedMessages();
        }
        LOG.debug(
                "Delivering message {} from {} to {} protocol {}",
                new Object[]{message.toString(), message.getSource(),
                    message.getDestination(), message.getProtocol()});
        trigger(message, net);
    }

    private void sendMessage(Message message) {
        switch (message.getProtocol()) {
            case TCP:
                sendTcpMessage(message);
                return;
            case UDT:
                sendUdtMessage(message);
                return;
            case UDP:
                sendUdpMessage(message);
                return;
            default:
                throw new Error("Unknown Transport type");
        }
    }

    private void sendUdpMessage(Message message) {
        InetSocketAddress src = addressToSocket(message.getSource());
        InetSocketAddress dst = addressToSocket(message.getDestination());
        Channel c = udpChannels.get(src);
        //ByteBuf buf = Unpooled.buffer(INITIAL_BUFFER_SIZE, SEND_BUFFER_SIZE);
        ByteBuf buf = c.alloc().buffer(INITIAL_BUFFER_SIZE, SEND_BUFFER_SIZE);
        try {
            Serializers.toBinary(message, buf);
            DatagramPacket pack = new DatagramPacket(buf, dst);
            LOG.debug("Sending Datagram message {} from {} to {}", new Object[]{message, src, dst});
            c.writeAndFlush(pack);
        } catch (Exception e) { // serialization might fail horribly with size bounded buff
            LOG.warn("Could not send Datagram message {}, error was: {}", message, e);
        }
    }

    private void sendUdtMessage(Message message) {
        Channel c = getUDTChannel(message.getDestination());
        if (c == null) {
            LOG.info("Couldn't get UDT channel for {}. Delaying message for now.", message.getDestination());
            delayedMessages.offer(message);
            return;
        }
        LOG.debug("Sending message {} from {} to {}", new Object[]{message, self, c.remoteAddress()});
        c.writeAndFlush(message);
    }

    private void sendTcpMessage(Message message) {
        Channel c = getTCPChannel(message.getDestination());
        if (c == null) {
            LOG.error("Couldn't get channel for {}. Not sending message!", message.getDestination());
            return;
        }
        LOG.debug("Sending message {} from {} to {}", new Object[]{message, self, c.remoteAddress()});
        c.writeAndFlush(message);
    }

    private void sendDelayedMessages() {
        // As a side note: Concurrent Queues in Java need better consistency guarantees for iterators -.-
        if (delayedMessages.isEmpty()) { // At least stop early if nothing to do
            return;
        }
        LinkedList<Message> tryThisTime = new LinkedList<Message>();
        while (!delayedMessages.isEmpty()) {
            Message m = delayedMessages.poll(); // Could have evaported in another way^^
            if (m != null) {
                tryThisTime.add(m);
            }
        }
        for (Message m : tryThisTime) {
            trigger(m, net.getPair()); // just try again...might end up on the delayed list again, of course
        }
    }

    void addLocalSocket(InetSocketAddress localAddress, DatagramChannel channel) {
        udpChannels.put(localAddress, channel); // Not sure how this makes sense...but at least the pipeline is there
    }

    void addLocalSocket(InetSocketAddress remoteAddress, SocketChannel channel) {
        tcpChannels.put(remoteAddress, channel);
        trigger(new NetworkSessionOpened(remoteAddress, Transport.TCP), netC);
    }

    void addLocalSocket(InetSocketAddress remoteAddress, UdtChannel channel) {
        udtChannels.put(remoteAddress, channel);
        trigger(new NetworkSessionOpened(remoteAddress, Transport.UDT), netC);
    }

    void channelInactive(ChannelHandlerContext ctx, Transport protocol) {

        SocketAddress addr = ctx.channel().remoteAddress();

        if (addr instanceof InetSocketAddress) {
            InetSocketAddress remoteAddress = (InetSocketAddress) addr;
            switch (protocol) {
                case TCP:
                    tcpChannels.remove(remoteAddress);
                    break;
                case UDT:
                    udtChannels.remove(remoteAddress);
                    break;
                default:
                    throw new Error("Unknown Transport type");
            }
            LOG.debug("Channel {} ({}) closed", remoteAddress, protocol);
        }

    }

    Address socketToAddress(InetSocketAddress sock) {
        return new Address(sock.getAddress(), sock.getPort(), null);
    }

    InetSocketAddress addressToSocket(Address addr) {
        return new InetSocketAddress(addr.getIp(), addr.getPort());
    }

    private SocketChannel getTCPChannel(Address destination) {
        InetSocketAddress isa = addressToSocket(destination);
        SocketChannel c = tcpChannels.get(isa);
        if (c == null) {
            try {
                c = (SocketChannel) bootstrapTCPClient.connect(isa).sync().channel();
                tcpChannels.put(isa, c); // There's a chance of orphaning open connections here...not sure if worth locking for, though
            } catch (InterruptedException ex) {
                LOG.error("Interrupted while trying to connect to {}!", isa);
            }
        }
        return c;
    }

    private UdtChannel getUDTChannel(Address destination) {
        InetSocketAddress isa = addressToSocket(destination);
        UdtChannel c = udtChannels.get(isa);
        if (c == null) {
            Integer udtPort = udtPortMap.get(destination.getPort());
            if ((udtPort == null) || (udtPort < 1)) { // We have to ask for the UDT port first, since it's random
                DisambiguateConnection.Req r = new DisambiguateConnection.Req(self, destination, Transport.TCP, destination.getPort(), boundUDTPort);
                trigger(r, net.getPair());
                return null;
            }
            InetSocketAddress newISA = new InetSocketAddress(isa.getAddress(), udtPort);
            try {
                c = (UdtChannel) bootstrapUDTClient.connect(newISA).sync().channel();
                udtChannels.put(isa, c); // There's a chance of orphaning open connections here...not sure if worth locking for, though
            } catch (InterruptedException ex) {
                LOG.error("Interrupted while trying to connect to {}!", newISA);
            }
        }
        return c;
    }

}
