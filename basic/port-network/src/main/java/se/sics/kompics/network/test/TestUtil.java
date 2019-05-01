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
package se.sics.kompics.network.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.KompicsEvent;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public abstract class TestUtil {

    private static final Logger log = LoggerFactory.getLogger(TestUtil.class);
    private static BlockingQueue<String> stringQ;
    private static BlockingQueue<KompicsEvent> eventQ;
    private static long timeout = 5000;
    private static String testDesc = "";
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    public static void reset(String testDescription) {
        stringQ = new LinkedBlockingQueue<String>();
        eventQ = new LinkedBlockingQueue<KompicsEvent>();
        testDesc = testDescription;
    }

    public static void reset(String testDescription, long timeout) {
        TestUtil.timeout = timeout;
        reset(testDescription);
    }

    public static void submit(String s) {
        try {
            if (!stringQ.offer(s, timeout, timeUnit)) {
                Assert.fail(testDesc + " -- " + "Timeout");
            }
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed on putting String: " + s, ex);
        }
    }

    public static void submit(KompicsEvent e) {
        try {
            if (!eventQ.offer(e, timeout, timeUnit)) {
                Assert.fail(testDesc + " -- " + "Timeout");
            }
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed on putting Event: " + e, ex);
        }
    }

    public static void waitFor(KompicsEvent e) {
        try {
            KompicsEvent qEvent = eventQ.poll(timeout, timeUnit);
            if (qEvent == null) {
                Assert.fail(testDesc + " -- " + "Timeout");
            }
            Assert.assertEquals(e, qEvent);
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed waiting for Event: " + e, ex);
        }
    }

    public static void waitFor(Class<? extends KompicsEvent> eventType) {
        try {
            KompicsEvent qEvent = eventQ.poll(timeout, timeUnit);
            if (qEvent == null) {
                Assert.fail(testDesc + " -- " + "Timeout");
            }
            Assert.assertTrue(eventType.isInstance(qEvent));
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed waiting for Event of Type: " + eventType, ex);
        }
    }

    public static void waitFor(String s) {
        try {
            String qString = stringQ.poll(timeout, timeUnit);
            if (qString == null) {
                Assert.fail(testDesc + " -- " + "Timeout on waiting for \'" + s + "\'");
            }
            Assert.assertEquals(s, qString);
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed waiting for String: " + s, ex);
        }
    }

    public static void waitForAll(KompicsEvent... events) {
        ArrayList<KompicsEvent> el = new ArrayList<KompicsEvent>();
        for (KompicsEvent e : events) {
            el.add(e);
        }
        try {
            while (!el.isEmpty()) {
                KompicsEvent qEvent = eventQ.poll(timeout, timeUnit);
                if (qEvent == null) {
                    Assert.fail(testDesc + " -- " + "Timeout");
                }
                if (el.contains(qEvent)) {
                    el.remove(qEvent);
                } else {
                    Assert.fail(testDesc + " -- " + "Unexpected event: " + qEvent);
                }
            }
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed waiting for Events.", ex);
        }
    }

    @SafeVarargs
    public static void waitForAll(Class<? extends KompicsEvent>... eventTypes) {
        ArrayList<Class<? extends KompicsEvent>> el = new ArrayList<Class<? extends KompicsEvent>>();
        for (Class<? extends KompicsEvent> e : eventTypes) {
            el.add(e);
        }
        try {
            while (!el.isEmpty()) {
                KompicsEvent qEvent = eventQ.poll(timeout, timeUnit);
                if (qEvent == null) {
                    Assert.fail(testDesc + " -- " + "Timeout");
                }
                Iterator<Class<? extends KompicsEvent>> it = el.iterator();
                boolean found = false;
                while (it.hasNext()) {
                    Class<? extends KompicsEvent> eventType = it.next();
                    if (eventType.isInstance(qEvent)) {
                        it.remove();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Assert.fail(testDesc + " -- " + "Unexpected event: " + qEvent);
                }
            }
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed waiting for Events.", ex);
        }
    }

    public static void waitForAll(String... strings) {
        ArrayList<String> sl = new ArrayList<String>();
        for (String s : strings) {
            sl.add(s);
        }
        try {
            while (!sl.isEmpty()) {
                String qString = stringQ.poll(timeout, timeUnit);
                if (qString == null) {
                    Assert.fail(testDesc + " -- " + "Timeout");
                }
                if (sl.contains(qString)) {
                    sl.remove(qString);
                    System.out.println("Got " + qString);
                } else {
                    Assert.fail(testDesc + " -- " + "Unexpected key: " + qString);
                }
            }
        } catch (InterruptedException ex) {
            log.debug(testDesc + " -- " + "Failed waiting for Events.", ex);
        }
    }
}
