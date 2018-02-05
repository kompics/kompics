package se.sics.kompics;

import java.util.concurrent.Semaphore;
import org.junit.Ignore;

public class ChannelRequestResponseTest {

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

            subscribe(testResponse, component1.getPositive(TestPort.class));
            subscribe(startHandler, control);

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

            connect(testPort, child.getPositive(TestPort.class));

            //trigger(new TestRequest(13), child.getPositive(TestPort.class));
        }
    }

    static class TestComponent2 extends ComponentDefinition {

        Negative<TestPort> testPort = negative(TestPort.class);

        public TestComponent2() {
            subscribe(testRequest, testPort);
        }
        /* java 8        
         Handler<TestRequest> h = handle(testPort, TestRequest.class, event -> {
         TestResponse response = new TestResponse(event, event.id);
         trigger(response, testPort);
         });
         */
        Handler<TestRequest> testRequest = new Handler<TestRequest>() {
            @Override
            public void handle(TestRequest event) {

                // System.err.println("Handling request " + event.id);
                TestResponse response = new TestResponse(event, event.id);
                trigger(response, testPort);
            }
        };
    }
    private static final int EVENT_COUNT = 1;
    private static Semaphore semaphore;

    @Ignore
    public void testHandlerRequestResponse() throws Exception {
        semaphore = new Semaphore(0);

        Kompics.createAndStart(TestRoot1.class, 1);

        semaphore.acquire(EVENT_COUNT);

        Kompics.shutdown();
    }
}
