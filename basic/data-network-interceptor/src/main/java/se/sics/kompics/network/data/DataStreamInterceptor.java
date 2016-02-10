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

import com.google.common.base.Optional;
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
 * @author lkroll
 */
public class DataStreamInterceptor extends ComponentDefinition {

    static final Logger LOG = LoggerFactory.getLogger(DataStreamInterceptor.class);

    final Positive<Network> netDown = requires(Network.class);
    final Negative<Network> netUp = provides(Network.class);
    final Positive<Timer> timer = requires(Timer.class);

    private final Map<UUID, TrackedMessage> outstanding = new HashMap<>();
    private UUID timeoutId = null;
    private final ConnectionFactory factory;
    private final HashMap<InetSocketAddress, ConnectionTracker> connections = new HashMap<>();

    public DataStreamInterceptor() {
        Optional<String> ratioPolicy = config().readValue("kompics.net.data.ratioPolicy", String.class);
        Optional<String> selectionPolicy = config().readValue("kompics.net.data.selectionPolicy", String.class);
        factory = new ConnectionFactory(ratioPolicy, selectionPolicy);

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
                Transport proto = ct.selectionPolicy.select(event);
                LOG.trace("Got DATA message over {} to track: {}", proto, event);
                MessageNotify.Req req = null;
                TrackedMessage tm = null;
                long ts = System.currentTimeMillis();
                Optional<MessageNotify.Req> or = Optional.absent();
                if (h instanceof DataHeader) { // replace protocol
                    DataHeader dh = (DataHeader) event.getHeader();
                    dh.replaceProtocol(proto);
                    req = MessageNotify.createWithDeliveryNotification(event);
                } else { // wrap message
                    Msg wrapped = new DataMsgWrapper(event, proto);
                    req = MessageNotify.createWithDeliveryNotification(wrapped);                  
                }
                tm = new TrackedMessage(event, ts, or, ct);
                trigger(req, netDown);
                outstanding.put(req.getMsgId(), tm);
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
                Transport proto = ct.selectionPolicy.select(event);
                LOG.trace("Got DATA message over {} to track: {}", proto, event);
                MessageNotify.Req req = null;
                TrackedMessage tm = null;
                long ts = System.currentTimeMillis();
                Optional<MessageNotify.Req> or = Optional.of(mnr);
                if (h instanceof DataHeader) { // replace protocol
                    DataHeader dh = (DataHeader) event.getHeader();
                    dh.replaceProtocol(proto);
                    req = MessageNotify.createWithDeliveryNotification(event);
                } else { // wrap message
                    Msg wrapped = new DataMsgWrapper(event, proto);
                    req = MessageNotify.createWithDeliveryNotification(wrapped);                  
                }
                tm = new TrackedMessage(event, ts, or, ct);
                trigger(req, netDown);
                outstanding.put(req.getMsgId(), tm);
            } else {
                throw new RuntimeException("Invalid protocol: " + h.getProtocol());
            }
        }
    };
    Handler<MessageNotify.Resp> respHandler = new Handler<MessageNotify.Resp>() {

        @Override
        public void handle(MessageNotify.Resp event) {
            if (!event.isSuccess()) {
                System.out.println("Could not send message: " + event.msgId);
            }
            TrackedMessage tm = outstanding.get(event.msgId);
            if (tm != null) {
                switch (event.getState()) {
                    case SENT: {
                        LOG.trace("Got a sent notify: {}", event);
//                        double st = ((double) event.getSendTime()) / (1e9);
//                        stats.update(st, event.getTime() - tm.ts, event.getSize());
                        if (tm.originalRequest.isPresent()) {
                            MessageNotify.Req or = tm.originalRequest.get();
                            or.injectSize(event.getSize(), 0);
                            or.prepareResponse(event.getTime(), event.isSuccess(), event.getSendTime());
                            answer(or);
                        }
                    }
                    break;
                    case DELIVERED: {
                        LOG.trace("Got a delivery notify: {}", event);
                        double dt = ((double) event.getDeliveryTime()) / Statistics.NANOSEC;
                        tm.connection.stats.update(dt, event.getSize());
                        if (tm.originalRequest.isPresent()) {
                            MessageNotify.Req or = tm.originalRequest.get();
                            if (or.notifyOfDelivery) {
                                or.injectSize(event.getSize(), 0);
                                answer(or, or.deliveryResponse(event.getTime(), event.isSuccess(), event.getDeliveryTime()));
                            }
                        }
                        outstanding.remove(event.msgId);
                    }
                    break;
                    default: {
                        LOG.trace("Got a notify with a failure state: {}", event);
//                        double st = ((double) event.getSendTime()) / (1e9);
//                        stats.update(st, event.getTime() - tm.ts, event.getSize());
                        if (tm.originalRequest.isPresent()) {
                            MessageNotify.Req or = tm.originalRequest.get();
                            or.injectSize(event.getSize(), 0);
                            or.prepareResponse(event.getTime(), event.isSuccess(), event.getSendTime());
                            answer(or);
                        }
                        outstanding.remove(event.msgId);
                    }
                }

            } else {
                LOG.warn("Got a response for an untracked message...something is probably wrong: \n {}", event);
                //throw new RuntimeException("Got a response for an untracked message...something is wrong!\n" + event);
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

    @Override
    public void tearDown() {
        LOG.debug("Cleaning up timeouts.");
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
