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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
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

    private static final Logger LOG = LoggerFactory.getLogger(DataStreamInterceptor.class);

    final Positive<Network> netDown = requires(Network.class);
    final Negative<Network> netUp = provides(Network.class);
    final Positive<Timer> timer = requires(Timer.class);

    private final Map<UUID, TrackedMessage> outstanding = new HashMap<>();
    private final Stats stats = new Stats();
    private UUID timeoutId = null;
    private Transport proto = Transport.UDT;

    public DataStreamInterceptor() {
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
            DataHeader h = (DataHeader) event.getHeader();
            if (h.getProtocol() == Transport.DATA) {
                LOG.trace("Got DATA message to track: {}", event);
                h.replaceProtocol(proto);
                MessageNotify.Req req = MessageNotify.createWithDeliveryNotification(event);
                Optional<MessageNotify.Req> or = Optional.absent();
                long ts = System.currentTimeMillis();
                trigger(req, netDown);
                TrackedMessage tm = new TrackedMessage(event, ts, or);
                outstanding.put(req.getMsgId(), tm);
            } else {
                throw new RuntimeException("Invalid protocol: " + h.getProtocol());
            }
        }
    };
    Handler<MessageNotify.Req> reqHandler = new Handler<MessageNotify.Req>() {

        @Override
        public void handle(MessageNotify.Req event) {
            DataHeader h = (DataHeader) event.msg.getHeader();
            if (h.getProtocol() == Transport.DATA) {
                LOG.debug("Got DATA message (notify) to track: {}", event.msg);
                h.replaceProtocol(proto);
                MessageNotify.Req req = MessageNotify.createWithDeliveryNotification(event.msg);
                Optional<MessageNotify.Req> or = Optional.of(event);
                long ts = System.currentTimeMillis();
                trigger(req, netDown);
                TrackedMessage tm = new TrackedMessage(event.msg, ts, or);
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
                        double dt = ((double) event.getDeliveryTime()) / (1e9);
                        stats.updateDelivery(dt, event.getSize());
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
            if (stats.updated) {
                LOG.info("Current Stats: { \n {} \n }", stats);
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

        TrackedMessage(Msg msg, long ts, Optional<MessageNotify.Req> originalRequest) {
            this.msg = msg;
            this.ts = ts;
            this.originalRequest = originalRequest;
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

    static class Stats {
        private SummaryStatistics delTime = new SummaryStatistics();
        private SummaryStatistics size = new SummaryStatistics();
        private boolean updated = true;
        private long lastPrintTS = -1;
        private long sizeAccum = 0;
        private SummaryStatistics approxThroughput = new SummaryStatistics();

        void reset() {
            delTime = new SummaryStatistics();
            size = new SummaryStatistics();
            lastPrintTS = -1;
            sizeAccum = 0;
            approxThroughput = new SummaryStatistics();
        }

        @Override
        public String toString() {
            updated = false;
            
            if (lastPrintTS > 0) {
                long diffL = System.nanoTime() - lastPrintTS;
                double diffD = ((double)diffL)/(1000.0*1000.0*1000.0);
                double accumD = ((double)sizeAccum)/1024.0;
                double tp = accumD/diffD;
                approxThroughput.addValue(tp);
            }
            sizeAccum = 0;
            lastPrintTS = System.nanoTime();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Delivery Time: ");
            sb.append(delTime);
            sb.append('\n');
            sb.append("Message Size: ");
            sb.append(size);
            sb.append('\n');
            sb.append("Approximate Throughput: ");
            sb.append(approxThroughput);
            sb.append('\n');
            return sb.toString();
        }

        private void updateDelivery(double delTD, int msgSize) {
            double sizeD = msgSize / 1024.0; // kb
            size.addValue(sizeD);
            delTime.addValue(delTD);
            sizeAccum += msgSize;
            
            updated = true;
        }
    }
}
