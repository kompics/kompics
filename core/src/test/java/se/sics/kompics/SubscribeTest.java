/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import org.junit.Assert;
import org.junit.Test;

/**
 * The
 * <code>SubscribeTest</code> class tests dynamic subscriptions.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
@SuppressWarnings("unused")
public class SubscribeTest {

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

    private static class TestRoot1 extends ComponentDefinition {

        public TestRoot1() {
            Component component1 = create(TestComponent1.class, Init.NONE);

            for (int i = 0; i < EVENT_COUNT; i++) {
                trigger(new TestEvent(i), component1.getPositive(TestPort.class));
            }
        }
    }

    private static class TestComponent1 extends ComponentDefinition {

        public TestComponent1() {
            Negative<TestPort> testPort = provides(TestPort.class);
            subscribe(testHandler, testPort);
        }
        Handler<TestEvent> testHandler = new Handler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                list1.add(event.id);
                semaphore.release();
            }
        };
    }
    private static final int EVENT_COUNT = 16;
    private static LinkedList<Integer> list1, list2;
    private static Semaphore semaphore;

    /**
     * Tests FIFO handling of events.
     *
     * @throws Exception the exception
     */
    @Test
    public void testFifoExecution() throws Exception {
        semaphore = new Semaphore(0);
        list1 = new LinkedList<Integer>();
        Integer expected[] = new Integer[EVENT_COUNT];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
        }

        Kompics.createAndStart(TestRoot1.class, 1);

        semaphore.acquire(EVENT_COUNT);

        Assert.assertArrayEquals(expected, list1.toArray());
        Kompics.shutdown();
    }

    private static class TestRoot2 extends ComponentDefinition {

        public TestRoot2() {
            Component component2 = create(TestComponent2.class, Init.NONE);

            subscribe(faultHandler, component2.getControl());

            for (int i = 0; i < EVENT_COUNT; i++) {
                trigger(new TestEvent(i), component2.getPositive(TestPort.class));
            }
        }
        Handler<Fault> faultHandler = new Handler<Fault>() {
            @Override
            public void handle(Fault event) {
                semaphore.release(EVENT_COUNT);
                // event.getFault().printStackTrace();
            }
        };
    }

    private static class TestComponent2 extends ComponentDefinition {

        private final Negative<TestPort> testPort = provides(TestPort.class);
        
        public TestComponent2() {
            
            subscribe(testHandler1, testPort);
        }
        Handler<TestEvent> testHandler1 = new Handler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                list2.add(event.id);

                unsubscribe(testHandler1, testPort);
                subscribe(testHandler2, testPort);

                semaphore.release();
            }
        };
        Handler<TestEvent> testHandler2 = new Handler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                list2.add(event.id);

                unsubscribe(testHandler2, testPort);
                subscribe(testHandler3, testPort);

                semaphore.release();
            }
        };
        Handler<TestEvent> testHandler3 = new Handler<TestEvent>() {
            @Override
            public void handle(TestEvent event) {
                list2.add(event.id);

                unsubscribe(testHandler3, testPort);
                subscribe(testHandler1, testPort);

                semaphore.release();
            }
        };
    }

    /**
     * Tests FIFO handling of events with dynamic subscriptions.
     *
     * @throws Exception the exception
     */
    @Test
    public void testFifoDynamicSubscriptions() throws Exception {
        semaphore = new Semaphore(0);
        list2 = new LinkedList<Integer>();
        Integer expected[] = new Integer[EVENT_COUNT];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
        }

        Kompics.createAndStart(TestRoot2.class, 1);

        semaphore.acquire(EVENT_COUNT);

        Assert.assertArrayEquals(expected, list2.toArray());
        Kompics.shutdown();
    }
}
