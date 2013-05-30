package se.sics.kompics.network.grizzly.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.grizzly.ConstantQuotaAllocator;
import se.sics.kompics.network.grizzly.GrizzlyNetwork;
import se.sics.kompics.network.grizzly.GrizzlyNetworkInit;
import se.sics.kompics.network.grizzly.kryo.KryoMessage;

public class TestClient extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args) {
        KryoMessage.register(TestMessage.class);

        Kompics.createAndStart(TestClient.class);
    }
    Address c = new Address(InetAddress.getByName("127.0.0.1"), 22334, (byte) 0x01);
    Address[] servers;
    Address mcast;
    Component grizzly;
    private int[] dataCounter;

    public TestClient() throws UnknownHostException {
        servers = new Address[TestServer.NUM_SERVERS];
        Address base = new Address(InetAddress.getByName("127.0.0.1"), 22333, (byte) 1);
        for (byte i = 0; i < TestServer.NUM_SERVERS; i++) {
            servers[i] = base.newVirtual(i);
        }


        GrizzlyNetworkInit init = new GrizzlyNetworkInit(c, 2, 0, 0, 2 * 1024, 16 * 1024,
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                new ConstantQuotaAllocator(50));
        grizzly = create(GrizzlyNetwork.class, init);
        
        subscribe(start, control);
        subscribe(h, grizzly.provided(Network.class));
    }
    Handler<Start> start = new Handler<Start>() {
        @Override
        public void handle(Start event) {

            dataCounter = new int[servers.length];
            Arrays.fill(dataCounter, 0);

            for (byte i = 0; i < servers.length; i++) {

                Random rand = new Random(1);
                byte[] spam = new byte[1000];
                rand.nextBytes(spam);

                String message = "Hello #" + i + "!!";

                TestMessage tm = new TestMessage(c, servers[i], message.getBytes());

                trigger(tm, grizzly.provided(Network.class));

//                try {
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    ObjectOutputStream oos;
//                    oos = new ObjectOutputStream(baos);
//                    oos.writeObject(tm);
//                    oos.flush();
//                    oos.close();
//
//                    byte[] buf = baos.toByteArray();
//
//                    System.err.println("Sent " + tm + " in " + buf.length
//                            + " bytes [" + new String(buf) + "]");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }
    };
    Handler<TestMessage> h = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage event) {
            if (event.getPayload().length > 20) {
                log.info("Received data #" + event.getSeq() + " with " + event.getPayload().length
                        + " bytes from " + event.getSource());

                byte serverId = event.getSource().getId()[0];
                dataCounter[serverId] = dataCounter[serverId] + 1;

                if (dataCounter[serverId] == TestServer.DATA_NUM) {
                    log.info("%%%%%%%%%%%%%%%%%%%%%%% \n"
                            + "Got all data from server " + event.getSource() + "\n"
                            + " %%%%%%%%%%%%%%%%%%%%%%% \n");
                }

                log.info(Arrays.toString(dataCounter));

            } else {
                log.info("Received #" + event.getSeq() + " with " + event.getPayload().length
                        + " bytes: " + new String(event.getPayload()));


                //String response = new String(event.getPayload());//+new String(event.getPayload());

                //trigger(new TestMessage(c, event.getSource(), "World".getBytes(), new byte[0], event.getSeq() + 1), grizzly.provided(Network.class));
            }
        }
    };
}
