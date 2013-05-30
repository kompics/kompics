package se.sics.kompics.network.grizzly.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.PropertyConfigurator;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.grizzly.GrizzlyNetwork;
import se.sics.kompics.network.grizzly.GrizzlyNetworkInit;
import se.sics.kompics.network.grizzly.kryo.KryoMessage;

public class PerfServer extends ComponentDefinition {

    static {
        PropertyConfigurator.configureAndWatch("log4j.properties");
    }

    public static void main(String[] args) {
        KryoMessage.register(TestMessage.class);

        Kompics.createAndStart(PerfServer.class);
    }
    Component grizzly;
    Address self;

    public PerfServer() throws UnknownHostException {


        try {
            String server = System.getProperty("PERF_SERVER");
            String address[] = server.split(":");

            InetAddress ip = InetAddress.getByName(address[0]);
            int port = Integer.parseInt(address[1]);

            System.err.println("Server listening on " + ip + ":" + port);

            self = new Address(ip, port, 0);

            grizzly = create(GrizzlyNetwork.class, new GrizzlyNetworkInit(self));
            subscribe(h, grizzly.provided(Network.class));
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize network", e);
        }
    }
    Handler<TestMessage> h = new Handler<TestMessage>() {
        public void handle(TestMessage event) {
            trigger(new TestMessage(self, event.getSource(), "World".getBytes()),
                    grizzly.provided(Network.class));
        }
    };
}
