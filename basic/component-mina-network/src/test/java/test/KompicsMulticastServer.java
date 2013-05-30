package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.JoinMulticastGroup;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;

public class KompicsMulticastServer extends ComponentDefinition {

    public static void main(String[] args) {
        Kompics.createAndStart(KompicsMulticastServer.class);
    }
    Component mina;

    public KompicsMulticastServer() throws UnknownHostException {
        Address self = new Address(InetAddress.getLocalHost(), 2222, 0);
        
        mina = create(MinaNetwork.class, new MinaNetworkInit(self, 1, 3344));
        subscribe(h, mina.provided(Network.class));


        InetAddress multicastGroup = InetAddress.getByName("239.240.241.242");
        trigger(new JoinMulticastGroup(self, multicastGroup), mina
                .provided(Network.class));

        System.out.println("Joined...");
    }
    Handler<TestMulticastMessage> h = new Handler<TestMulticastMessage>() {
        public void handle(TestMulticastMessage event) {
            System.err.println("Received " + event.getPayload().length
                    + " bytes " + new String(event.getPayload()));
        }
    };
}
