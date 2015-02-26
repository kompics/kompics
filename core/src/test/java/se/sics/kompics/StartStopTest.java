/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
@RunWith(JUnit4.class)
public class StartStopTest {

    private static BlockingQueue<KompicsEvent> queue;

    @Test
    public void testStartStop() {
        queue = new LinkedBlockingQueue<KompicsEvent>();
        Kompics.createAndStart(RootComponent.class, 4, 10); // totally arbitrary values
        try {
            KompicsEvent e = queue.take();
            assertTrue("First run started.", e instanceof TestReply);
            e = queue.take();
            assertTrue("First run stopped.", e instanceof Stopped);
            e = queue.take();
            assertTrue("Second run started.", e instanceof TestReply);
            e = queue.take();
            assertTrue("Second run stopped.", e instanceof Stopped);
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }

        Kompics.shutdown();

        Kompics.createAndStart(RootComponent.class, 4, 10); // totally arbitrary values
        try {
            KompicsEvent e = queue.take();
            assertTrue("Third run started.", e instanceof TestReply);
            e = queue.take();
            assertTrue("Third run stopped.", e instanceof Stopped);
            e = queue.take();
            assertTrue("Fourth run started.", e instanceof TestReply);
            e = queue.take();
            assertTrue("Fourth run stopped.", e instanceof Stopped);
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }

        Kompics.shutdown();

    }

    public static class TestPort extends PortType {

        {
            request(TestRequest.class);
            indication(TestReply.class);
        }
    }

    public static class TestRequest implements KompicsEvent {
    }

    public static class TestReply implements KompicsEvent {
    }

    public static class RootComponent extends ComponentDefinition {

        private boolean first = true;

        {
            final Positive<TestPort> port = requires(TestPort.class);

            final Component child = create(ForwarderComponent.class, Init.NONE);
            final Component child2 = create(ForwarderComponent.class, Init.NONE);

            connect(port.getPair(), child.getPositive(TestPort.class));

            Handler<TestReply> replyHandler = new Handler<TestReply>() {
                @Override
                public void handle(TestReply event) {
                    try {
                        queue.put(event);
                        if (first) {
                            trigger(Stop.event, child.control());
                        } else {
                            trigger(Stop.event, child2.control());
                        }
                    } catch (InterruptedException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            };

            Handler<Start> startHandler = new Handler<Start>() {

                @Override
                public void handle(Start event) {
                    trigger(new TestRequest(), port);
                }
            };
            Handler<Stopped> stoppedHandler = new Handler<Stopped>() {
                @Override
                public void handle(Stopped event) {
                    try {
                        if (first) {
                            first = false;
                            disconnect(port.getPair(), child.getPositive(TestPort.class));
                            destroy(child);
                            connect(port.getPair(), child2.getPositive(TestPort.class));
                            trigger(new TestRequest(), port);
                        } else {
                            disconnect(port.getPair(), child2.getPositive(TestPort.class));
                            destroy(child2);
                        }
                        queue.put(event);
                    } catch (InterruptedException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            };

            subscribe(replyHandler, port);
            subscribe(startHandler, control);
            subscribe(stoppedHandler, control);
        }
    }

    public static class ForwarderComponent extends ComponentDefinition {

        {
            final Positive<TestPort> down = requires(TestPort.class);
            final Negative<TestPort> up = provides(TestPort.class);

            Component child = create(ResponderComponent.class, Init.NONE);

            connect(down.getPair(), child.getPositive(TestPort.class));

            Handler<TestReply> replyHandler = new Handler<TestReply>() {
                @Override
                public void handle(TestReply event) {
                    trigger(event, up);
                }
            };

            subscribe(replyHandler, down);

            Handler<TestRequest> requestHandler = new Handler<TestRequest>() {
                @Override
                public void handle(TestRequest event) {
                    trigger(event, down);
                }
            };

            subscribe(requestHandler, up);
        }
    }

    public static class ResponderComponent extends ComponentDefinition {

        {
            final Negative<TestPort> port = provides(TestPort.class);

            Handler<TestRequest> requestHandler = new Handler<TestRequest>() {
                @Override
                public void handle(TestRequest event) {
                    trigger(new TestReply(), port);
                }
            };

            subscribe(requestHandler, port);
        }
    }
}
