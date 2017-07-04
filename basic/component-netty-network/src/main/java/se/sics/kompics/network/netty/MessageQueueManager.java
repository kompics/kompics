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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.MDC;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;

/**
 *
 * @author lkroll
 */
class MessageQueueManager {

    private final NettyNetwork component;

    private final HashMap<InetSocketAddress, Queue<MessageWrapper>> tcpDelays = new HashMap<>();
    private final HashMap<InetSocketAddress, Queue<MessageWrapper>> udtDelays = new HashMap<>();
    private final ConcurrentHashMap<UUID, MessageNotify.Req> awaitingDelivery = new ConcurrentHashMap<>();

    MessageQueueManager(NettyNetwork component) {
        this.component = component;
    }

    void send(Msg msg) {
        send(new MessageWrapper(msg));
    }

    void send(MessageNotify.Req notify) {
        send(new MessageWrapper(notify));
    }

    private void send(MessageWrapper msg) {
        switch (msg.msg.getProtocol()) {
            case TCP:
                sendTCP(msg);
                break;
            case UDT:
                sendUDT(msg);
                break;
            case UDP: {
                ChannelFuture cf = component.sendUdpMessage(msg);
                if (msg.notify.isPresent()) {
                    if (cf != null) {
                        cf.addListener(new NotifyListener(msg.notify.get()));
                    } else {
                        msg.notify.get().prepareResponse(System.currentTimeMillis(), false, System.nanoTime());
                        component.notify(msg.notify.get());
                    }
                }
            }
            break;
            default:
                throw new Error("Unknown Transport type");
        }
    }

    private void sendTCP(MessageWrapper msg) {
        Address peer = msg.msg.getDestination();
        Queue<MessageWrapper> delays = tcpDelays.get(peer.asSocket());
        if (delays != null) {
            component.extLog.debug("Delaying message while establishing connection: {}", msg);
            delays.add(msg);
            return;
        }
        Channel c = component.channels.getTCPChannel(peer);
        if (c == null) {
            c = component.channels.createTCPChannel(peer, component.bootstrapTCPClient);
        }
        if (c == null) {
            delays = new LinkedList<>();
            tcpDelays.put(peer.asSocket(), delays);
            component.extLog.debug("Delaying message while establishing connection: {}", msg);
            delays.add(msg);
            return;
        }
        component.extLog.debug("Sending message {}. Local {}, Remote {}", new Object[]{msg, c.localAddress(), c.remoteAddress()});
        ChannelFuture cf = c.writeAndFlush(msg);
        if (msg.notify.isPresent()) {
            cf.addListener(new NotifyListener(msg.notify.get()));
        }
    }

    private void sendUDT(MessageWrapper msg) {
        Address peer = msg.msg.getDestination();
        Queue<MessageWrapper> delays = udtDelays.get(peer.asSocket());
        if (delays != null) {
            component.extLog.debug("Delaying message while establishing connection: {}", msg);
            delays.add(msg);
            return;
        }
        Channel c = component.channels.getUDTChannel(peer);
        if (c == null) {
            c = component.channels.createUDTChannel(peer, component.bootstrapUDTClient);
        }
        if (c == null) {
            delays = new LinkedList<>();
            udtDelays.put(peer.asSocket(), delays);
            component.extLog.debug("Delaying message while establishing connection: {}", msg);
            delays.add(msg);
            return;
        }
        component.extLog.debug("Sending message {}. Local {}, Remote {}", new Object[]{msg, c.localAddress(), c.remoteAddress()});
        ChannelFuture cf = c.writeAndFlush(msg);
        if (msg.notify.isPresent()) {
            cf.addListener(new NotifyListener(msg.notify.get()));
        }
    }

    void retry(SendDelayed event) {
        component.extLog.info("Trying to send delayed messages: {} on {}", event.peer, event.protocol);
        Address peer = event.peer;
        switch (event.protocol) {
            case TCP:
                retryTCP(peer);
                break;
            case UDT:
                retryUDT(peer);
                break;
            default:
                return;
        }
    }

