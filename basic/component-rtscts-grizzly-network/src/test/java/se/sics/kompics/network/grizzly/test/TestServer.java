package se.sics.kompics.network.grizzly.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.DataMessage;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.RequestToSend;
import se.sics.kompics.network.VirtualNetworkChannel;
import se.sics.kompics.network.grizzly.ConstantQuotaAllocator;
import se.sics.kompics.network.grizzly.GrizzlyNetwork;
import se.sics.kompics.network.grizzly.GrizzlyNetworkInit;
import se.sics.kompics.network.grizzly.kryo.KryoMessage;

public class TestServer extends ComponentDefinition {

    public static final byte NUM_SERVERS = 32;
    private static final Logger log = LoggerFactory.getLogger(TestServer.class);
    private static final Random RAND = new Random(0);
    private static final int DATA_SIZE = 10000;
    public static final int DATA_NUM = 1000;

    public static void main(String[] args) {
        KryoMessage.register(TestMessage.class);

        Kompics.createAndStart(MainComponent.class);
    }

    public static class MainComponent extends ComponentDefinition {

        Component grizzly;

        public MainComponent() throws UnknownHostException {

            Address netSelf = new Address(InetAddress.getByName("127.0.0.1"), 22333, null);
            GrizzlyNetworkInit init = new GrizzlyNetworkInit(netSelf, 8, 0, 0, 2 * 1024, 16 * 1024,
                    Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(),
                    new ConstantQuotaAllocator(5));
            grizzly = create(GrizzlyNetwork.class, init);

            VirtualNetworkChannel vnc = VirtualNetworkChannel.connect(grizzly.getPositive(Network.class));

            for (byte i = 0; i < NUM_SERVERS; i++) {
                Address addr = netSelf.newVirtual(i);
                Component server = create(TestServer.class, new TSInit(addr));

                vnc.addConnection(addr.getId(), server.getNegative(Network.class));
            }
        }
    }
    Positive<Network> net = requires(Network.class);
    private Address self;
    private int dataCount = 0;
    private boolean firstMessage = true;

    public TestServer(TSInit event) {
        subscribe(h, net);
        subscribe(sh, control);
        subscribe(ctsh, net);

        // INIT
        self = event.addr;
    }
    Handler<Start> sh = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("Starting server: " + self.toString());
        }
    };
    Handler<TestMessage> h = new Handler<TestMessage>() {
        @Override
        public void handle(TestMessage event) {
            if (event.getPayload().length > 20) {
                log.info("Received data #" + event.getSeq() + " with " + event.getPayload().length
                        + " bytes");
            } else {
                log.info("Received #" + event.getSeq() + " with " + event.getPayload().length
                        + " bytes: " + new String(event.getPayload()));
            }

            int paddingSize = RAND.nextInt(DATA_SIZE);
            byte[] padding = new byte[paddingSize];
            RAND.nextBytes(padding);
            //String response = new String(event.getPayload());//+new String(event.getPayload());
            //log.debug("Addresses equal? " + self.compareTo(event.getDestination()));
            trigger(new TestMessage(self, event.getSource(), "World".getBytes(), padding, event.getSeq() + 1), net);

            if (firstMessage) {
                firstMessage = false;
                RequestToSend rts = new RequestToSend();
                TestCTS tcts = new TestCTS(event.getSource(), self, rts);
                rts.setEvent(tcts);
                trigger(rts, net);
            }

        }
    };
    Handler<TestCTS> ctsh = new Handler<TestCTS>() {
        @Override
        public void handle(TestCTS event) {
            int quota = event.getQuota();
            log.info("Sending {} data packages from {} to {} on reqId={} and flowId={}.", new Object[]{quota, self, event.getSource(), event.getRequestId(), event.getFlowId()});
            for (int i = quota; i > 0; i--) {
                dataCount++;
                byte[] data = new byte[DATA_SIZE];
                RAND.nextBytes(data);
                TestMessage msg = new TestMessage(self, event.getSource(), data, new byte[0], dataCount);
                DataMessage dmsg = new DataMessage(event.getFlowId(), event.getRequestId(), msg);

                if (dataCount == DATA_NUM) {
                    dmsg.setFinal();
                    trigger(dmsg, net);
                    break;
                }
                trigger(dmsg, net);

            }
            if (dataCount == DATA_NUM) {
                log.info("##################################### \n"
                        + self + " finished sending data. \n"
                        + "##################################### \n");
            }
        }
    };

    private static class TSInit extends Init {

        public final Address addr;

        public TSInit(Address addr) {
            this.addr = addr;
        }
    }
}
