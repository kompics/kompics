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

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.ConfigUpdate;
import se.sics.kompics.config.ValueMerger;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class ConfigUpdateTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigUpdateTest.class);

    private static final BlockingQueue<String> stringQ = new LinkedBlockingQueue<String>();
    private static long timeout = 5000;
    private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    @Test
    public void basicTest() {
        Config.Builder cb = Kompics.getConfig().modify(UUID.randomUUID());
        cb.setValue("testValue", "NOTEST");
        Config.Impl ci = (Config.Impl) Kompics.getConfig();
        ci.apply(cb.finalise(), ValueMerger.NONE);
        Kompics.setFaultHandler(new FaultHandler() {

            @Override
            public Fault.ResolveAction handle(Fault f) {
                f.getCause().printStackTrace(System.err);
                return Fault.ResolveAction.DESTROY;
            }
        });
        Kompics.createAndStart(Parent.class);
        waitFor("TEST");
        waitFor("TEST");
        waitFor("TEST");
        Kompics.shutdown();
        Kompics.resetConfig();
        Kompics.resetFaultHandler();
    }

    public static class Parent extends ComponentDefinition {

        Component updater = null;
        {
            updater = create(Updater.class, Init.NONE);
        }

        @Override
        public UpdateAction handleUpdate(ConfigUpdate update) {
            LOG.info("Parent received update.");
            assertEquals("NOTEST", config().getValueOrDefault("testValue", "DEFAULT"));
            return UpdateAction.DEFAULT;
        }

        @Override
        public void postUpdate() {
            stringQ.offer(config().getValueOrDefault("testValue", "DEFAULT"));
        }
    }

    public static class Updater extends ComponentDefinition {
        Component child = null;
        {
            child = create(Child.class, Init.NONE);

            Handler<Start> startHandler = new Handler<Start>() {

                @Override
                public void handle(Start event) {
                    Config.Builder builder = config().modify(id());
                    builder.setValue("testValue", "TEST");
                    updateConfig(builder.finalise());
                }
            };
            subscribe(startHandler, control);
        }

        @Override
        public void postUpdate() {
            stringQ.offer(config().getValueOrDefault("testValue", "DEFAULT"));
        }
    }

    public static class Child extends ComponentDefinition {

        @Override
        public UpdateAction handleUpdate(ConfigUpdate update) {
            LOG.info("Child received update.");
            assertEquals("NOTEST", config().getValueOrDefault("testValue", "DEFAULT"));
            return UpdateAction.DEFAULT;
        }

        @Override
        public void postUpdate() {
            stringQ.offer(config().getValueOrDefault("testValue", "DEFAULT"));
        }
    }

    private static void waitFor(String s) {
        try {
            String qString = stringQ.poll(timeout, timeUnit);
            if (qString == null) {
                fail("Timeout on waiting for \'" + s + "\'");
            }
            assertEquals(s, qString);
        } catch (InterruptedException ex) {
            LOG.debug("Failed waiting for String: " + s, ex);
        }
    }
}
