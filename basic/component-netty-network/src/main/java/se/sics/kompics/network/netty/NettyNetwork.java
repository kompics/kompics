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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
import io.netty.util.concurrent.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkControl;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class NettyNetwork extends ComponentDefinition {

    public final Logger LOG;
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
    private final LinkedList<Msg> delayedMessages = new LinkedList<Msg>();
    private final LinkedList<MessageNotify.Req> delayedNotifies = new LinkedList<MessageNotify.Req>();
    private final HashSet<DisambiguateConnection> delayedDisambs = new HashSet<DisambiguateConnection>();
    final ChannelManager channels = new ChannelManager(this);
    private DatagramChannel udpChannel;
    // Info
    final NettyAddress self;
    private final int boundPort;
    volatile int boundUDTPort = -1; // Unbound
    private final boolean bindTCP;
    private final boolean bindUDP;
    private final boolean bindUDT;

    public NettyNetwork(NettyInit init) {
        System.setProperty("java.net.preferIPv4Stack", "true");

        self = new NettyAddress(init.self);

        boundPort = self.getPort();

        LOG = LoggerFactory.getLogger("NettyNetwork@" + self.getPort());

//        if (!self.equals(init.self)) {
//            LOG.error("Do NOT bind Netty to a virtual address!");
//            System.exit(1);
//        }
        bindTCP = init.protocols.contains(Transport.TCP);
        bindUDP = init.protocols.contains(Transport.UDP);
        bindUDT = init.protocols.contains(Transport.UDT);
        if (bindUDT) {
            boundUDTPort = init.udtPort;
        }

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
        subscribe(notifyHandler, net);
        subscribe(delayedHandler, loopback);
        subscribe(dropHandler, loopback);
    }

    Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            // Prepare listening sockets
            if (bindTCP) {
                bindPort(self.getIp(), self.getPort(), Transport.TCP);
            }
            if (bindUDT) {
                bindPort(self.getIp(), self.getPort(), Transport.UDT);
            }
            if (bindUDP) {
                bindPort(self.getIp(), self.getPort(), Transport.UDP);
            }
        }

    };

    Handler<Stop> stopHandler = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            clearConnections();
        }
    };

    Handler<Msg> msgHandler = new Handler<Msg>() {

        @Override
        public void handle(Msg event) {
            if (event.getDestination().sameHostAs(self)) {
                LOG.trace("Delivering message {} locally.", event);
                trigger(event, net);
                return;
            }
            if (sendMessage(event) == null) {
                if (event instanceof DisambiguateConnection) {
                    DisambiguateConnection dc = (DisambiguateConnection) event;
                    if (delayedDisambs.add(dc)) {
                        LOG.info("Couldn't find channel for {}. Delaying message while establishing connection!", event);
                    }
                    return;
                }
                LOG.info("Couldn't find channel for {}. Delaying message while establishing connection!", event);
                delayedMessages.offer(event);
            }
        }
    };

    Handler<MessageNotify.Req> notifyHandler = new Handler<MessageNotify.Req>() {

        @Override
        public void handle(final MessageNotify.Req notify) {
            Msg event = notify.msg;
            if (event.getDestination().sameHostAs(self)) {
                LOG.trace("Delivering message {} locally.", event);
                trigger(event, net);
                answer(notify);
                return;
            }
            ChannelFuture f = sendMessage(event);
            if (f == null) {
                LOG.info("Couldn't find channel for {} (with notify). Delaying message while establishing connection!", event);
                delayedNotifies.offer(notify);
                return; // Assume message got delayed or some error occurred
            }
            f.addListener(new NotifyListener(notify));
        }
    };

    Handler<SendDelayed> delayedHandler = new Handler<SendDelayed>() {

        @Override
        public void handle(SendDelayed event) {
            if (delayedMessages.isEmpty() && delayedNotifies.isEmpty()) { // At least stop early if nothing to do
                return;
            }
            LOG.info("Trying to send delayed messages.");
            Iterator<Msg> mit = delayedMessages.listIterator();
            while (mit.hasNext()) {
                Msg m = mit.next();
                if (sendMessage(m) != null) {
                    mit.remove();
                }
            }

            Iterator<MessageNotify.Req> mnit = delayedNotifies.listIterator();
            while (mnit.hasNext()) {
                MessageNotify.Req m = mnit.next();
                ChannelFuture f = sendMessage(m.msg);
                if (f != null) {
                    mnit.remove();
                    f.addListener(new NotifyListener(m));
                }
            }
        }
    };

    Handler<DropDelayed> dropHandler = new Handler<DropDelayed>() {

        @Override
        public void handle(DropDelayed event) {
            LOG.info("Cleaning delayed messages.");
            for (Iterator<Msg> it = delayedMessages.iterator(); it.hasNext();) {
                Msg m = it.next();
                if (m.getDestination().asSocket().equals(event.isa) && event.protocol == m.getProtocol()) {
                    LOG.warn("Dropping message {} because connection could not be established.", m);
                    it.remove();
                }
            }
            for (Iterator<MessageNotify.Req> it = delayedNotifies.iterator(); it.hasNext();) {
                MessageNotify.Req notify = it.next();
                Msg m = notify.msg;
                if (m.getDestination().asSocket().equals(event.isa) && event.protocol == m.getProtocol()) {
                    LOG.warn("Dropping message {} (with notify) because connection could not be established.", m);
                    it.remove();
                    notify.prepareResponse(System.currentTimeMillis(), false);
                    answer(notify);
                }
            }
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
            udpChannel = (DatagramChannel) bootstrapUDP.bind(iAddr).sync().channel();

            //addLocalSocket(iAddr, c);
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
            Channel c = bootstrapUDT.bind(addr, boundUDTPort).sync().channel();
            InetSocketAddress localAddress = (InetSocketAddress) c.localAddress(); // Should work
            boundUDTPort = localAddress.getPort(); // in case it was 0 -> random port

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

    protected void deliverMessage(Msg message, Channel c) {
        if (message instanceof DisambiguateConnection) {
            DisambiguateConnection msg = (DisambiguateConnection) message;
            channels.disambiguate(msg, c);
            if (msg.reply) {
                c.writeAndFlush(new DisambiguateConnection(self, msg.getSource(), msg.getProtocol(), boundUDTPort, false));
            }
            //trigger(SendDelayed.event, onSelf);
            return;
        }
        if (message instanceof CheckChannelActive) {
            CheckChannelActive msg = (CheckChannelActive) message;
            channels.checkActive(msg, c);
            return;
        }
        if (message instanceof CloseChannel) {
            CloseChannel msg = (CloseChannel) message;
            channels.flushAndClose(msg, c);
            return;
        }
        LOG.debug(
                "Delivering message {} from {} to {} protocol {}",
                new Object[]{message.toString(), message.getSource(),
                    message.getDestination(), message.getProtocol()});
        trigger(message, net);
    }

    private ChannelFuture sendMessage(Msg message) {
        switch (message.getProtocol()) {
            case TCP:
                return sendTcpMessage(message);
            case UDT:
                return sendUdtMessage(message);
            case UDP:
                return sendUdpMessage(message);
            default:
                throw new Error("Unknown Transport type");
        }
    }

    private ChannelFuture sendUdpMessage(Msg message) {
        ByteBuf buf = udpChannel.alloc().buffer(INITIAL_BUFFER_SIZE, SEND_BUFFER_SIZE);
        try {
            Serializers.toBinary(message, buf);
            DatagramPacket pack = new DatagramPacket(buf, message.getDestination().asSocket());
            LOG.debug("Sending Datagram message {} ({}bytes)", message, buf.readableBytes());
            return udpChannel.writeAndFlush(pack);
        } catch (Exception e) { // serialization might fail horribly with size bounded buff
            LOG.warn("Could not send Datagram message {}, error was: {}", message, e);
            return null;
        }
    }

    private ChannelFuture sendUdtMessage(Msg message) {
        Channel c = channels.getUDTChannel(message.getDestination());
        if (c == null) {
            c = channels.createUDTChannel(message.getDestination(), bootstrapUDTClient);
        }
        if (c == null) {
            return null;
        }
        LOG.debug("Sending message {}. Local {}, Remote {}", new Object[]{message, c.localAddress(), c.remoteAddress()});
        return c.writeAndFlush(message);
    }

    private ChannelFuture sendTcpMessage(Msg message) {
        Channel c = channels.getTCPChannel(message.getDestination());
        if (c == null) {
            c = channels.createTCPChannel(message.getDestination(), bootstrapTCPClient);
        }
        if (c == null) {
            return null;
        }
        LOG.debug("Sending message {}. Local {}, Remote {}", new Object[]{message, c.localAddress(), c.remoteAddress()});
        return c.writeAndFlush(message);
    }

//    void addLocalSocket(InetSocketAddress localAddress, DatagramChannel channel) {
//        udpChannels.put(localAddress, channel); // Not sure how this makes sense...but at least the pipeline is there
//    }
//    void addLocalSocket(InetSocketAddress remoteAddress, SocketChannel channel) {
//        tcpChannels.put(remoteAddress, channel);
//        trigger(new NetworkSessionOpened(remoteAddress, Transport.TCP), netC);
//    }
//
//    void addLocalSocket(InetSocketAddress remoteAddress, UdtChannel channel) {
//        udtChannels.put(remoteAddress, channel);
//        trigger(new NetworkSessionOpened(remoteAddress, Transport.UDT), netC);
//    }
    private void clearConnections() {

        long tstart = System.currentTimeMillis();

        channels.clearConnections();

        if (bindUDP) {
            udpChannel.close().syncUninterruptibly();
        }

        delayedMessages.clear();
        delayedNotifies.clear();
        delayedDisambs.clear();

        long tend = System.currentTimeMillis();

        LOG.info("Closed all connections in {}ms", tend - tstart);
    }

    @Override
    public void tearDown() {
        long tstart = System.currentTimeMillis();

        clearConnections();

        LOG.info("Shutting down handler groups...");
        List<Future> gfutures = new LinkedList<Future>();
        gfutures.add(bootstrapUDTClient.group().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS));
        if (bindTCP) {
            gfutures.add(bootstrapTCP.childGroup().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS));
            gfutures.add(bootstrapTCP.group().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS));
        }
        gfutures.add(bootstrapTCPClient.group().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS));
        if (bindUDP) {
            gfutures.add(bootstrapUDP.group().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS));
        }
        if (bindUDT) {
            gfutures.add(bootstrapUDT.childGroup().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS));
            gfutures.add(bootstrapUDT.group().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS));
        }
        for (Future f : gfutures) {
            f.syncUninterruptibly();
        }
        bootstrapUDTClient = null;
        bootstrapTCP = null;
        bootstrapTCPClient = null;
        bootstrapUDP = null;
        bootstrapUDT = null;

        long tend = System.currentTimeMillis();

        LOG.info("Netty shutdown complete. It took {}ms", tend - tstart);

    }

    public void trigger(KompicsEvent event) {
        if (event instanceof Msg) {
            trigger(event, net.getPair());
        } else {
            trigger(event, onSelf);
        }
    }

    class NotifyListener implements ChannelFutureListener {

        public final MessageNotify.Req notify;

        NotifyListener(MessageNotify.Req notify) {
            this.notify = notify;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                notify.prepareResponse(System.currentTimeMillis(), true);
            } else {
                LOG.warn("Sending of message {} did not succeed :( : {}", notify.msg, future.cause());
                notify.prepareResponse(System.currentTimeMillis(), false);
            }
            answer(notify);
        }

    }
}
