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

/**
 *
 * @author lkroll
 */
@RunWith(JUnit4.class)
public class LoopbackTest {

    static final Logger LOG = LoggerFactory.getLogger(LoopbackTest.class);

    static class TestEvent implements KompicsEvent {

        final int id;

        public TestEvent(int id) {
            this.id = id;
        }
    }

    static class TestPort extends PortType {

        {
            request(TestEvent.class);
        }
    }

    static class TestComponent extends ComponentDefinition {

        public TestComponent() {
            subscribe(startHandler, control);
            subscribe(testHandler, loopback);
        }
        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                trigger(new TestEvent(0), onSelf);
                stringQ.offer(SENT_TEST);
            }
        };
        Handler<TestEvent> testHandler = new Handler<TestEvent>() {

            @Override
            public void handle(TestEvent event) {
                stringQ.offer(GOT_TEST);
            }

        };
    }

    private static final BlockingQueue<String> stringQ = new LinkedBlockingQueue<String>();
    private static final String GOT_TEST = "GOT_TEST";
    private static final String SENT_TEST = "SENT_TEST";
    private static long timeout = 5000;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    @Test
    public void simpleTest() {
        Kompics.createAndStart(TestComponent.class);
        waitFor(SENT_TEST);
        waitFor(GOT_TEST);
        Kompics.shutdown();
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
