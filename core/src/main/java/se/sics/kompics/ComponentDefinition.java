/*
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

import java.util.Optional;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import se.sics.kompics.Fault.ResolveAction;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.ConfigUpdate;

/**
 * The <code>ComponentDefinition</code> class.
 *
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @version $Id$
 */
public abstract class ComponentDefinition {

    /**
     * Create a negative (provided) port instance and return it.
     *
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the class instance of the port type
     *
     * @return the new instance
     */
    protected final <P extends PortType> Negative<P> negative(Class<P> portType) {
        return core.createNegativePort(portType);
    }

    /**
     * Create a provided (negative) port instance and return it.
     * 
     * Same as {@link #negative(Class)}.
     *
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the class instance of the port type
     *
     * @return the new instance
     */
    protected final <P extends PortType> Negative<P> provides(Class<P> portType) {
        return core.createNegativePort(portType);
    }

    /**
     * Create a positive (required) port instance and return it.
     *
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the class instance of the port type
     *
     * @return the new instance
     */
    protected final <P extends PortType> Positive<P> positive(Class<P> portType) {
        return core.createPositivePort(portType);
    }

    /**
     * Create a required (positive) port instance and return it.
     *
     * @param <P>
     *            the type of the port type
     * @param portType
     *            the class instance of the port type
     *
     * @return the new instance
     */
    protected final <P extends PortType> Positive<P> requires(Class<P> portType) {
        return core.createPositivePort(portType);
    }

    /**
     * Trigger an event on a port instance.
     *
     * @param <P>
     *            type of the port type
     * @param event
     *            the event to be triggered
     * @param port
     *            the port where the event is triggered on
     */
    protected final <P extends PortType> void trigger(KompicsEvent event, Port<P> port) {
        if (event instanceof Direct.Request) {
            Direct.Request<?> r = (Direct.Request<?>) event;
            r.setOrigin(port.getPair());
            logger.trace("Set port on request {} to {}", r, r.getOrigin());
        } else if (event instanceof Direct.Response) {
            throw new KompicsException(
                    "Direct.Response can not be \"trigger\"ed. It has to \"answer\" a Direct.Request!");
        }
        // System.out.println(this.getClass()+": "+event+" triggert on "+port);
        port.doTrigger(event, core.wid, core);
    }

    /**
     * Reply to a request directly, using the stored origin and the preset response.
     * 
     * The passed requests must have a preset response!
     * 
     * @param event
     *            the request event to reply to
     */
    protected final void answer(Direct.Request<?> event) {
        if (!event.hasResponse()) {
            logger.warn("Can't trigger a response for {} since none was given!", event);
            return;
        }
        event.getOrigin().doTrigger(event.getResponse(), core.wid, core);
    }

    /**
     * Reply to a request directly, using the stored origin and the given response.
     * 
     * The passed requests must have a preset response!
     * 
     * @param req
     *            the request event to reply to
     * @param resp
     *            the response to send
     */
    protected final void answer(Direct.Request<?> req, Direct.Response resp) {
        req.getOrigin().doTrigger(resp, core.wid, core);
    }

    /**
     * Create a functionl handler and automatically subscribe it to a port.
     * 
     * @param <E>
     *            type of the handled event
     * @param <P>
     *            type of the port being subscribed to
     * @param port
     *            a port instance to subcribe the handler to
     * @param type
     *            a class instance of the handled event type
     * @param fun
     *            the handler function
     * @return the newly created handler
     */
    protected final <E extends KompicsEvent, P extends PortType> Handler<E> handle(Port<P> port, Class<E> type,
            Consumer<E> fun) {
        Handler<E> handler = new FunctionHandler<>(type, fun);
        subscribe(handler, port);
        return handler;
    }

    /**
     * Subscribe a handler to the {@link Start} event.
     * 
     * @param fun
     *            the handler function
     * @return the newly created handler
     */
    protected final Handler<Start> onStart(Consumer<Start> fun) {
        return handle(control, Start.class, fun);
    }

    /**
     * Subscribe a handler to the {@link Stop} event.
     * 
     * @param fun
     *            the handler function
     * @return the newly created handler
     */
    protected final Handler<Stop> onStop(Consumer<Stop> fun) {
        return handle(control, Stop.class, fun);
    }

