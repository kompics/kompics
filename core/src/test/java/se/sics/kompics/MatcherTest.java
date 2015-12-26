/**
 * This file is part of the Kompics component model runtime.
 * <p>
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
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

    public interface Data {

    }

    public static class CData implements Data {

    }

    public static class FData implements Data {

    }

    public static class DataContainer implements PatternExtractor<Class, Data> {

        private final Data data;

        public DataContainer(Data data) {
            this.data = data;
        }

        @Override
        public Class extractPattern() {
            return data.getClass();
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

            subscribe(falseDataHandler, dp);
            subscribe(dataHandler, dp);
        }

        ClassMatchedHandler<CData, DataContainer> dataHandler = new ClassMatchedHandler<CData, DataContainer>() {

            @Override
            public void handle(CData content, DataContainer context) {
                if ((content != null) && (context != null)) {
                    stringQ.offer(RECEIVED);
                } else {
                    LOG.error("Expected data {} and context {}", content, context);
                    stringQ.offer("FAIL!");
                }
            }
        };

        ClassMatchedHandler<FData, DataContainer> falseDataHandler = new ClassMatchedHandler<FData, DataContainer>() {

            @Override
            public void handle(FData content, DataContainer context) {
                LOG.error("Only CData handlers should be triggered, not FData!");
                stringQ.offer("FAIL!");
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
                trigger(new DataContainer(new CData()), dp);
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
