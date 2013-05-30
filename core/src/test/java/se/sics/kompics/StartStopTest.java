/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
@RunWith(JUnit4.class)
public class StartStopTest {

    private static BlockingQueue<Event> queue;

    @Test
    public void testStartStop() {
        queue = new LinkedBlockingQueue<Event>();
        Kompics.createAndStart(RootComponent.class, 4, 10); // totally arbitrary values
        try {
            Event e = queue.take();
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
            Event e = queue.take();
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

    public static class TestRequest extends Event {
    }

    public static class TestReply extends Event {
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
            subscribe(stoppedHandler, control);

            trigger(new TestRequest(), port);
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
