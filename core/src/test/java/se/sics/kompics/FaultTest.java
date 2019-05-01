/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
@RunWith(JUnit4.class)
public class FaultTest {

    private static final Logger LOG = LoggerFactory.getLogger(FaultTest.class);

    private static ResolveAction type;
    private static final BlockingQueue<String> stringQ = new LinkedBlockingQueue<String>();
    private static long timeout = 5000;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private static final String PARENT_HANDLED = "PARENT_HANDLED";
    private static final String TOP_HANDLED = "TOP_HANDLED";
    private static final String GC_STARTED = "GC_STARTED";
    // private static final String TMSG = "TMSG";

    @Test
    public void escalateTest() {
        type = ResolveAction.ESCALATE;
        LOG.info("Escalate Test: Starting Kompics...");
        Kompics.createAndStart(ParentComponent.class);
        LOG.info("Escalate Test: Waiting for GrandChild...");
        waitFor(GC_STARTED);
        LOG.info("Escalate Test: Waiting for Parent...");
        waitFor(PARENT_HANDLED);
        LOG.info("Escalate Test:Shutting down Kompics...");
        Kompics.shutdown();
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            Assert.fail(ex.getMessage());
        }
        LOG.info("Escalate Test: Kompics shut down.");
    }

    @Test
    public void ignoreTest() {
        type = ResolveAction.IGNORE;
        LOG.info("Ignore Test: Starting Kompics...");
        Kompics.createAndStart(ParentComponent.class);
        LOG.info("Ignore Test: Waiting for regular start...");
        waitFor(GC_STARTED); // regular start
        LOG.info("Ignore Test: Waiting for resume...");
        waitFor(GC_STARTED); // resumed start
        LOG.info("Ignore Test: Shutting down Kompics...");
        Kompics.shutdown();
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            Assert.fail(ex.getMessage());
        }
        LOG.info("Ignore Test: Kompics shut down.");
    }

    @Test
    public void parentFaultTest() {
        Kompics.setFaultHandler(new FaultHandler() {

            @Override
            public ResolveAction handle(Fault f) {
                stringQ.offer(TOP_HANDLED);
                return ResolveAction.DESTROY;
            }
        });
        LOG.info("Parent Fault Test: Starting Kompics...");
        Kompics.createAndStart(FailingParent.class);
        LOG.info("Parent Fault Test: Waiting for fault handling...");
        waitFor(TOP_HANDLED);
        LOG.info("Parent Fault Test: Waiting for shutdown...");
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            Assert.fail(ex.getMessage());
        }
        LOG.info("Parent Fault Test: Kompics shut down.");
    }

    public static class FailingParent extends ComponentDefinition {

        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                throw new TestError();
            }
        };

        {
            subscribe(startHandler, control);
        }
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
            default:
                return ResolveAction.RESOLVED;
            }
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

    @SuppressWarnings("serial")
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
