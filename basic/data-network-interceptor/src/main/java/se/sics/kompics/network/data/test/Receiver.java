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
import java.util.TreeSet;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Address;
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
public class Receiver {
    
    static {
        Serializers.register(new DataMessageSerialiser(), "dmS");
        Serializers.register(DataMessage.class, "dmS");
    }

    public static void main(String[] args) throws UnknownHostException {
        InetAddress ip = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);
        Address selfAddress = new NettyAddress(ip, port);
        Kompics.createAndStart(Parent.class, new Parent.Init(selfAddress), 2, 50);
    }

    public static class Parent extends ComponentDefinition {

        public Parent(final Init init) {
            System.out.println("Creating receiver at " + init.selfAddress);
            final Component recvC = create(ReceiverC.class, new ReceiverC.Init(init.selfAddress));
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
            connect(netC.getPositive(Network.class), recvC.getNegative(Network.class), Channel.TWO_WAY);
        }

        public static class Init extends se.sics.kompics.Init<Parent> {

            public final Address selfAddress;

            public Init(Address selfAddress) {
                this.selfAddress = selfAddress;
            }
        }
    }

    public static class ReceiverC extends ComponentDefinition {

        final Positive<Network> net = requires(Network.class);
        
        private final Address selfAddress;
        private final TreeSet<Integer> tracker = new TreeSet<>();
        private byte[] data;
        private long startTS;
        private boolean first = true;
        
        
        public ReceiverC(Init init) {
            this.selfAddress = init.selfAddress;
            subscribe(dataHandler, net);
            subscribe(prepHandler, net);
        }
        
        Handler<Data> dataHandler = new Handler<Data>() {

            @Override
            public void handle(Data event) {
                if (first) {
                    startTS = System.currentTimeMillis();
                    first = false;
                }
                System.arraycopy(event.data, 0, data, event.pos, DataMessage.MESSAGE_SIZE);
                tracker.add(event.pos);
                System.out.println("Received message #"+event.pos);
                if (tracker.size() == event.total) {
                    long endTS = System.currentTimeMillis();
                    double diff = ((double) endTS - startTS)/1000.0;
                    double throughput = ((double)data.length)/(diff*1024.0);
                    System.out.println("Transfer complete! Data received in " + diff + "s with " + throughput + "kb/s");
                    Kompics.asyncShutdown();
                }
            }
        };
        
        Handler<Prepare> prepHandler = new Handler<Prepare>() {

            @Override
            public void handle(Prepare event) {
                data = new byte[DataMessage.MESSAGE_SIZE*event.volume];
                trigger(new Prepared(selfAddress, event.getSource(), Transport.TCP), net);
            }
        };
        
        public static class Init extends se.sics.kompics.Init<ReceiverC> {

            public final Address selfAddress;

            public Init(Address selfAddress) {
                this.selfAddress = selfAddress;
            }
        }
    }
}
