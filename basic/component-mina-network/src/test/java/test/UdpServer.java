package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;

public class UdpServer extends ComponentDefinition {

    public static void main(String[] args) {
        Kompics.createAndStart(UdpServer.class);
    }
    Component mina;

    public UdpServer() throws UnknownHostException {
        Address self = new Address(InetAddress.getLocalHost(), 2222, 0);
        mina = create(MinaNetwork.class, new MinaNetworkInit(self));
        subscribe(h, mina.provided(Network.class));


        // Address ms = new
        // Address(InetAddress.getByName(MulticastReceiver.ADDRESS),
        // MulticastReceiver.PORT,
        // 0);

    }
    Handler<TestUdpMessage> h = new Handler<TestUdpMessage>() {
        public void handle(TestUdpMessage event) {
            System.err.println("Received " + event.getPayload().length
                    + " bytes " + new String(event.getPayload()));
        }
    };
}
