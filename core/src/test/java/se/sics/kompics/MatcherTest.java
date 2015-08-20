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
public class MatcherTest {

    private static final Logger LOG = LoggerFactory.getLogger(MatcherTest.class);

    private static final BlockingQueue<String> stringQ = new LinkedBlockingQueue<String>();
    private static long timeout = 5000;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private static final String SENT = "SENT";
    private static final String RECEIVED = "RECEIVED";

    @Test
    public void basicTest() {
        Kompics.createAndStart(DataParent.class);
        waitFor(SENT);
        waitFor(RECEIVED);
        Kompics.shutdown();
    }

    public static class Data {

    }

    public static class DataContainer implements PatternExtractor<Class<Data>, Data> {

        private final Data data;

        public DataContainer(Data data) {
            this.data = data;
        }

        @Override
        public Class<Data> extractPattern() {
            return Data.class;
        }

        @Override
        public Data extractValue() {
            return this.data;
        }

    }

    public static class DataPort extends PortType {

        {
            indication(DataContainer.class);
        }
    }

    public static class DataParent extends ComponentDefinition {

        Positive<DataPort> dp = requires(DataPort.class);

        public DataParent() {
            Component child = create(DataChild.class, Init.NONE);
            connect(this.dp.getPair(), child.getPositive(DataPort.class));
            
            subscribe(dataHandler, dp);
        }

        ClassMatchedHandler<Data, DataContainer> dataHandler = new ClassMatchedHandler<Data, DataContainer>() {

            @Override
            public void handle(Data content, DataContainer context) {
                if ((content != null) && (context != null)) {
                    stringQ.offer(RECEIVED);
                } else {
                    LOG.error("Expected data {} and context {}", content, context);
                    stringQ.offer("FAIL!");
                }
            }
        };
    }

    public static class DataChild extends ComponentDefinition {

        Negative<DataPort> dp = provides(DataPort.class);

        public DataChild() {
            subscribe(startHandler, control);
        }

        Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                stringQ.offer(SENT);
                trigger(new DataContainer(new Data()), dp);
            }
        };
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
