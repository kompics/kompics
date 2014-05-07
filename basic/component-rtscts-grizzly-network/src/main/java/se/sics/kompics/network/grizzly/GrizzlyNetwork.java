/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009-2011 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009-2011 Royal Institute of Technology (KTH)
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
package se.sics.kompics.network.grizzly;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.ClearToSend;
import se.sics.kompics.network.ConnectionStatusRequest;
import se.sics.kompics.network.ConnectionStatusResponse;
import se.sics.kompics.network.DataMessage;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkConnectionRefused;
import se.sics.kompics.network.NetworkControl;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.NetworkSessionClosed;
import se.sics.kompics.network.NetworkSessionOpened;
import se.sics.kompics.network.RequestToSend;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.grizzly.kryo.KryoSerializationFilter;

/**
 * The
 * <code>GrizzlyNetwork</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: GrizzlyNetwork.java 4050 2012-03-30 14:32:18Z Cosmin $
 */
public class GrizzlyNetwork extends ComponentDefinition {

    private static final Logger logger = LoggerFactory
            .getLogger(GrizzlyNetwork.class);
    private static final long FLOW_TIMEOUT = 10 * 1000; // 10s
    /**
     * The "implemented" Network port.
     */
    Negative<Network> net = negative(Network.class);
    /**
     * The "implemented" NetworkControl port.
     */
    Negative<NetworkControl> netControl = negative(NetworkControl.class);
    private FilterChainBuilder filterChainBuilder;
    private TCPNIOTransportBuilder builder;
    private TCPNIOTransport transport;
    private final HashMap<InetSocketAddress, GrizzlySession> tcpSession;
    private GrizzlyHandler tcpHandler;
    private InetSocketAddress localSocketAddress;
    int connectRetries;
    ExecutorService sendersPool;
    ExecutorService lowPrioritySendersPool;
    private Address self;
    // I want to send
    private final ReadWriteLock outgoingLock = new ReentrantReadWriteLock();
    //private final Map<Integer, CTSMessage> outgoingFlows = new TreeMap<Integer, CTSMessage>(); // requestId to actual CTS
    private final Map<Integer, Integer> outgoingRemainingQuota = new TreeMap<Integer, Integer>(); // requestId to remaining quota
    private final Map<Integer, ClearToSend> pendingRequests = new TreeMap<Integer, ClearToSend>(); // requestId to original CTS
    private final AtomicInteger requestCounter = new AtomicInteger(0); // Not necessary but has the nice incrementAndGet method
    // Others want to send
    private QuotaAllocator qAlloc;
    private final Queue<RTSMessage> requestQueue = new ConcurrentLinkedQueue<RTSMessage>();
    private final SortedSet<Integer> flowIdPool = new TreeSet<Integer>(); // free flowIds
//    private final ConcurrentMap<Integer, Long> lastMessage = new ConcurrentSkipListMap<Integer, Long>(); // flowId to ms timestamp
    private AtomicIntegerArray flowCounter;
    private AtomicLongArray lastMessage;

