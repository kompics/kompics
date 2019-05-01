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

import java.util.concurrent.Semaphore;
import org.junit.Test;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
@SuppressWarnings("deprecation")
public class HandlerRequestResponseTest {

    static class TestRequest extends Request {

        final int id;

        public TestRequest(int id) {
            this.id = id;
        }
    }

    static class TestResponse extends Response {

        final int id;

        public TestResponse(TestRequest request, int id) {
            super(request);
            this.id = id;
        }
    }

    static class TestPort extends PortType {

        {
            negative(TestRequest.class);
            positive(TestResponse.class);
        }
    }

    static class TestRoot1 extends ComponentDefinition {

        private Component component1;

        public TestRoot1() {
            component1 = create(TestComponent1.class, Init.NONE);

            subscribe(startHandler, control);
            subscribe(testResponse, component1.getPositive(TestPort.class));

        }

        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                TestRequest request = new TestRequest(12);
                // System.err.println("Root triggered request " + request.id);

                trigger(request, component1.getPositive(TestPort.class));
            }
        };

        Handler<TestResponse> testResponse = new Handler<TestResponse>() {
            public void handle(TestResponse event) {
                // System.err.println("Root got response " + event.id);
                semaphore.release(1);
            }
        };
    }

    static class TestComponent1 extends ComponentDefinition {

        Negative<TestPort> testPort = negative(TestPort.class);
        Component child;

        public TestComponent1() {
            child = create(TestComponent2.class, Init.NONE);

            subscribe(testRequest, testPort);
            subscribe(testResponse, child.getPositive(TestPort.class));
        }

        Handler<TestRequest> testRequest = new Handler<TestRequest>() {
            public void handle(TestRequest event) {
                trigger(event, child.getPositive(TestPort.class));
            }
        };

        Handler<TestResponse> testResponse = new Handler<TestResponse>() {
            public void handle(TestResponse event) {
                trigger(event, testPort);
            }
        };
    }

    static class TestComponent2 extends ComponentDefinition {

        Negative<TestPort> testPort = negative(TestPort.class);

        public TestComponent2() {
            subscribe(testRequest, testPort);
        }

        Handler<TestRequest> testRequest = new Handler<TestRequest>() {
            public void handle(TestRequest event) {
                TestResponse response = new TestResponse(event, event.id);
                trigger(response, testPort);
            }
        };
    }

    private static final int EVENT_COUNT = 1;
    private static Semaphore semaphore;

    @Test
    public void testHandlerRequestResponse() throws Exception {
        semaphore = new Semaphore(0);

        Kompics.createAndStart(TestRoot1.class, 1);

        semaphore.acquire(EVENT_COUNT);

        Kompics.shutdown();
    }
}
