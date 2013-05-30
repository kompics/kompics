/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ControlPort;
import se.sics.kompics.Event;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Init.None;
import se.sics.kompics.Kompics;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class NetworkTest {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTest.class);
    //private static final String STARTED = "STARTED";
    private static final String STOPPED = "STOPPED";
    private static final String SENT = "SENT";
    private static final String RECEIVED = "RECEIVED";
    private static final String ACKED = "ACKED";
    private static final String FAIL = "FAIL";
    private static final int NUM_MESSAGES = 100;
    private static NetworkGenerator nGen;
    private static int numNodes;
    private static AtomicInteger msgId = new AtomicInteger(0);
    private static ConcurrentMap<Integer, String> messageStatus = new ConcurrentSkipListMap<Integer, String>();
    private static int startPort = 33000;
    
    public static synchronized void runTests(NetworkGenerator nGen, int numNodes) {
        NetworkTest.nGen = nGen;
        NetworkTest.numNodes = numNodes;

        msgId.set(0);
        messageStatus.clear();
        TestUtil.reset();

        Kompics.createAndStart(LauncherComponent.class, 8, 50);

        for (int i = 0; i < numNodes; i++) {
            TestUtil.waitFor(STOPPED);
        }
        Kompics.shutdown();

        assertEquals(NUM_MESSAGES * numNodes, messageStatus.size());
        for (String s : messageStatus.values()) {
            assertEquals(ACKED, s);
        }
    }

    public static class LauncherComponent extends ComponentDefinition {

        public LauncherComponent() {
            Address[] nodes = new Address[numNodes];
            InetAddress ip = null;
            try {
                ip = InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException ex) {
                LOG.error("Aborting test.", ex);
                System.exit(1);
            }
            for (int i = 0; i < numNodes; i++) {
                nodes[i] = new Address(ip, startPort + i, i);
                Component net = nGen.generate(myProxy, nodes[i]);
                Component scen = create(ScenarioComponent.class, new ScenarioInit(nodes[i], nodes));
                connect(net.provided(Network.class), scen.required(Network.class));
            }
            startPort = startPort + numNodes; // Don't start the same ports next time
            // Some network components shut down asynchronously -.-
        }
        private final ComponentProxy myProxy = new ComponentProxy() {
            @Override
            public <P extends PortType> void trigger(Event e, Port<P> p) {
                LauncherComponent.this.trigger(e, p);
            }

            @Override
            public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
                return LauncherComponent.this.create(definition, initEvent);
            }

            @Override
            public <T extends ComponentDefinition> Component create(Class<T> definition, None initEvent) {
                return LauncherComponent.this.create(definition, initEvent);
            }

            @Override
            public void destroy(Component component) {
                LauncherComponent.this.destroy(component);
            }

            @Override
            public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
                return LauncherComponent.this.connect(positive, negative);
            }

            @Override
            public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
                return LauncherComponent.this.connect(negative, positive);
            }

            @Override
            public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
                LauncherComponent.this.disconnect(negative, positive);
            }

            @Override
            public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
                LauncherComponent.this.disconnect(positive, negative);
            }

            @Override
            public Negative<ControlPort> getControlPort() {
                return LauncherComponent.this.control;
            }
        };
    }

    public static class ScenarioComponent extends ComponentDefinition {

        public final Address self;
        public final Address[] nodes;
        private final Positive<Network> net = requires(Network.class);
        private int msgCount = 0;
        private Random rand = new Random(0);

        public ScenarioComponent(ScenarioInit init) {
            self = init.self;
            nodes = init.nodes;

            Handler<Start> startHandler = new Handler<Start>() {
                @Override
                public void handle(Start event) {
                    int id = msgId.getAndIncrement();
                    trigger(new TestMessage(self, nodes[rand.nextInt(nodes.length)], id), net);
                    if (messageStatus.putIfAbsent(id, SENT) != null) {
                        LOG.error("Key {} was already present in messageStatus!", id);
                        TestUtil.submit(FAIL);
                    }
                    msgCount++;
                }
            };
            subscribe(startHandler, control);

            Handler<Ack> ackHandler = new Handler<Ack>() {
                @Override
                public void handle(Ack event) {
                    messageStatus.put(event.msgId, ACKED);


                    if (msgCount < NUM_MESSAGES) {
                        int id = msgId.getAndIncrement();
                        trigger(new TestMessage(self, nodes[rand.nextInt(nodes.length)], id), net);
                        if (messageStatus.putIfAbsent(id, SENT) != null) {
                            LOG.error("Key {} was already present in messageStatus!", id);
                            TestUtil.submit(FAIL);
                        }
                        msgCount++;
                    } else if (msgCount == NUM_MESSAGES) {
                        TestUtil.submit(STOPPED);
                    }
                }
            };
            subscribe(ackHandler, net);

            Handler<TestMessage> msgHandler = new Handler<TestMessage>() {
                @Override
                public void handle(TestMessage event) {
                    messageStatus.put(event.msgId, RECEIVED);
                    trigger(event.ack(), net);
                }
            };
            subscribe(msgHandler, net);
        }
    }

    public static class ScenarioInit extends Init<ScenarioComponent> {

        public final Address self;
        public final Address[] nodes;

        public ScenarioInit(Address self, Address[] nodes) {
            this.self = self;
            this.nodes = nodes;
        }
    }

    public static class TestMessage extends Message {

        public final int msgId;

        public TestMessage(Address src, Address dst, int id) {
            super(src, dst);
            this.msgId = id;
        }

        public Ack ack() {
            return new Ack(this.getDestination(), this.getSource(), msgId);
        }
    }

    public static class Ack extends Message {

        public final int msgId;

        public Ack(Address src, Address dst, int id) {
            super(src, dst);
            this.msgId = id;
        }
    }
}