    /**
     * Instantiates a new Netty network component.
     */
    public GrizzlyNetwork(GrizzlyNetworkInit init) {
        filterChainBuilder = FilterChainBuilder.stateless();

        tcpSession = new HashMap<InetSocketAddress, GrizzlySession>();
        tcpHandler = new GrizzlyHandler(this, Transport.TCP);

        sendersPool = Executors.newCachedThreadPool(new SenderThreadFactory());
        lowPrioritySendersPool = Executors.newSingleThreadExecutor(
                new SenderThreadFactory());


        subscribe(handleMessage, net);
        subscribe(handleData, net);
        subscribe(handleRTS, net);

        subscribe(handleStatusReq, netControl);

        subscribe(startHandler, control);
        subscribe(stopHandler, control);
        
        // INIT

        self = init.getSelf();

        connectRetries = init.getConnectRetries();
        localSocketAddress = new InetSocketAddress(init.getSelf().getIp(),
                init.getSelf().getPort());

        //incomingFlows = new Address[init.getNumberOfChannels()];
        for (int i = 0; i < init.getNumberOfChannels(); i++) {
            flowIdPool.add(i);
        }
        qAlloc = init.getAllocator();


        int[] ints = new int[init.getNumberOfChannels()];
        Arrays.fill(ints, 0);
        long[] longs = new long[init.getNumberOfChannels()];
        Arrays.fill(ints, -1);
        flowCounter = new AtomicIntegerArray(ints);
        lastMessage = new AtomicLongArray(longs);

        boolean compress = init.getCompressionLevel() != 0;
        int initialBufferCapacity = init.getInitialBufferCapacity();
        int maxBufferCapacity = init.getMaxBufferCapacity();
        int workerCount = init.getWorkerCount();

        filterChainBuilder.add(new TransportFilter());
        // filterChainBuilder.add(new ProtostuffSerializationFilter());
        filterChainBuilder.add(new KryoSerializationFilter(compress,
                initialBufferCapacity, maxBufferCapacity));
        // filterChainBuilder.add(new JavaSerializationFilter());
        filterChainBuilder.add(tcpHandler);

        builder = TCPNIOTransportBuilder.newInstance();

        final ThreadPoolConfig config = ThreadPoolConfig.defaultConfig();
        config.setCorePoolSize(workerCount).setMaxPoolSize(workerCount)
                .setQueueLimit(-1);

        builder.setIOStrategy(SameThreadIOStrategy.getInstance())
                // .setIOStrategy(WorkerThreadIOStrategy.getInstance())
                // .setIOStrategy(SimpleDynamicNIOStrategy.getInstance())
                // .setIOStrategy(LeaderFollowerNIOStrategy.getInstance())
                .setKeepAlive(true).setReuseAddress(true)
                .setTcpNoDelay(true);

        transport = builder.build();

        transport.setProcessor(filterChainBuilder.build());
        transport.setConnectionTimeout(5000);
        transport.setReuseAddress(true);
        transport.setKeepAlive(true);
        transport.setTcpNoDelay(true);

        transport.setSelectorRunnersCount(init.getSelectorCount());
        transport.setWorkerThreadPoolConfig(config);
    }
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            try {
                transport.bind(localSocketAddress);
                transport.start();
                logger.debug("Bound to port {}", localSocketAddress.toString());
            } catch (IOException e) {
                throw new RuntimeException("Grizzly cannot bind/start port", e);
            }
        }
    };
    Handler<Stop> stopHandler = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            try {
                
                transport.stop();
                transport.unbindAll();
                transport = null;
                logger.debug("All ports unbound.");
            } catch (IOException e) {
                throw new RuntimeException("Grizzly cannot stop port", e);
            }
        }
    };
    /**
     * The handle message.
     */
    Handler<Message> handleMessage = new Handler<Message>() {
        @Override
        public void handle(Message message) {
            logger.debug(
                    "Handling Message {} from {} to {} protocol {}",
                    new Object[]{message, message.getSource(),
                        message.getDestination(), message.getProtocol()});

            if (message.getDestination().sameHostAs(self)) {
                // deliver locally
                trigger(message, net);
                return;
            }

            sendMessage(message);
        }
    };
    Handler<DataMessage> handleData = new Handler<DataMessage>() {
        @Override
        public void handle(DataMessage event) {
            Message msg = event.getMessage();
            int reqId = event.getRequestId();
            boolean send = false;
            boolean extend = false;

            outgoingLock.writeLock().lock();
            try {
                int remQuota = outgoingRemainingQuota.get(reqId);
                if (remQuota > 0) {
                    int newRemQuota = remQuota - 1;
                    outgoingRemainingQuota.put(reqId, newRemQuota);
                    send = true;
                    if (newRemQuota == 0) {
                        extend = true;
                    }
                }
            } finally {
                outgoingLock.writeLock().unlock();
            }
            if (send) {
                msg.setFlowId(event.getFlowId());
                sendMessage(msg);
            } else {
                logger.warn("Flow with rId={} is over quota. Message {} from {} to {} will be dropped!",
                        new Object[]{reqId, msg.toString(), msg.getSource(), msg.getDestination()});
            }

            // Handle Final Flag
            if (event.isFinal()) {
                logger.debug("Finalizing stream with rId={}", reqId);
                FinalMessage fm = new FinalMessage(msg.getSource(), msg.getDestination(), reqId, event.getFlowId());
                sendMessage(fm);
                outgoingLock.writeLock().lock();
                try {
                    outgoingRemainingQuota.remove(reqId);
                    //outgoingFlows.remove(reqId);
                    pendingRequests.remove(reqId);
                } finally {
                    outgoingLock.writeLock().unlock();
                }
            } else if (extend) {
                logger.debug("Extending stream with rId={}", reqId);
                RTSMessage rtsm = new RTSMessage(msg.getSource(), msg.getDestination(), reqId, true);
                sendMessage(rtsm);
            }
        }
    };
    Handler<RequestToSend> handleRTS = new Handler<RequestToSend>() {
        @Override
        public void handle(RequestToSend event) {
            if (event.getDestination().sameHostAs(self)) {
                // allow unlimited flows for local
                logger.debug("Handling local RTS event at {}", self);
                ClearToSend cts = event.getEvent();
                cts.setFlowId(-1);
                cts.setQuota(Integer.MAX_VALUE);
                cts.setRequestId(-1);
                trigger(cts, net);
                return;
            }


            int requestId = requestCounter.incrementAndGet();

            logger.debug(
                    "Handling RTS event {} from {} to {} requestId {}",
                    new Object[]{event, event.getSource(),
                        event.getDestination(), requestId});


            RTSMessage rtsm = new RTSMessage(event.getSource(), event.getDestination(), requestId);
            outgoingLock.writeLock().lock();
            try {
                //logger.debug("acquired wl.");
                pendingRequests.put(requestId, event.getEvent());
            } finally {
                outgoingLock.writeLock().unlock();
                //logger.debug("released wl.");
            }

            sendMessage(rtsm);
        }
    };

    private void sendMessage(Message msg) {
        Transport protocol = msg.getProtocol();
        Address destination = msg.getDestination();

        switch (protocol) {
            case TCP:
            case UDP:
                GrizzlySession tcpSess = getTcpSession(destination);
                tcpSess.sendMessage(msg);
                break;
        }
    }

    final void deliverMessage(Message message, Transport protocol) {
        message.setProtocol(protocol);

        if (message instanceof RTSMessage) {
            handleRTS((RTSMessage) message);
            GrizzlyListener.delivered(message);
            return;
        }

        if (message instanceof CTSMessage) {
            handleCTS((CTSMessage) message);
            GrizzlyListener.delivered(message);
            return;
        }

        if (message instanceof FinalMessage) {
            handleFinal((FinalMessage) message);
            GrizzlyListener.delivered(message);
            return;
        }
        logger.debug(
                "Delivering message on flow {} from {} to {} protocol {}",
                new Object[]{message.getFlowId(), message.getSource(),
                    message.getDestination(), message.getProtocol()});

        trigger(message, net);

        GrizzlyListener.delivered(message);

        int flowId = message.getFlowId();
        if (flowId >= 0) {
            lastMessage.set(flowId, System.currentTimeMillis());
            int rem = flowCounter.decrementAndGet(flowId);
            if (rem <= 0) {
                logger.debug("Flow {} exhausted quota. Reallocating.", flowId);
                lastMessage.set(flowId, -1);
                synchronized (flowIdPool) {
                    flowIdPool.add(flowId);
                    checkFlowPool(); // Don't acquire lock twice
                }
            }
        }
    }

    private void handleFinal(FinalMessage message) {
        logger.debug(
                "Handling FinalMessage from {} to {} reqId={}.",
                new Object[]{message.toString(), message.getSource(),
                    message.getDestination(), message.requestId});

        lastMessage.set(message.flowId, -1);
        synchronized (flowIdPool) {
            flowIdPool.add(message.flowId);
            // Maybe add some sanity checking if misbehaving nodes are expected
            checkFlowPool();
        }

        qAlloc.endFlow(message.requestId, message.getSource());
    }

    private void handleRTS(RTSMessage message) {
        logger.debug(
                "Handling RTS message {} from {} to {} protocol {}",
                new Object[]{message.toString(), message.getSource(),
                    message.getDestination(), message.getProtocol()});

        requestQueue.offer(message);
        if (!message.extension) {
            qAlloc.newFlow(message.reqId, message.getSource());
        }
        checkFlowPool();
    }

    private void handleCTS(CTSMessage message) {
        try {
            logger.debug(
                    "Handling CTS message {} from {} to {} protocol {}",
                    new Object[]{message.toString(), message.getSource(),
                        message.getDestination(), message.getProtocol()});
            
            ClearToSend cts = null;
            outgoingLock.writeLock().lock();
            //logger.debug("acquired wl.");
            try {
                cts = pendingRequests.get(message.reqId);
                if (cts == null) {
                    logger.warn("No pending request matches id " + message.reqId);
                    return;
                }
                //outgoingFlows.put(message.reqId, message);
                outgoingRemainingQuota.put(message.reqId, message.quota);
            } finally {
                outgoingLock.writeLock().unlock();
                //logger.debug("released wl.");
            }
            cts = (ClearToSend) cts.clone();
            cts.setFlowId(message.flowId);
            cts.setQuota(message.quota);
            cts.setRequestId(message.reqId);
            trigger(cts, net);
            logger.debug("triggered CTS event");
        }   catch (CloneNotSupportedException ex) {
            logger.warn("Cloudn't clone CTS event", ex);
        }
    }

    private void checkFlowPool() {
        // CLEANUP
        // This is not necessarily consistent, but should be good enough
        long now = System.currentTimeMillis();
        Set<Integer> timeoutedIds = new TreeSet<Integer>();
        for (int i = 0; i < lastMessage.length(); i++) {
            long ts = lastMessage.get(i);
            if ((ts >= 0) && ((now - ts) > FLOW_TIMEOUT)) {
                timeoutedIds.add(i);
                logger.debug("Flow {} timeouted. Deallocating.", i);
                lastMessage.set(i, -1);
            }
        }

        synchronized (flowIdPool) {
            flowIdPool.addAll(timeoutedIds);

            // ASSIGNMENT
            while (!flowIdPool.isEmpty()) {
                RTSMessage rtsm = requestQueue.poll(); // This doesn't use locking so no deadlock possibility
                if (rtsm == null) {
                    return;
                }
                int fId = flowIdPool.first();
                flowIdPool.remove(fId);
                int quota = qAlloc.getQuota(rtsm.reqId, rtsm.getSource());
                CTSMessage ctsm = new CTSMessage(rtsm.getDestination(),
                        rtsm.getSource(), rtsm.reqId, fId, quota);
                lastMessage.set(fId, now);
                flowCounter.set(fId, quota);
                sendMessage(ctsm);
            }
        }
    }

    private GrizzlySession getTcpSession(Address destination) {
        InetSocketAddress socketAddress = address2SocketAddress(destination);
        synchronized (tcpSession) {
            GrizzlySession session = tcpSession.get(socketAddress);
            if (session == null) {
                session = new GrizzlySession(transport, Transport.TCP,
                        address2SocketAddress(destination), this);
                session.connectInit();
                tcpSession.put(socketAddress, session);
            }
            return session;
        }
    }

    final void dropSession(GrizzlySession s) {
        synchronized (tcpSession) {
            tcpSession.remove(s.getRemoteAddress());
        }
        trigger(new NetworkConnectionRefused(s.getRemoteAddress(),
                Transport.TCP), netControl);
    }

    final void networkException(NetworkException event) {
        trigger(event, netControl);
    }

    final void networkSessionClosed(Connection<?> channel) {
        InetSocketAddress clientSocketAddress = (InetSocketAddress) channel
                .getPeerAddress();
        synchronized (tcpSession) {
            tcpSession.remove(clientSocketAddress);
        }

        trigger(new NetworkSessionClosed(clientSocketAddress, Transport.TCP),
                netControl);
    }

    final void networkSessionOpened(Connection<?> session) {
        InetSocketAddress clientSocketAddress = (InetSocketAddress) session
                .getPeerAddress();
        trigger(new NetworkSessionOpened(clientSocketAddress, Transport.TCP),
                netControl);
    }

    private InetSocketAddress address2SocketAddress(Address address) {
        return new InetSocketAddress(address.getIp(), address.getPort());
    }
    Handler<ConnectionStatusRequest> handleStatusReq = new Handler<ConnectionStatusRequest>() {
        @Override
        public void handle(ConnectionStatusRequest event) {
            HashSet<InetSocketAddress> tcp = null;
            synchronized (tcpSession) {
                tcp = new HashSet<InetSocketAddress>(tcpSession.keySet());
            }
            trigger(new ConnectionStatusResponse(event, tcp,
                    new HashSet<InetSocketAddress>()), netControl);
        }
    };
}
