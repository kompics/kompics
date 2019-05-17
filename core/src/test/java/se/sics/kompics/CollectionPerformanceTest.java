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

import com.google.common.collect.ArrayListMultimap;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class CollectionPerformanceTest {

    static final long LOOKUPS = 100 * 1000 * 1000;

    public CollectionPerformanceTest() {
    }

    @Test
    public void hashMapTest() {
        TimedTest test = new TimedTest() {

            private final HashMap<Class<? extends KompicsEvent>, ArrayList<Handler<?>>> subs = new HashMap<>();

            @Override
            public void runMeasurement() {
                for (long i = 0; i < LOOKUPS; i++) {
                    ArrayList<Handler<?>> handlers = subs.get(TestEvent.class);
                    assert (handlers != null);
                }
            }

            @Override
            public String name() {
                return "HashMap";
            }

            @Override
            public void setUp() {
                for (Handler<? extends KompicsEvent> h : testHandlers) {
                    Class<? extends KompicsEvent> eType = fixEventType(h);
                    ArrayList<Handler<?>> handlers = subs.get(eType);
                    if (handlers == null) {
                        handlers = new ArrayList<>();
                        handlers.add(h);
                        subs.put(eType, handlers);
                    } else {
                        handlers.add(h);
                    }
                }
            }

        };
        long res = test.run();
        printResult(test.name(), res);
    }

    @Test
    public void guavaArrayListMultimapTest() {
        TimedTest test = new TimedTest() {

            private final ArrayListMultimap<Class<? extends KompicsEvent>, Handler<?>> subs = ArrayListMultimap
                    .create();

            @Override
            public void runMeasurement() {
                for (long i = 0; i < LOOKUPS; i++) {
                    List<Handler<?>> handlers = subs.get(TestEvent.class);
                    assert (handlers != null);
                }
            }

            @Override
            public String name() {
                return "Guava ArrayListMultimap";
            }

            @Override
            public void setUp() {
                for (Handler<? extends KompicsEvent> h : testHandlers) {
                    subs.put(fixEventType(h), h);
                }
            }

        };
        long res = test.run();
        printResult(test.name(), res);
    }

    private static void printResult(String name, long nanos) {
        double t = (double) nanos * (1e-6); // ms
        double ops = LOOKUPS / t;
        System.out.println("Test '" + name + "' achieved " + String.format("%.3e", ops) + "op/ms");
    }

    static interface PerformanceTest {

        public String name();

        public long run();
    }

    static abstract class TimedTest implements PerformanceTest {

        public abstract void setUp();

        public abstract void runMeasurement();

        @Override
        public long run() {
            setUp();
            ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
            if (tmxb.isCurrentThreadCpuTimeSupported()) {
                tmxb.setThreadCpuTimeEnabled(true);
                long start = tmxb.getCurrentThreadCpuTime();
                this.runMeasurement();
                long end = tmxb.getCurrentThreadCpuTime();
                return end - start;
            } else {
                System.err.println("ThreadMXBean CPU Time is not supported. Using System.nanaTime() instead.");
                long start = System.nanoTime();
                this.runMeasurement();
                long end = System.nanoTime();
                return end - start;
            }
        }

        protected <E extends KompicsEvent> Class<E> fixEventType(Handler<E> h) {
            Class<E> eventType = h.getEventType();
            if (h.eventType == null) {
                eventType = reflectEventType(h.getClass());
                h.setEventType(eventType);
            }
            return eventType;
        }

        @SuppressWarnings("unchecked")
        protected <E extends KompicsEvent> Class<E> reflectEventType(Class<?> handlerC) {
            Class<E> eventType = null;
            try {
                Method declared[] = handlerC.getDeclaredMethods();
                // The JVM in Java 7 wrongly reflects the "handle" methods for some
                // handlers: e.g. both `handle(Event e)` and `handle(Message m)` are
                // reflected as "declared" methods when only the second is actually
                // declared in the handler. A workaround is to reflect all `handle`
                // methods and pick the one with the most specific event type.
                // This sorted set stores the event types of all reflected handler
                // methods topologically ordered by the event type relationships.
                TreeSet<Class<? extends KompicsEvent>> relevant = new TreeSet<Class<? extends KompicsEvent>>(
                        new Comparator<Class<? extends KompicsEvent>>() {
                            @Override
                            public int compare(Class<? extends KompicsEvent> e1, Class<? extends KompicsEvent> e2) {
                                if (e1.isAssignableFrom(e2)) {
                                    return 1;
                                } else if (e2.isAssignableFrom(e1)) {
                                    return -1;
                                }
                                return 0;
                            }
                        });
                for (Method m : declared) {
                    if (m.getName().equals("handle")) {
                        relevant.add((Class<? extends KompicsEvent>) m.getParameterTypes()[0]);
                    }
                }
                eventType = (Class<E>) relevant.first();
            } catch (Exception e) {
                throw new RuntimeException("Cannot reflect handler event type for " + "handler " + handlerC
                        + ". Please specify it " + "as an argument to the handler constructor.", e);
            }
            if (eventType == null) {
                throw new RuntimeException("Cannot reflect handler event type for handler " + handlerC
                        + ". Please specify it " + "as an argument to the handler constructor.");
            } else {
                return eventType;
            }
        }
    }

    static class TestEvent implements KompicsEvent {

        static final TestEvent event = new TestEvent();

        private TestEvent() {
        }
    }

    static class TestEvent2 implements KompicsEvent {

        static final TestEvent2 event = new TestEvent2();

        private TestEvent2() {
        }
    }

    static class TestEvent3 implements KompicsEvent {

        static final TestEvent3 event = new TestEvent3();

        private TestEvent3() {
        }
    }

    static class TestEvent4 implements KompicsEvent {

        static final TestEvent4 event = new TestEvent4();

        private TestEvent4() {
        }
    }

    static class TestEvent5 implements KompicsEvent {

        static final TestEvent5 event = new TestEvent5();

        private TestEvent5() {
        }
    }

    static final Handler<TestEvent> testHandler1 = new Handler<TestEvent>() {

        @Override
        public void handle(TestEvent event) {
            // do nothing
        }
    };
    static final Handler<TestEvent> testHandler2 = new Handler<TestEvent>() {

        @Override
        public void handle(TestEvent event) {
            // do nothing
        }
    };
    static final Handler<TestEvent2> test2Handler1 = new Handler<TestEvent2>() {

        @Override
        public void handle(TestEvent2 event) {
            // do nothing
        }
    };
    static final Handler<TestEvent3> test3Handler1 = new Handler<TestEvent3>() {

        @Override
        public void handle(TestEvent3 event) {
            // do nothing
        }
    };
    static final Handler<TestEvent4> test4Handler1 = new Handler<TestEvent4>() {

        @Override
        public void handle(TestEvent4 event) {
            // do nothing
        }
    };
    static final Handler<TestEvent5> test5Handler1 = new Handler<TestEvent5>() {

        @Override
        public void handle(TestEvent5 event) {
            // do nothing
        }
    };
    static final List<Handler<? extends KompicsEvent>> testHandlers = new LinkedList<>();

    static {
        testHandlers.add(testHandler1);
        testHandlers.add(testHandler2);
        testHandlers.add(test2Handler1);
        testHandlers.add(test3Handler1);
        testHandlers.add(test4Handler1);
        testHandlers.add(test5Handler1);
    }
}
