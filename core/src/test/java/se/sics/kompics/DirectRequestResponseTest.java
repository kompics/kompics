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
public class DirectRequestResponseTest {

    private static final Logger LOG = LoggerFactory.getLogger(DirectRequestResponseTest.class);

    private static final BlockingQueue<String> stringQ = new LinkedBlockingQueue<String>();
    private static long timeout = 5000;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private static final long num = 500;
    private static volatile int batch = 1;

    private static final String START = "STARTED";
    private static final String END = "FINISHED";

    @Test
    public void basicTest() {
        Kompics.createAndStart(Parent.class);
        waitFor(START);
        waitFor(END);
        Kompics.shutdown();
    }

    @Test
    public void batchTest() {
        batch = 50;
        Kompics.createAndStart(Parent.class);
        waitFor(START);
        waitFor(END);
        Kompics.shutdown();
    }

    public static class Down extends Direct.Request {

        public final long id;

        public Down(long id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Down(" + id + ")";
        }
    }

    public static class Up implements Direct.Response {

        public final long id;

        public Up(long id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Up(" + id + ")";
        }
    }

    public static class PPPort extends PortType {

        {
            request(Down.class);
            indication(Up.class);
        }
    }

    public static class Parent extends ComponentDefinition {

        Component pinger = create(Pinger.class, Init.NONE);
        Component ponger = create(Ponger.class, Init.NONE);

        {
            connect(pinger.getNegative(PPPort.class), ponger.getPositive(PPPort.class));
        }
    }

    public static class Pinger extends ComponentDefinition {

        Positive<PPPort> p = requires(PPPort.class);
        Component starver = create(Starver.class, Init.NONE);

        private long scount = 0;
        private long rcount = 0;
        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                stringQ.offer(START);
                for (int i = 0; i < batch; i++) {
                    trigger(new Down(scount), p);
                    scount++;
                }
            }
        };
        Handler<Up> upHandler = new Handler<Up>() {

            @Override
            public void handle(Up event) {
                if (rcount != event.id) {
                    Assert.fail("Messages got misordered! Expected " + rcount + " but got " + event.id);
                }
                rcount++;
                if (rcount == scount) {
                    if (scount < num) {
                        for (int i = 0; i < batch; i++) {
                            trigger(new Down(scount), p);
                            scount++;
                        }
                    } else {
                        stringQ.offer(END);
                    }
                }
            }
        };

        {
            connect(p, starver.getNegative(PPPort.class));
            subscribe(startHandler, control);
            subscribe(upHandler, p);
        }
    }

    public static class Ponger extends ComponentDefinition {

        Negative<PPPort> p = provides(PPPort.class);

        Handler<Down> downHandler = new Handler<Down>() {

            @Override
            public void handle(Down event) {
                answer(event, new Up(event.id));
            }
        };

        {
            subscribe(downHandler, p);
        }
    }

    public static class Starver extends ComponentDefinition {

        Positive<PPPort> p = requires(PPPort.class);

        Handler<Up> upHandler = new Handler<Up>() {

            @Override
            public void handle(Up event) {
                Assert.fail("Direct.Response is not supposed to be forwarded!");
            }
        };

        {
            subscribe(upHandler, p);
        }
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
