package se.sics.kompics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.NatNetworkControl;
import se.sics.gvod.net.events.PortAllocRequest;
import se.sics.gvod.address.Address;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.NettyInit;
import se.sics.gvod.net.NettyNetwork;
import se.sics.gvod.net.Transport;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timer;
import se.sics.gvod.timer.java.JavaTimer;

/**
 * Unit test for simple App.
 */
public class PortAllocTest
        extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(PortAllocTest.class);
    private boolean testStatus = true;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PortAllocTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(PortAllocTest.class);
    }

    public static void setTestObj(PortAllocTest testObj) {
        TestStClientComponent.testObj = testObj;
    }


    public static class TestStClientComponent extends ComponentDefinition {

        private Component netty;
        private Component timer;
        private static PortAllocTest testObj = null;
        private Address self;

        public TestStClientComponent() {
            timer = create(JavaTimer.class);
            netty = create(NettyNetwork.class);

            subscribe(handleStart, control);
            subscribe(handleMsgTimeout, timer.getPositive(Timer.class));
            subscribe(handlePortAllocResponse, netty.getPositive(NatNetworkControl.class));
//            subscribe(handlePortBindResponse, netty.getPositive(NatNetworkControl.class));

            InetAddress addr = null;
            int port = 13478;

            try {
                addr = InetAddress.getLocalHost();

            } catch (UnknownHostException ex) {
                logger.error("UnknownHostException");
                testObj.fail();
            }

            self = new Address(addr, port, 1);


            trigger(new NettyInit(12312, true, VodMsgFrameDecoder.class), netty.getControl());

        }
        public Handler<Start> handleStart = new Handler<Start>() {

            public void handle(Start event) {
                ScheduleTimeout st = new ScheduleTimeout(2000);
                MsgTimeout mt = new MsgTimeout(st);
                st.setTimeoutEvent(mt);
                PortAllocRequest req  = new PortAllocRequest(self.getIp(), 
                        self.getId(), 2, Transport.UDP);
                MyPortAllocResponse resp = new MyPortAllocResponse(req, new Integer(1));
                req.setResponse(resp);
                trigger(req, netty.getPositive(NatNetworkControl.class));
                trigger(st, timer.getPositive(Timer.class));
            }
        };
        public Handler<MyPortAllocResponse> handlePortAllocResponse = new Handler<MyPortAllocResponse>() {
            @Override
            public void handle(MyPortAllocResponse event) {
                trigger(new Stop(), netty.getControl());
                if (event.getAllocatedPorts().size() == 2) {
                    System.out.println("Got port alloc response!");
                    testObj.pass();
                } else {
                    testObj.fail(true);
                    System.out.println("No port alloc response!");
                }
            }
        };
        public Handler<MsgTimeout> handleMsgTimeout = new Handler<MsgTimeout>() {
            @Override
            public void handle(MsgTimeout event) {
                trigger(new Stop(), netty.getControl());
                System.out.println("Msg timeout");
                testObj.testStatus = false;
                testObj.fail(true);
            }
        };
    }
    private static final int EVENT_COUNT = 1;
    private static Semaphore semaphore = new Semaphore(0);

    private void allTests() {
        int i = 0;

        runInstance();
        if (testStatus == true) {
            assertTrue(true);
        }
    }

    private void runInstance() {
        Kompics.createAndStart(TestStClientComponent.class, 1);
        try {
            PortAllocTest.semaphore.acquire(EVENT_COUNT);
            System.out.println("Finished test.");
        } catch (InterruptedException e) {
            assert (false);
        } finally {
            Kompics.shutdown();
        }
        if (testStatus == false) {
            assertTrue(false);
        }

    }

    @org.junit.Ignore
    public void testApp() {
        setTestObj(this);

        allTests();
    }

    public void pass() {
        PortAllocTest.semaphore.release();
    }

    public void fail(boolean release) {
        testStatus = false;
        PortAllocTest.semaphore.release();
    }
}