    private void retryTCP(Address peer) {
        Queue<MessageWrapper> delays = tcpDelays.get(peer.asSocket());
        if (delays == null) {
            return;
        }
        Channel c = component.channels.getTCPChannel(peer);
        if (c == null) {
            component.extLog.warn("Connection to {} still not available. Not retrying anything.", peer);
            return;
        }
        while (!delays.isEmpty()) {
            MessageWrapper msg = delays.poll();
            component.extLog.debug("Sending message {}. Local {}, Remote {}", new Object[]{msg, c.localAddress(), c.remoteAddress()});
            ChannelFuture cf = c.write(msg);
            if (msg.notify.isPresent()) {
                cf.addListener(new NotifyListener(msg.notify.get()));
            }
        }
        c.flush();
        tcpDelays.remove(peer.asSocket());
    }

    private void retryUDT(Address peer) {
        Queue<MessageWrapper> delays = udtDelays.get(peer.asSocket());
        if (delays == null) {
            return;
        }
        Channel c = component.channels.getUDTChannel(peer);
        if (c == null) {
            component.extLog.warn("Connection to {} still not available. Not retrying anything.", peer);
            return;
        }
        while (!delays.isEmpty()) {
            MessageWrapper msg = delays.poll();
            component.extLog.debug("Sending message {}. Local {}, Remote {}", new Object[]{msg, c.localAddress(), c.remoteAddress()});
            ChannelFuture cf = c.write(msg);
            if (msg.notify.isPresent()) {
                cf.addListener(new NotifyListener(msg.notify.get()));
            }
        }
        c.flush();
        udtDelays.remove(peer.asSocket());
    }

    void drop(DropDelayed event) {
        Address peer = event.peer;
        Queue<MessageWrapper> delays;
        switch (event.protocol) {
            case TCP:
                delays = tcpDelays.remove(peer.asSocket());
                break;
            case UDT:
                delays = udtDelays.remove(peer.asSocket());
                break;
            default:
                return;
        }
        if (delays == null) {
            return;
        }
        for (MessageWrapper msgw : delays) {
            component.extLog.warn("Dropping message {} (with notify) because connection could not be established.", msgw);
            if (msgw.notify.isPresent()) {
                MessageNotify.Req notify = msgw.notify.get();
                notify.prepareResponse(System.currentTimeMillis(), false, System.nanoTime());
                component.notify(notify);
            }
        }
        delays.clear();
    }

    void ack(NotifyAck ack) {
        MessageNotify.Req req = awaitingDelivery.remove(ack.id);
        if (req != null) {
            component.notify(req, req.deliveryResponse(System.currentTimeMillis(), true, System.nanoTime()));
        } else {
            component.extLog.warn("Could not find MessageNotify.Req with id: {}!", ack.id);
        }
    }

    void clear() {
        component.extLog.info("Cleaning message queues.");
        this.tcpDelays.clear();
        this.udtDelays.clear();
        this.awaitingDelivery.clear();
    }

    class NotifyListener implements ChannelFutureListener {

        public final MessageNotify.Req notify;

        NotifyListener(MessageNotify.Req notify) {
            this.notify = notify;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            component.setCustomMDC();
            try {
                if (future.isSuccess()) {
                    notify.prepareResponse(System.currentTimeMillis(), true, System.nanoTime());
                    if (notify.notifyOfDelivery) {
                        awaitingDelivery.put(notify.getMsgId(), notify);
                    }
                } else {
                    component.extLog.warn("Sending of message {} did not succeed :( : {}", notify.msg, future.cause());
                    notify.prepareResponse(System.currentTimeMillis(), false, System.nanoTime());
                }
                component.notify(notify);
            } finally {
                MDC.clear();
            }

        }
    }
}