    /**
     * Subscribe a handler to the {@link Kill} event.
     * 
     * @param fun
     *            the handler function
     * @return the newly created handler
     */
    protected final Handler<Kill> onKill(Consumer<Kill> fun) {
        return handle(control, Kill.class, fun);
    }

    /**
     * Subscribe a handler to a port.
     *
     * @param <E>
     *            the type of event to subscribe to
     * @param <P>
     *            the type of port to subscribe to
     * @param handler
     *            the handler to subcribe
     * @param port
     *            the port to subcribe to
     * @throws ConfigurationException
     *             when trying to subscribe on instances that aren't of type {@link JavaPort}
     */
    protected final <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) {
        if (port instanceof JavaPort) {
            JavaPort<P> p = (JavaPort<P>) port;
            p.doSubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler subscription only works in Java");
        }
    }

    /**
     * Subscribe a matched handler to a port.
     *
     * @param handler
     *            the handler to subcribe
     * @param port
     *            the port to subcribe to
     * @throws ConfigurationException
     *             when trying to subscribe on foreign ports or instances that aren't of type {@link JavaPort}
     */
    protected final void subscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
        if (port instanceof JavaPort) {
            JavaPort<?> p = (JavaPort<?>) port;
            if (p.owner.equals(this.core)) {
                p.doSubscribe(handler);
            } else {
                throw new ConfigurationException("Cannot subscribe Handlers to other component's ports, "
                        + "since the behaviour of this is unspecifed. "
                        + "(The handler might be executed on the wrong thread)");
            }

        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler subscription only works with matching ports and components");
        }
    }

    /**
     * Unsubscribe a matched handler from a port
     *
     * @param handler
     *            the handler to unsubcribe
     * @param port
     *            the port to unsubcribe from
     * @throws ConfigurationException
     *             when trying to subscribe on instances that aren't of type {@link JavaPort}
     */
    protected final void unsubscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
        if (port instanceof JavaPort) {
            JavaPort<?> p = (JavaPort<?>) port;
            p.doUnsubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler unsubscription only works with matching ports and components");
        }
    }

    /**
     * Unsubscribe a handler from a port
     *
     * @param <E>
     *            the event type of the handle to subscribe
     * @param <P>
     *            the port type to subscribe to
     * @param handler
     *            the handler to unsubcribe
     * @param port
     *            the port to unsubcribe from
     * @throws ConfigurationException
     *             when trying to subscribe on instances that aren't of type {@link JavaPort}
     */
    protected final <E extends KompicsEvent, P extends PortType> void unsubscribe(Handler<E> handler, Port<P> port)
            throws ConfigurationException {
        if (port instanceof JavaPort) {
            JavaPort<P> p = (JavaPort<P>) port;
            p.doUnsubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler (un)subscription only works in Java");
        }
    }

    /**
     * Create a child component with a given init event.
     *
     * @param <T>
     *            the type of the child component
     * @param definition
     *            the component definition of the child component
     * @param initEvent
     *            init event to be passed to constructor
     *
     * @return the newly created component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
        return core.doCreate(definition, Optional.of(initEvent));
    }

    /**
     * Create a child component with no init event.
     * 
     * Use `Init.NONE` for the instance.
     *
     * @param <T>
     *            the type of the child component
     * @param definition
     *            the component definition of the child component
     * @param initEvent
     *            special {@link Init.None} event to be passed to constructor
     *
     * @return the newly created component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
        Optional<Init<T>> init = Optional.empty();
        return core.doCreate(definition, init);
    }

    /**
     * Create a child component with a given init event and a configuration update.
     *
     * @param <T>
     *            the type of the child component
     * @param definition
     *            the component definition of the child component
     * @param initEvent
     *            init event to be passed to constructor
     * @param update
     *            a configuration update to pass to the child
     *
     * @return the newly created component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent,
            ConfigUpdate update) {
        return core.doCreate(definition, Optional.of(initEvent), Optional.of(update));
    }

    /**
     * Create a child component with no init event and a configuration update.
     * 
     * Use `Init.NONE` for the instance.
     *
     * @param <T>
     *            the type of the child component
     * @param definition
     *            the component definition of the child component
     * @param initEvent
     *            special {@link Init.None} event to be passed to constructor
     * @param update
     *            a configuration update to pass to the child
     *
     * @return the newly created component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent,
            ConfigUpdate update) {
        Optional<Init<T>> init = Optional.empty();
        return core.doCreate(definition, init, Optional.of(update));
    }

    /**
     * Force destruction of a child component.
     * 
     * <b>Only</b> ever call this on a component in the PASSIVE state!
     * 
     * @param component
     *            the child component to destroy
     */
    protected final void destroy(Component component) {
        core.doDestroy(component);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @deprecated Use {@link #connect(Positive, Negative, ChannelFactory) } with `Channel.TWO_WAY` instead
     * 
     * @return the newly created channel intance
     */
    @Deprecated
    protected final <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @deprecated Use {@link #connect(Positive, Negative, ChannelFactory) } with `Channel.TWO_WAY` instead
     * 
     * @return the newly created channel intance
     */
    @Deprecated
    protected final <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @param selector
     *            the channel selector to use
     * @deprecated Use {@link #connect(Positive, Negative, ChannelSelector, ChannelFactory) } with `Channel.TWO_WAY`
     *             instead
     * 
     * @return the newly created channel intance
     */
    @Deprecated
    protected <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
            ChannelSelector<?, ?> selector) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @param selector
     *            the channel selector to use
     * @deprecated Use {@link #connect(Positive, Negative, ChannelSelector, ChannelFactory) } with `Channel.TWO_WAY`
     *             instead
     * 
     * @return the newly created channel intance
     */
    @Deprecated
    protected <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
            ChannelSelector<?, ?> selector) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @param selector
     *            the channel selector to use
     * @param factory
     *            a one-way or two-way factory (see {@link Channel})
     * 
     * @return the newly created channel intance
     */
    protected <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
            ChannelSelector<?, ?> selector, ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @param selector
     *            the channel selector to use
     * @param factory
     *            a one-way or two-way factory (see {@link Channel})
     * 
     * @return the newly created channel intance
     */
    protected <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
            ChannelSelector<?, ?> selector, ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @param factory
     *            a one-way or two-way factory (see {@link Channel})
     * 
     * @return the newly created channel intance
     */
    protected <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
            ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    /**
     * Connect two ports via a channel.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @param factory
     *            a one-way or two-way factory (see {@link Channel})
     * 
     * @return the newly created channel intance
     */
    protected <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
            ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    /**
     * Disconnect two ports that are connected via a channel.
     * 
     * This is very inefficient, as it needs to find a matching channel first. Use {@link #disconnect(Channel)} or
     * {@link Channel#disconnect()} instead.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @deprecated Use {@link #disconnect(Channel)} or {@link Channel#disconnect()} instead
     */
    @Deprecated
    protected final <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
        PortCore<P> pos = (PortCore<P>) positive;
        PortCore<P> neg = (PortCore<P>) negative;
        List<Channel<P>> channels = pos.findChannelsTo(neg);
        for (Channel<P> c : channels) {
            c.disconnect();
        }
    }

    /**
     * Disconnect two ports that are connected via a channel.
     * 
     * This is very inefficient, as it needs to find a matching channel first. Use {@link #disconnect(Channel)} or
     * {@link Channel#disconnect()} instead.
     * 
     * @param <P>
     *            the shared port type
     * @param negative
     *            the negative port instance
     * @param positive
     *            the positive port instance
     * @deprecated Use {@link #disconnect(Channel)} or {@link Channel#disconnect()} instead
     */
    @Deprecated
    protected final <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
        PortCore<P> pos = (PortCore<P>) positive;
        PortCore<P> neg = (PortCore<P>) negative;
        List<Channel<P>> channels = neg.findChannelsTo(pos);
        for (Channel<P> c : channels) {
            c.disconnect();
        }
    }

    /**
     * Disconnect a channel connecting two ports.
     * 
     * 
     * @param <P>
     *            the shared port type
     * @param channel
     *            the channel to drop
     */
    protected final <P extends PortType> void disconnect(Channel<P> channel) {
        channel.disconnect();
    }

    /**
     * The components own control port.
     */
    protected final Negative<ControlPort> control;
    // different sides of the same port...naming is for readability in usage
    /**
     * The receiving side of the loopback port.
     * 
     * The sending side is at {@link #onSelf}.
     */
    protected final Negative<LoopbackPort> loopback;

    /**
     * The sending side of the loopback port.
     * 
     * The receiving side is at {@link #loopback}.
     */
    protected final Positive<LoopbackPort> onSelf;

    /**
     * Get the component's control port.
     * 
     * @return the control port
     */
    public final Negative<ControlPort> getControlPort() {
        return control;
    }

    /**
     * Get the component's core instance.
     * 
     * @return the component core
     */
    public final ComponentCore getComponentCore() {
        return core;
    }

    /**
     * Get the component's config instance.
     * 
     * @return the config instance
     */
    public final Config config() {
        return core.config();
    }

    /**
     * Get the component's unique id.
     * 
     * @return the component id
     */
    public final UUID id() {
        return core.id();
    }

    /**
     * Send a kill event to the component itself.
     * 
     * Only valid if the component's state is active!
     */
    public final void suicide() {
        if (core.state == Component.State.ACTIVE || core.state == Component.State.STARTING) {
            trigger(Kill.event, control.getPair());
        } else {
            logger.warn("Could not commit suicide as state is non-active");
        }
    }

    /**
     * Cleanup the component before shutdown.
     * 
     * Use for custom cleanup. Will be called after all child components have stopped, but before sending a Stopped
     * message to the parent.
     * 
     * Does nothings by default.
     *
     */
    public void tearDown() {
        // Do nothing normally
    }

    /**
     * Define how to handle a fault.
     * <p>
     * Override this method for custom error handling.
     * <p>
     * Default action is ESCALATE.
     * <p>
     * Possible actions are:
     * <ul>
     * <li>ESCALATE: Forward fault to parent.
     * <li>IGNORE: Drop fault. Resume component as if nothing happened.
     * <li>RESOLVED: Fault has been handled by user. Don't do anything else.
     * </ul>
     * 
     * @param fault
     *            the fault that must be handled
     * @return the action to take
     */
    public ResolveAction handleFault(Fault fault) {
        return ResolveAction.ESCALATE;
    }

    /**
     * Deal with a configuration update.
     * <p>
     * Override for custom update handling.
     * <p>
     * Default action is to propagate the original everywhere and apply to self
     * 
     * @param update
     *            the update to be handled
     * @return the action to be taken
     */
    public UpdateAction handleUpdate(ConfigUpdate update) {
        return UpdateAction.DEFAULT;
    }

    /**
     * Override to perform actions after a ConfigUpdate was applied and forwarded.
     */
    public void postUpdate() {
    }

    /**
     * Perform a configuration update.
     * 
     * @param update
     *            the update to perform
     */
    public final void updateConfig(ConfigUpdate update) {
        core.doConfigUpdate(update);
    }

    /**
     * Override to allow components of this type to start their own independent {@link se.sics.kompics.config.Config} id
     * lines.
     * <p>
     * This is helpful in simulation, when simulating multiple independent nodes. Make sure that no
     * {@code ConfigUpdate}s are passed to siblings or parents of such nodes! (Override
     * {@link #handleUpdate(se.sics.kompics.config.ConfigUpdate)})
     * <p>
     * 
     * @return Whether to create a new config id line for this component (default: {@code true})
     */
    public boolean separateConfigId() {
        return false;
    }

    /**
     * The component's proxy instance for reflective configuration.
     */
    public final ComponentProxy proxy = new ComponentProxy() {

        @Override
        public <P extends PortType> void trigger(KompicsEvent e, Port<P> p) {
            ComponentDefinition.this.trigger(e, p);
        }

        @Override
        public <P extends PortType> void answer(Direct.Request<?> event) {
            ComponentDefinition.this.answer(event);
        }

        @Override
        public <P extends PortType> void answer(Direct.Request<?> req, Direct.Response resp) {
            ComponentDefinition.this.answer(req, resp);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
            return ComponentDefinition.this.create(definition, initEvent);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
            return ComponentDefinition.this.create(definition, initEvent);
        }

        @Override
        public void destroy(Component component) {
            ComponentDefinition.this.destroy(component);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
            return ComponentDefinition.this.connect(positive, negative);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
            return ComponentDefinition.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
            ComponentDefinition.this.disconnect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
            ComponentDefinition.this.disconnect(positive, negative);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
                ChannelSelector<?, ?> filter) {
            return ComponentDefinition.this.connect(positive, negative, filter);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
                ChannelSelector<?, ?> filter) {
            return ComponentDefinition.this.connect(negative, positive, filter);
        }

        @Override
        public Negative<ControlPort> getControlPort() {
            return ComponentDefinition.this.getControlPort();
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
                ChannelFactory factory) {
            return ComponentDefinition.this.connect(positive, negative, factory);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
                ChannelSelector<?, ?> selector, ChannelFactory factory) {
            return ComponentDefinition.this.connect(positive, negative, selector, factory);
        }

        @Override
        public <P extends PortType> void disconnect(Channel<P> c) {
            ComponentDefinition.this.disconnect(c);
        }

        @Override
        public <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) {
            ComponentDefinition.this.subscribe(handler, port);
        }

        @Override
        public void subscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
            ComponentDefinition.this.subscribe(handler, port);
        }

        @Override
        public void unsubscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
            ComponentDefinition.this.unsubscribe(handler, port);
        }

        @Override
        public <E extends KompicsEvent, P extends PortType> void unsubscribe(Handler<E> handler, Port<P> port) {
            ComponentDefinition.this.unsubscribe(handler, port);
        }

        @Override
        public UUID id() {
            return ComponentDefinition.this.id();
        }

        @Override
        public <P extends PortType> Positive<P> getPositive(Class<P> portType) {
            return ComponentDefinition.this.getComponentCore().getPositive(portType);
        }

        @Override
        public <P extends PortType> Negative<P> getNegative(Class<P> portType) {
            return ComponentDefinition.this.getComponentCore().getNegative(portType);
        }

        @Override
        public <P extends PortType> Positive<P> requires(Class<P> portType) {
            return ComponentDefinition.this.requires(portType);
        }

        @Override
        public <P extends PortType> Negative<P> provides(Class<P> portType) {
            return ComponentDefinition.this.provides(portType);
        }
    };

    /* ********** Logging Related *********** */
    /**
     * Pre-configured MDC key for the unique component id.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     */
    public static final String MDC_KEY_CID = "kcomponent-id";
    /**
     * Pre-configured MDC key for the current component lifecycle state.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     */
    public static final String MDC_KEY_CSTATE = "kcomponent-state";

    /**
     * Kompics provided slf4j logger with managed diagnostic context.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, String> mdcState = new HashMap<>();
    private final Map<String, String> mdcReset = new HashMap<>();

    /**
     * Should not be necessary to call usually, as ComponentCore will do it.
     * <p>
     * Protected mainly for use by Kompics Scala.
     * <p>
     * Can also be used to set component MDC when executing related off-kompics work (check for concurrency issues,
     * though!).
     */
    protected void setMDC() {
        MDC.setContextMap(mdcState);
    }

    /**
     * Associate key with value in the logging diagnostic context.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to associate with the key
     * @deprecated Since 1.2.0, use {@link #loggingContextPut(String, String)} instead.
     */
    @Deprecated
    protected void loggingCtxPut(String key, String value) {
        this.loggingContextPut(key, value);
    }

    /**
     * Associate key with value in the logging diagnostic context.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to associate with the key
     */
    protected void loggingContextPut(String key, String value) {
        mdcState.put(key, value);
        MDC.put(key, value);
    }

    /**
     * Associate key permanently with value in the logging diagnostic context.
     * <p>
     * Keys set in this way are not removed by {@link #loggingContextReset()} or {@link #loggingContextRemove(String)}.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to associate with the key
     * @deprecated Since 1.2.0, use {@link #loggingContextPutAlways(String, String)} instead.
     */
    protected void loggingCtxPutAlways(String key, String value) {
        this.loggingContextPutAlways(key, value);
    }

    /**
     * Associate key permanently with value in the logging diagnostic context.
     * <p>
     * Keys set in this way are not removed by {@link #loggingContextReset()} or {@link #loggingContextRemove(String)}.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     *
     * @param key
     *            the key to use
     * @param value
     *            the value to associate with the key
     */
    protected void loggingContextPutAlways(String key, String value) {
        mdcReset.put(key, value);
        mdcState.put(key, value);
        MDC.put(key, value);
    }

    /**
     * Disassociate any value with the key in the logging diagnostic context.
     * <p>
     * 
     * @param key
     *            the key to remove
     * @deprecated Since 1.2.0, use {@link #loggingContextRemove(String)} instead.
     */
    @Deprecated
    protected void loggingCtxRemove(String key) {
        this.loggingContextRemove(key);
    }

    /**
     * Disassociate any value with the key in the logging diagnostic context.
     * <p>
     * 
     * @param key
     *            the key to remove
     */
    protected void loggingContextRemove(String key) {
        mdcState.remove(key);
        MDC.remove(key);
    }

    /**
     * Get the value associated with key in the current logging diagnostic context.
     * <p>
     * 
     * @param key
     *            the key to fetch the value for
     * 
     * @deprecated Since 1.2.0, use {@link #loggingContextGet(String)} instead.
     * 
     * @return the value associated with key
     */
    @Deprecated
    protected String loggingCtxGet(String key) {
        return this.loggingContextGet(key);
    }

    /**
     * Get the value associated with key in the current logging diagnostic context.
     * <p>
     * 
     * @param key
     *            the key to fetch the value for
     * @return the value associated with key
     */
    protected String loggingContextGet(String key) {
        return mdcState.get(key);
    }

    /**
     * Reset the current logging diagnostic context.
     * <p>
     * Removes all items added to context by the user that weren't set with
     * {@link #loggingContextPutAlways(String, String)}
     * 
     * @deprecated Since 1.2.0, use {@link #loggingContextReset()} instead.
     */
    @Deprecated
    protected void loggingCtxReset() {
        this.loggingContextReset();
    }

    /**
     * Reset the current logging diagnostic context.
     * <p>
     * Removes all items added to context by the user that weren't set with
     * {@link #loggingContextPutAlways(String, String)}
     */
    protected void loggingContextReset() {
        String state = MDC.get(MDC_KEY_CSTATE);
        mdcState.clear();
        mdcState.putAll(mdcReset);
        MDC.setContextMap(mdcState);
        if (state != null) {
            MDC.put(MDC_KEY_CSTATE, state);
        }
    }

    private void loggingContextInit() {
        // if (!mdcState.isEmpty()) {
        // mdcReset.putAll(mdcState);
        // }
        mdcReset.put(MDC_KEY_CID, this.id().toString());
        loggingCtxReset();
    }

    /* === PRIVATE === */
    private ComponentCore core;

    /**
     * Instantiate a new component definition.
     */
    protected ComponentDefinition() {
        core = new JavaComponent(this);
        control = core.createControlPort();
        loopback = core.createNegativePort(LoopbackPort.class);
        onSelf = loopback.getPair();
        loggingContextInit();
    }

    /**
     * Instantiate a new component definition.
     * 
     * @param coreClass
     *            to use as the runtime core for the definition
     */
    @SuppressWarnings("rawtypes")
    protected ComponentDefinition(Class<? extends ComponentCore> coreClass) {
        try {
            Constructor[] constrs = coreClass.getConstructors();
            for (Constructor constr : constrs) {
                Class[] paramTypes = constr.getParameterTypes();
                if ((paramTypes.length) == 1 && paramTypes[0].isInstance(this)) {
                    core = (ComponentCore) constr.newInstance(this);
                }
            }
            if (core == null) {
                core = coreClass.newInstance();
            }
        } catch (Exception e) {
            // e.printStackTrace();
            // System.out.println(e + ": " + e.getMessage());
            throw new ConfigurationException(e);
        }
        control = core.createControlPort();
        loopback = core.createNegativePort(LoopbackPort.class);
        onSelf = loopback.getPair();
        loggingContextInit();
    }
}
