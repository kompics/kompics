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

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class TracerTest {

    public TracerTest() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    @Test
    public void simpleTest() {
        try {
            Kompics.createAndStart(ParentComponent.class);
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            //System.err.println(ex.getMessage());
            Kompics.shutdown();
            Assert.fail(ex.getMessage());
        }
    }

    static class TestTracer implements Tracer {

        @Override
        public boolean triggeredOutgoing(KompicsEvent event, PortCore<?> port) {
            System.out.println("Got outgoing event " + event + " on " + port.getPortType());
            return true;
        }

        @Override
        public boolean triggeredIncoming(KompicsEvent event, PortCore<?> port) {
            System.out.println("Got incoming event " + event + " on " + port.getPortType());
            return true;
        }
    }

    static class ParentComponent extends ComponentDefinition {

        final Component tc;

        public ParentComponent() {
            Tracer t = new TestTracer();
            ComponentCore.childTracer.set(t);
            tc = create(TracingComponent.class, Init.NONE);

            subscribe(startHandler, control);
        }

        protected final Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                logger.info("Started ParentComponent.");
            }

        };
    }

    static class TracingComponent extends ComponentDefinition {

        public TracingComponent() {
            subscribe(startHandler, control);
        }

        protected final Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                logger.info("Started TracingComponent.");
                Kompics.asyncShutdown();
            }

        };
    }
}
