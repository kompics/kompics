/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Fault.ResolveAction;

/**
 *
 * @author lkroll
 */
@RunWith(JUnit4.class)
public class FaultTest {

    private static final Logger LOG = LoggerFactory.getLogger(FaultTest.class);

    private static ResolveAction type;
    private static final BlockingQueue<String> stringQ = new LinkedBlockingQueue<String>();
    private static long timeout = 5000;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private static final String PARENT_HANDLED = "PARENT_HANDLED";
    private static final String GC_STARTED = "GC_STARTED";
    //private static final String TMSG = "TMSG";

    @Test
    public void escalateTest() {
        type = ResolveAction.ESCALATE;
        Kompics.createAndStart(ParentComponent.class);
        waitFor(GC_STARTED);
        waitFor(PARENT_HANDLED);
        Kompics.shutdown();
    }

    @Test
    public void ignoreTest() {
        type = ResolveAction.IGNORE;
        Kompics.createAndStart(ParentComponent.class);
        waitFor(GC_STARTED); // regular start

        waitFor(GC_STARTED); // resumed start
        Kompics.shutdown();
    }

    public static class ParentComponent extends ComponentDefinition {

        Component child;

        public ParentComponent() {
            child = create(ChildComponent.class, Init.NONE);
            subscribe(startHandler, control);
        }

        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                trigger(new TestEvent(), child.provided(TestPort.class));
            }
        };

        @Override
        public ResolveAction handleFault(Fault fault) {
            stringQ.offer(PARENT_HANDLED);
            return ResolveAction.DESTROY;
        }
    }

    public static class ChildComponent extends ComponentDefinition {

        Component child;

        Negative<TestPort> testport = provides(TestPort.class);

        public ChildComponent() {
            child = create(GrandChildComponent.class, Init.NONE);
            subscribe(startHandler, control);
        }

        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                trigger(new TestEvent(), child.provided(TestPort.class));
            }
        };

        @Override
        public ResolveAction handleFault(Fault fault) {
            switch (type) {
                case ESCALATE:
                    return ResolveAction.ESCALATE;
                case IGNORE:
                    return ResolveAction.IGNORE;
            }
            return ResolveAction.RESOLVED;
        }
    }

    public static class GrandChildComponent extends ComponentDefinition {

        Negative<TestPort> testport = provides(TestPort.class);

        public GrandChildComponent() {
            subscribe(testHandler, testport);
            subscribe(startHandler, control);
        }

        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                stringQ.offer(GC_STARTED);
            }
        };

        Handler<TestEvent> testHandler = new Handler<TestEvent>() {

            @Override
            public void handle(TestEvent event) {
                if (type == ResolveAction.ESCALATE) {
                    throw new TestError();
                }
                if (type == ResolveAction.IGNORE) {
                    throw new TestError();
                }
            }
        };
    }

    public static class TestPort extends PortType {

        {
            indication(KompicsEvent.class);
            request(KompicsEvent.class);
        }
    }

    public static class TestEvent implements KompicsEvent {

    }

    public static class TestError extends RuntimeException {

    }

    private static void waitFor(String s) {
        try {
            String qString = stringQ.poll(timeout, timeUnit);
            if (qString == null) {
                Assert.fail("Timeout on waiting for \'" + s + "\'");
            }
            Assert.assertEquals(s, qString);
        } catch (InterruptedException ex) {
            LOG.debug("Failed waiting for String: " + s, ex);
        }
    }

}
