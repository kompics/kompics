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

import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import io.netty.channel.udt.UdtChannelOption;
import io.netty.channel.udt.nio.NioUdtProvider;
import io.netty.util.concurrent.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.MDC;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.ConnectionStatus;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkControl;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class NettyNetwork extends ComponentDefinition {

    public static final int STREAM_MAX = 65536;
    public static final int DATAGRAM_MAX = 1500;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    static final int RECV_BUFFER_SIZE = 65536;
    static final int SEND_BUFFER_SIZE = 65536;
    static final int INITIAL_BUFFER_SIZE = 512;
    // Ports
    Negative<Network> net = provides(Network.class);
    Negative<NetworkControl> netC = provides(NetworkControl.class);
    // Network
    private ServerBootstrap bootstrapTCP;
    final Bootstrap bootstrapTCPClient;
    private Bootstrap bootstrapUDP;
    private ServerBootstrap bootstrapUDT;
    final Bootstrap bootstrapUDTClient;
    // private final LinkedList<Msg> delayedMessages = new LinkedList<Msg>();
    // private final LinkedList<MessageNotify.Req> delayedNotifies = new LinkedList<MessageNotify.Req>();
    // private final HashSet<DisambiguateConnection> delayedDisambs = new HashSet<DisambiguateConnection>();
    // private final HashMap<UUID, MessageNotify.Req> awaitingDelivery = new HashMap<UUID, MessageNotify.Req>();
    final ChannelManager channels = new ChannelManager(this);
    final MessageQueueManager messages = new MessageQueueManager(this);
    private DatagramChannel udpChannel;
    // Info
    final NettyAddress self;
    private final int boundPort; // TODO use me!
    volatile int boundUDTPort = -1; // Unbound
    private final boolean bindTCP;
    private final boolean bindUDP;
    private final boolean bindUDT;
    private InetAddress alternativeBindIf = null;
    private boolean udtMonitoring = false;
    private final long monitoringInterval = 1000; // 1s
    final int udtBufferSizes;
    final int udtMSS;
    // LOGGING
    public static final String MDC_KEY_PORT = "knet-port";
    public static final String MDC_KEY_IF = "knet-if";
    public static final String MDC_KEY_ALT_IF = "knet-alt-if";
    final Logger extLog = this.logger;
    private final Map<String, String> customLogCtx = new HashMap<>();

    void setCustomMDC() {
        MDC.setContextMap(customLogCtx);
    }

    private void initLoggingCtx() {
        for (Map.Entry<String, String> e : customLogCtx.entrySet()) {
            loggingCtxPutAlways(e.getKey(), e.getValue());
        }
    }

    public NettyNetwork(NettyInit init) {
        // probably useless to set here as it won't be re-read in most JVMs after start
        System.setProperty("java.net.preferIPv4Stack", "true");

        self = new NettyAddress(init.self);
        customLogCtx.put(MDC_KEY_PORT, Integer.toString(self.getPort()));
        customLogCtx.put(MDC_KEY_IF, self.getIp().getHostAddress());

        boundPort = self.getPort();

        // CONFIG
        Optional<InetAddress> abiO = config().readValue("netty.bindInterface", InetAddress.class);
        if (abiO.isPresent()) {
            alternativeBindIf = abiO.get();
            customLogCtx.put(MDC_KEY_ALT_IF, self.getIp().getHostAddress());
        }

        initLoggingCtx();

        logger.info("Alternative Bind Interface set to {}", alternativeBindIf);

        udtMonitoring = config().getValueOrDefault("netty.udt.monitor", false);
        if (udtMonitoring) {
            logger.info("UDT monitoring requested.");
        }

        udtBufferSizes = config().getValueOrDefault("netty.udt.buffer", -1);
        udtMSS = config().getValueOrDefault("netty.udt.mss", -1);

        // if (!self.equals(init.self)) {
        // LOG.error("Do NOT bind Netty to a virtual address!");
        // System.exit(1);
        // }
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
        NioEventLoopGroup groupUDT = new NioEventLoopGroup(0, (Executor) null, NioUdtProvider.BYTE_PROVIDER);
        bootstrapUDTClient.group(groupUDT).channelFactory(NioUdtProvider.BYTE_CONNECTOR)
                .handler(new NettyInitializer<SocketChannel>(new StreamHandler(this, Transport.UDT)))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .option(ChannelOption.SO_REUSEADDR, true);
        if (this.udtBufferSizes > 0) {
            bootstrapUDTClient.option(UdtChannelOption.PROTOCOL_SEND_BUFFER_SIZE, this.udtBufferSizes)
                    .option(UdtChannelOption.PROTOCOL_RECEIVE_BUFFER_SIZE, this.udtBufferSizes);
        }
        if (udtMonitoring) {
            logger.info("Activating UDT monitoring (client).");
            bootstrapUDTClient.group().scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    setCustomMDC();
                    try {
                        channels.monitor();
                    } finally {
                        MDC.clear();
                    }
                }
            }, monitoringInterval, monitoringInterval, TimeUnit.MILLISECONDS);
        }

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
            InetAddress bindIp = self.getIp();
            if (alternativeBindIf != null) {
                bindIp = alternativeBindIf;
            }
            if (bindTCP) {
                bindPort(bindIp, self.getPort(), Transport.TCP);
            }
            if (bindUDT) {
                bindPort(bindIp, self.getPort(), Transport.UDT);
            }
            if (bindUDP) {
                bindPort(bindIp, self.getPort(), Transport.UDP);
            }
        }

    };

    Handler<Stop> stopHandler = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            clearConnections();
        }
    };

    Handler<Msg<?, ?>> msgHandler = new Handler<Msg<?, ?>>() {

        @Override
        public void handle(Msg<?, ?> event) {
            if (event.getHeader().getDestination().sameHostAs(self)) {
                logger.trace("Delivering message {} locally.", event);
                trigger(event, net);
                return;
            }

            messages.send(event);
        }
    };

    Handler<MessageNotify.Req> notifyHandler = new Handler<MessageNotify.Req>() {

        @Override
        public void handle(final MessageNotify.Req notify) {
            Msg<?, ?> event = notify.msg;
            if (event.getHeader().getDestination().sameHostAs(self)) {
                logger.trace("Delivering message {} locally.", event);
                trigger(event, net);
                answer(notify);
                return;
            }
            messages.send(notify);
        }
    };

    Handler<SendDelayed> delayedHandler = new Handler<SendDelayed>() {

        @Override
        public void handle(SendDelayed event) {
            messages.retry(event);
        }
    };

    Handler<DropDelayed> dropHandler = new Handler<DropDelayed>() {

        @Override
        public void handle(DropDelayed event) {
            messages.drop(event);
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
        bootstrapUDP.group(group).channel(NioDatagramChannel.class).handler(new DatagramHandler(this, Transport.UDP));

        bootstrapUDP.option(ChannelOption.RCVBUF_ALLOCATOR,
                new AdaptiveRecvByteBufAllocator(1500, 1500, RECV_BUFFER_SIZE));
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

            // addLocalSocket(iAddr, c);
            logger.info("Successfully bound to ip:port {}:{}", addr, port);
        } catch (InterruptedException e) {
            logger.error("Problem when trying to bind to {}:{}", addr.getHostAddress(), port);
            return false;
        }

        return true;
    }

    private boolean bindTcpPort(final InetAddress addr, int port) {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        TCPServerHandler handler = new TCPServerHandler(this);
        bootstrapTCP = new ServerBootstrap();
        bootstrapTCP.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler((new NettyInitializer<SocketChannel>(handler))).option(ChannelOption.SO_REUSEADDR, true);

        try {
            bootstrapTCP.bind(new InetSocketAddress(addr, port)).sync();

            logger.info("Successfully bound to ip:port {}:{}", addr, port);
        } catch (InterruptedException e) {
            logger.error("Problem when trying to bind to {}:{}", addr, port);
            return false;
        }

        // InetSocketAddress iAddr = new InetSocketAddress(addr, port);
        return true;
    }

    private boolean bindUdtPort(final InetAddress addr) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(0, (Executor) null, NioUdtProvider.BYTE_PROVIDER);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(0, (Executor) null, NioUdtProvider.BYTE_PROVIDER);
        UDTServerHandler handler = new UDTServerHandler(this);
        bootstrapUDT = new ServerBootstrap();
        bootstrapUDT.group(bossGroup, workerGroup).channelFactory(NioUdtProvider.BYTE_ACCEPTOR)
                .childHandler(new NettyInitializer<UdtChannel>(handler)).option(ChannelOption.SO_REUSEADDR, true);
        if (this.udtBufferSizes > 0) {
            bootstrapUDT.option(UdtChannelOption.PROTOCOL_SEND_BUFFER_SIZE, this.udtBufferSizes)
                    .option(UdtChannelOption.PROTOCOL_RECEIVE_BUFFER_SIZE, this.udtBufferSizes);
            bootstrapUDT.childOption(UdtChannelOption.PROTOCOL_SEND_BUFFER_SIZE, this.udtBufferSizes)
                    .childOption(UdtChannelOption.PROTOCOL_RECEIVE_BUFFER_SIZE, this.udtBufferSizes);
        }
        if (udtMonitoring) {
            logger.info("Activating UDT monitoring (server).");
            bootstrapUDT.childGroup().scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    setCustomMDC();
                    try {
                        channels.monitor();
                    } finally {
                        MDC.clear();
                    }
                }
            }, monitoringInterval, monitoringInterval, TimeUnit.MILLISECONDS);
        }
        try {
            Channel c = bootstrapUDT.bind(addr, boundUDTPort).sync().channel();
            InetSocketAddress localAddress = (InetSocketAddress) c.localAddress(); // Should work
            boundUDTPort = localAddress.getPort(); // in case it was 0 -> random port

            logger.info("Successfully bound UDT to ip:port {}:{} with config: {}",
                    new Object[] { addr, boundUDTPort, c.config().getOptions() });
        } catch (InterruptedException e) {
            logger.error("Problem when trying to bind UDT to {}", addr);
            return false;
        }

        return true;
    }

    protected void networkException(NetworkException networkException) {
        trigger(networkException, netC);
    }

    protected void networkStatus(ConnectionStatus status) {
        trigger(status, netC);
    }

    protected void deliverMessage(Msg<?, ?> message, Channel c) {
        if (message instanceof DisambiguateConnection) {
            DisambiguateConnection msg = (DisambiguateConnection) message;
            channels.disambiguate(msg, c);
            if (msg.reply) {
                c.writeAndFlush(new MessageWrapper(
                        new DisambiguateConnection(self, msg.getSource(), msg.getProtocol(), boundUDTPort, false)));
            }
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
        if (message instanceof NotifyAck) {
            NotifyAck ack = (NotifyAck) message;
            logger.trace("Got NotifyAck for {}", ack.id);
            messages.ack(ack);
            return;
        }
        Header<?> h = message.getHeader();
        logger.debug("Delivering message {} from {} to {} protocol {}",
                new Object[] { message.toString(), h.getSource(), h.getDestination(), h.getProtocol() });
        trigger(message, net);
    }

    ChannelFuture sendUdpMessage(MessageWrapper msgw) {
        ByteBuf buf = udpChannel.alloc().ioBuffer(INITIAL_BUFFER_SIZE, SEND_BUFFER_SIZE);
        try {
            if (msgw.notify.isPresent() && msgw.notify.get().notifyOfDelivery) {
                MessageNotify.Req msgr = msgw.notify.get();
                AckRequestMsg arm = new AckRequestMsg(msgw.msg, msgr.getMsgId());
                Serializers.toBinary(arm, buf);
            } else {
                Serializers.toBinary(msgw.msg, buf);
            }
            msgw.injectSize(buf.readableBytes(), System.nanoTime());
            DatagramPacket pack = new DatagramPacket(buf, msgw.msg.getHeader().getDestination().asSocket());
            logger.debug("Sending Datagram message {} ({}bytes)", msgw.msg, buf.readableBytes());
            return udpChannel.writeAndFlush(pack);
        } catch (Exception e) { // serialisation might fail horribly with size bounded buff
            logger.warn("Could not send Datagram message {}, error was: {}", msgw, e);
            return null;
        }
    }

    private void clearConnections() {

        long tstart = System.currentTimeMillis();

        channels.clearConnections();

        if (bindUDP) {
            try {
                udpChannel.close().syncUninterruptibly();
            } catch (Exception ex) {
                logger.warn("Error during Netty shutdown. Messages might have been lost! \n {}", ex);
            }
        }

        messages.clear();

        long tend = System.currentTimeMillis();

        logger.info("Closed all connections in {}ms", tend - tstart);
    }

    @Override
    public void tearDown() {
        long tstart = System.currentTimeMillis();

        clearConnections();

        logger.info("Shutting down handler groups...");
        List<Future<?>> gfutures = new LinkedList<>();
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
        for (Future<?> f : gfutures) {
            f.syncUninterruptibly();
        }
        // bootstrapUDTClient = null;
        bootstrapTCP = null;
        // bootstrapTCPClient = null;
        bootstrapUDP = null;
        bootstrapUDT = null;

        long tend = System.currentTimeMillis();

        logger.info("Netty shutdown complete. It took {}ms", tend - tstart);

    }

    void trigger(KompicsEvent event) {
        if (event instanceof Msg) {
            throw new RuntimeException("Not support anymore!");
            // trigger(event, net.getPair());
        } else {
            trigger(event, onSelf);
        }
    }

    void notify(MessageNotify.Req notify) {
        answer(notify);
    }

    void notify(MessageNotify.Req notify, MessageNotify.Resp response) {
        answer(notify, response);
    }
}
