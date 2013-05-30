package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.network.netty.NettyNetworkInit;

public class PerfServer extends ComponentDefinition {

    public static void main(String[] args) {
        Kompics.createAndStart(PerfServer.class);
    }
    Component grizzly;
    Address self;

    public PerfServer() throws UnknownHostException {
        self = new Address(InetAddress.getLocalHost(), 2222, 0);

        grizzly = create(NettyNetwork.class, new NettyNetworkInit(self, 3, 0));
        subscribe(h, grizzly.provided(Network.class));

    }
    Handler<TestMessage> h = new Handler<TestMessage>() {
        public void handle(TestMessage event) {
            trigger(new TestMessage(self, event.getSource(), "World".getBytes()),
                    grizzly.provided(Network.class));
        }
    };
}
