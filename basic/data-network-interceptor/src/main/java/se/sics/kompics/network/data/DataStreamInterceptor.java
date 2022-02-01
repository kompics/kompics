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
package se.sics.kompics.network.data;

import java.util.Optional;
import com.lkroll.common.Either;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
@SuppressWarnings("rawtypes")
public class DataStreamInterceptor extends ComponentDefinition {

    final Positive<Timer> timer = requires(Timer.class);
    final Positive<Network> netDown = requires(Network.class);
    final Negative<Network> netUp = provides(Network.class);

    private final Map<UUID, TrackedMessage> outstanding = new HashMap<>();
    private UUID timeoutId = null;
    private final ConnectionFactory factory;
    private final HashMap<InetSocketAddress, ConnectionTracker> connections = new HashMap<>();
    private final long maxQueueLength;

    static final Logger EXT_LOG = LoggerFactory.getLogger(DataStreamInterceptor.class);

    public DataStreamInterceptor() {
        maxQueueLength = config().getValue("kompics.net.data.queueLength", Long.class);
        Optional<String> ratioPolicy = config().readValue("kompics.net.data.ratioPolicy", String.class);
        Optional<String> selectionPolicy = config().readValue("kompics.net.data.selectionPolicy", String.class);
        factory = new ConnectionFactory(config(), ratioPolicy, selectionPolicy);

        subscribe(startHandler, control);
        subscribe(msgHandler, netUp);
        subscribe(reqHandler, netUp);
        subscribe(respHandler, netDown);
        subscribe(timeoutHandler, timer);
    }

    Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
            StatsTimeout st = new StatsTimeout(spt);
            spt.setTimeoutEvent(st);
            trigger(spt, timer);
            timeoutId = st.getTimeoutId();
        }
    };
    Handler<Msg> msgHandler = new Handler<Msg>() {

        @SuppressWarnings("unchecked")
        @Override
        public void handle(Msg event) {
            Header h = event.getHeader();
            if (h.getProtocol() == Transport.DATA) {
                InetSocketAddress target = h.getDestination().asSocket();
                ConnectionTracker ct = connections.get(target);
                if (ct == null) {
                    ct = factory.findConnection(target);
                    connections.put(target, ct);
                }
                ct.enqueue(event);
                tryToSend(ct);
            } else {
                throw new RuntimeException("Invalid protocol: " + h.getProtocol());
            }
        }
    };
    Handler<MessageNotify.Req> reqHandler = new Handler<MessageNotify.Req>() {

        @Override
        public void handle(MessageNotify.Req mnr) {
            Msg event = mnr.msg;
            Header h = event.getHeader();
            if (h.getProtocol() == Transport.DATA) {
                InetSocketAddress target = h.getDestination().asSocket();
                ConnectionTracker ct = connections.get(target);
                if (ct == null) {
                    ct = factory.findConnection(target);
                    connections.put(target, ct);
                }
                ct.enqueue(mnr);
                tryToSend(ct);
            } else {
                throw new RuntimeException("Invalid protocol: " + h.getProtocol());
            }
        }
    };
    Handler<MessageNotify.Resp> respHandler = new Handler<MessageNotify.Resp>() {

        @SuppressWarnings("unused")
        @Override
        public void handle(MessageNotify.Resp event) {
            if (!event.isSuccess()) {
                System.out.println("Could not send message: " + event.msgId);
            }
            TrackedMessage tm = outstanding.get(event.msgId);
            ConnectionTracker ct = tm.connection;
            if (tm != null) {
                switch (event.getState()) {
                case SENT: {
                    logger.trace("Got a sent notify: {}", event);
                    // double st = ((double) event.getSendTime()) / (1e9);
                    // stats.update(st, event.getTime() - tm.ts, event.getSize());
                    if (tm.originalRequest.isPresent()) {
                        MessageNotify.Req or = tm.originalRequest.get();
                        or.injectSize(event.getSize(), 0);
                        or.prepareResponse(event.getTime(), event.isSuccess(), event.getSendTime());
                        answer(or);
                    }
                    ct.sent(event.msgId);
                    tryToSend(ct);
                }
                    break;
                case DELIVERED: {
                    logger.trace("Got a delivery notify: {}", event);
                    double dt = ((double) event.getDeliveryTime()) / Statistics.NANOSEC;
                    tm.connection.stats.update(dt, event.getSize());
                    if (tm.originalRequest.isPresent()) {
                        MessageNotify.Req or = tm.originalRequest.get();
                        if (or.notifyOfDelivery) {
                            or.injectSize(event.getSize(), 0);
                            answer(or,
                                    or.deliveryResponse(event.getTime(), event.isSuccess(), event.getDeliveryTime()));
                        }
                    }
                    outstanding.remove(event.msgId);
                }
                    break;
                default: {
                    logger.trace("Got a notify with a failure state: {}", event);
                    // double st = ((double) event.getSendTime()) / (1e9);
                    // stats.update(st, event.getTime() - tm.ts, event.getSize());
                    if (tm.originalRequest.isPresent()) {
                        MessageNotify.Req or = tm.originalRequest.get();
                        or.injectSize(event.getSize(), 0);
                        or.prepareResponse(event.getTime(), event.isSuccess(), event.getSendTime());
                        answer(or);
                    }
                    outstanding.remove(event.msgId);
                    ct.sent(event.msgId);
                    tryToSend(ct);
                }
                }

            } else {
                logger.warn("Got a response for an untracked message...something is probably wrong: \n {}", event);
                // throw new RuntimeException("Got a response for an untracked message...something is wrong!\n" +
                // event);
            }
        }
    };
    Handler<StatsTimeout> timeoutHandler = new Handler<StatsTimeout>() {

        @Override
        public void handle(StatsTimeout event) {
            for (ConnectionTracker ct : connections.values()) {
                ct.update();
            }
        }
    };

    private void tryToSend(ConnectionTracker ct) {
        while (ct.canSend(maxQueueLength)) {
            @SuppressWarnings("unchecked")
            Either<MessageNotify.Req, Msg> eMsg = ct.dequeue();
            Msg event;
            Optional<MessageNotify.Req> or;
            if (eMsg.isLeft()) {
                MessageNotify.Req mnr = eMsg.getLeft();
                event = mnr.msg;
                or = Optional.of(mnr);
            } else {
                event = eMsg.getRight();
                or = Optional.empty();
            }
            Header h = event.getHeader();
            @SuppressWarnings("unchecked")
            Transport proto = ct.selectionPolicy.select(event);
            ct.stats.updateSelection(proto);
            logger.trace("Got DATA message over {} to track: {}", proto, event);
            long ts = System.currentTimeMillis();
            MessageNotify.Req req;
            if (h instanceof DataHeader) { // replace protocol
                DataHeader dh = (DataHeader) event.getHeader();
                dh.replaceProtocol(proto);
                req = MessageNotify.createWithDeliveryNotification(event);
            } else { // wrap message
                Msg wrapped = new DataMsgWrapper(event, proto);
                req = MessageNotify.createWithDeliveryNotification(wrapped);
            }
            TrackedMessage tm = new TrackedMessage(event, ts, or, ct);
            trigger(req, netDown);
            outstanding.put(req.getMsgId(), tm);
        }
    }

    @Override
    public void tearDown() {
        logger.debug("Cleaning up timeouts.");
        if (timeoutId != null) {
            trigger(new CancelPeriodicTimeout(timeoutId), timer);
        }
    }

    static class TrackedMessage {

        public final Msg msg;
        public final long ts;
        public final Optional<MessageNotify.Req> originalRequest;
        public final ConnectionTracker connection;

        TrackedMessage(Msg msg, long ts, Optional<MessageNotify.Req> originalRequest, ConnectionTracker connection) {
            this.msg = msg;
            this.ts = ts;
            this.originalRequest = originalRequest;
            this.connection = connection;
        }
    }

    public static class StatsTimeout extends Timeout {

        public StatsTimeout(ScheduleTimeout st) {
            super(st);
        }

        public StatsTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
}
