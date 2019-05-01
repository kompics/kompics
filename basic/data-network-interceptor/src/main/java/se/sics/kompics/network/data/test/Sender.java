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
package se.sics.kompics.network.data.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.data.DataNetwork;
import se.sics.kompics.network.data.DataNetwork.NetHook;
import se.sics.kompics.network.netty.NettyAddress;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

/**
 *
 * @author lkroll
 */
public class Sender {

    static {
        Serializers.register(new DataMessageSerialiser(), "dmS");
        Serializers.register(DataMessage.class, "dmS");
    }

    public static void main(String[] args) throws UnknownHostException {
        InetAddress ip = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        Address selfAddress = new NettyAddress(ip, port);
        InetAddress ip2 = InetAddress.getByName(args[2]);
        int port2 = Integer.parseInt(args[3]);
        Address targetAddress = new NettyAddress(ip2, port2);
        Kompics.createAndStart(Parent.class, new Parent.Init(selfAddress, targetAddress), Receiver.THREADS, 50);
    }

    public static class Parent extends ComponentDefinition {

        public Parent(final Init init) {
            System.out.println("Creating sender at " + init.selfAddress);
            final Component senderC = create(SenderC.class, new SenderC.Init(init.selfAddress, init.targetAddress));
            final Component timerC = create(JavaTimer.class, se.sics.kompics.Init.NONE);
            final Component netC = create(DataNetwork.class, new DataNetwork.Init(new NetHook() {

                @Override
                public Component setupNetwork(ComponentProxy proxy) {
                    Component nettyC = create(NettyNetwork.class, new NettyInit(init.selfAddress));
                    return nettyC;
                }

                @Override
                public void connectTimer(ComponentProxy proxy, Component c) {
                    proxy.connect(timerC.getPositive(Timer.class), c.getNegative(Timer.class), Channel.TWO_WAY);
                }
            }));
            connect(netC.getPositive(Network.class), senderC.getNegative(Network.class), Channel.TWO_WAY);
        }

        public static class Init extends se.sics.kompics.Init<Parent> {

            public final Address selfAddress;
            public final Address targetAddress;

            public Init(Address selfAddress, Address targetAddress) {
                this.selfAddress = selfAddress;
                this.targetAddress = targetAddress;
            }
        }
    }

    public static class SenderC extends ComponentDefinition {

        private static final int DATA_VOLUME = 10000; // in #Messages

        final Positive<Network> net = requires(Network.class);

        private final Address selfAddress;
        private final Address targetAddress;
        private final HashMap<UUID, Data> outstanding = new HashMap<>();
        private byte[] data;
        private long startTS;

        public SenderC(Init init) {
            this.selfAddress = init.selfAddress;
            this.targetAddress = init.targetAddress;
            subscribe(startHandler, control);
            subscribe(respHandler, net);
            subscribe(prepHandler, net);
        }

        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                data = new byte[DATA_VOLUME * DataMessage.MESSAGE_SIZE];
                Random rand = new Random();
                rand.nextBytes(data);
                trigger(new Prepare(selfAddress, targetAddress, Transport.DATA, DATA_VOLUME), net);
                System.out.println("Preparing...");
            }
        };

        Handler<Prepared> prepHandler = new Handler<Prepared>() {

            @Override
            public void handle(Prepared event) {
                System.out.println("Prepared!");
                startTS = System.currentTimeMillis();
                for (int i = 0; i < DATA_VOLUME; i++) {
                    byte[] blob = new byte[DataMessage.MESSAGE_SIZE];
                    System.arraycopy(data, i, blob, 0, DataMessage.MESSAGE_SIZE);
                    Data msg = new Data(selfAddress, targetAddress, Transport.DATA, i, DATA_VOLUME, blob);
                    MessageNotify.Req req = MessageNotify.createWithDeliveryNotification(msg);
                    trigger(req, net);
                    outstanding.put(req.getMsgId(), msg);
                }
                data = null; // not needed anymore
            }
        };

        Handler<MessageNotify.Resp> respHandler = new Handler<MessageNotify.Resp>() {

            @Override
            public void handle(MessageNotify.Resp event) {
                Data dm = outstanding.get(event.msgId);
                if (dm != null) {
                    switch (event.getState()) {
                    case SENT: {
                        System.out.println("Message #" + dm.pos + " was sent.");
                    }
                        break;
                    case DELIVERED: {
                        System.out.println("Message #" + dm.pos + " was delivered.");
                        outstanding.remove(event.msgId);
                    }
                        break;
                    default: {
                        System.out.println("Message #" + dm.pos + " encountered a problem!");
                        outstanding.remove(event.msgId);
                    }
                        break;
                    }

                    if (outstanding.isEmpty()) {
                        long endTS = System.currentTimeMillis();
                        double diff = ((double) endTS - startTS) / 1000.0;
                        double throughput = ((double) (DATA_VOLUME * DataMessage.MESSAGE_SIZE)) / (diff * 1024.0);
                        System.out.println("Transfer complete! Data sent in " + diff + "s with " + throughput + "kb/s");
                        Kompics.asyncShutdown();
                    }
                } else {
                    throw new RuntimeException("Got a notify that wasn't outstanding! " + event);
                }
            }
        };

        public static class Init extends se.sics.kompics.Init<SenderC> {

            public final Address selfAddress;
            public final Address targetAddress;

            public Init(Address selfAddress, Address targetAddress) {
                this.selfAddress = selfAddress;
                this.targetAddress = targetAddress;
            }
        }
    }
}
