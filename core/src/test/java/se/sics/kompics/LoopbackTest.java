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

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
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
