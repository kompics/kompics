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

// TODO: Auto-generated Javadoc
import java.util.function.Consumer;

/**
 * The <code>ComponentDefinition</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public abstract class ComponentDefinition {

    /**
     * Negative.
     *
     * @param portType the port type
     *
     * @return the negative < p>
     */
    protected final <P extends PortType> Negative<P> negative(Class<P> portType) {
        return core.createNegativePort(portType);
    }

    protected final <P extends PortType> Negative<P> provides(Class<P> portType) {
        return core.createNegativePort(portType);
    }

    /**
     * Positive.
     *
     * @param portType the port type
     *
     * @return the positive < p>
     */
    protected final <P extends PortType> Positive<P> positive(Class<P> portType) {
        return core.createPositivePort(portType);
    }

    /**
     * specifies that this component requires a port of type
     * <code>portType</code>.
     *
     * @param <P>
     * @param portType
     * @return
     */
    protected final <P extends PortType> Positive<P> requires(Class<P> portType) {
        return core.createPositivePort(portType);
    }

    /**
     * Trigger.
     *
     * @param event the event
     * @param port the port
     */
    protected final <P extends PortType> void trigger(KompicsEvent event, Port<P> port) {
        if (event instanceof Direct.Request) {
            Direct.Request r = (Direct.Request) event;
            r.setOrigin(port.getPair());
            Kompics.logger.info("Set port on request {} to {}", r, r.getOrigin());
        }
//		System.out.println(this.getClass()+": "+event+" triggert on "+port);
        port.doTrigger(event, core.wid, core);
    }

    protected final <P extends PortType> void answer(Direct.Request event) {
        if (!event.hasResponse()) {
            Kompics.logger.warn("Can't trigger a response for {} since none was given!", event);
            return;
        }
        event.getOrigin().doTrigger(event.getResponse(), core.wid, core);
    }

    protected final <P extends PortType> void answer(Direct.Request req, Direct.Response resp) {
        req.getOrigin().doTrigger(resp, core.wid, core);
    }

    /**
     * Expect.
     *
     * @param filter the filter
     */
    protected final void expect(Filter<?>... filter) {
        // TODO
    }

    protected final <E extends KompicsEvent, P extends PortType> Handler<E> handle(Port<P> port, Class<E> type, Consumer<E> fun) {
        Handler<E> handler = new FunctionHandler<>(type, fun);
        subscribe(handler, port);
        return handler;
    }

    protected final Handler<Start> onStart(Consumer<Start> fun) {
        return handle(control, Start.class, fun);
    }
    
    protected final Handler<Stop> onStop(Consumer<Stop> fun) {
        return handle(control, Stop.class, fun);
    }
    
    /**
     * Subscribe.
     *
     * @param handler the handler
     * @param port the port
     * @throws ConfigurationException
     */
    protected final <E extends KompicsEvent, P extends PortType> void subscribe(
            Handler<E> handler, Port<P> port) throws ConfigurationException {
        if (port instanceof JavaPort) {
            JavaPort<P> p = (JavaPort<P>) port;
            p.doSubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler subscription only works in Java");
        }
    }

    /**
     * Unsubscribe.
     *
     * @param handler the handler
     * @param port the port
     * @throws ConfigurationException
     */
    protected final <E extends KompicsEvent, P extends PortType> void unsubscribe(
            Handler<E> handler, Port<P> port) throws ConfigurationException {
        if (port instanceof JavaPort) {
            JavaPort<P> p = (JavaPort<P>) port;
            p.doUnsubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler (un)subscription only works in Java");
        }
    }

    /**
     * Creates the.
     *
     * @param definition the definition
     * @param initEvent init event to be passed to constructor
     *
     * @return the component
     */
    protected final <T extends ComponentDefinition> Component create(
            Class<T> definition, Init<T> initEvent) {
        return core.doCreate(definition, initEvent);
    }

    /**
     * Creates the.
     *
     * @param definition the definition
     * @param initEvent none
     *
     * @return the component
     */
    protected final <T extends ComponentDefinition> Component create(
            Class<T> definition, Init.None initEvent) {
        return core.doCreate(definition, null);
    }

    protected final void destroy(Component component) {
        core.doDestroy(component);
    }

    /**
     * Connect.
     *
     * @param positive the positive
     * @param negative the negative
     *
     * @return the channel < p>
     */
    protected final <P extends PortType> Channel<P> connect(
            Positive<P> positive, Negative<P> negative) {
        return core.doConnect(positive, negative);
    }

    /**
     * Connect.
     *
     * @param negative the negative
     * @param positive the positive
     *
     * @return the channel < p>
     */
    protected final <P extends PortType> Channel<P> connect(
            Negative<P> negative, Positive<P> positive) {
        return core.doConnect(positive, negative);
    }

    protected <P extends PortType> Channel<P> connect(Positive<P> positive,
            Negative<P> negative, ChannelFilter<?, ?> filter) {
        return core.doConnect(positive, negative, filter);
    }

    protected <P extends PortType> Channel<P> connect(Negative<P> negative,
            Positive<P> positive, ChannelFilter<?, ?> filter) {
        return core.doConnect(positive, negative, filter);
    }

    protected final <P extends PortType> void disconnect(Negative<P> negative,
            Positive<P> positive) {
        core.doDisconnect(positive, negative);
    }

    protected final <P extends PortType> void disconnect(Positive<P> positive,
            Negative<P> negative) {
        core.doDisconnect(positive, negative);
    }
    protected Negative<ControlPort> control;

    public Negative<ControlPort> getControlPort() {
        return control;
    }

    public ComponentCore getComponentCore() {
        return core;
    }

    /**
     * Use for custom cleanup. Will be called after all child components have
     * stopped, but before sending a Stopped message to the parent.
     *
     */
    public void tearDown() {
        // Do nothing normally
    }

    /* === PRIVATE === */
    private ComponentCore core;

    /**
     * Instantiates a new component definition.
     */
    protected ComponentDefinition() {
        core = new JavaComponent(this);
        control = core.createControlPort();
    }

    protected ComponentDefinition(Class<? extends ComponentCore> coreClass) {
        try {
            core = coreClass.newInstance();
        } catch (Exception e) {
            //e.printStackTrace();
            //System.out.println(e + ": " + e.getMessage());
            throw new ConfigurationException(e.getMessage());
        }
        control = core.createControlPort();
    }
}
