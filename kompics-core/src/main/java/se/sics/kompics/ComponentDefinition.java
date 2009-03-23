/**
 * This file is part of the Kompics component model runtime.
 * 
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics;

// TODO: Auto-generated Javadoc
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
	 * @param portType
	 *            the port type
	 * 
	 * @return the negative
	 *         < p>
	 */
	protected final <P extends PortType> Negative<P> negative(Class<P> portType) {
		return core.createNegativePort(portType);
	}

	/**
	 * Positive.
	 * 
	 * @param portType
	 *            the port type
	 * 
	 * @return the positive
	 *         < p>
	 */
	protected final <P extends PortType> Positive<P> positive(Class<P> portType) {
		return core.createPositivePort(portType);
	}

	/**
	 * Trigger.
	 * 
	 * @param event
	 *            the event
	 * @param port
	 *            the port
	 */
	protected final <P extends PortType> void trigger(Event event, Port<P> port) {
		((PortCore<P>) port).doTrigger(event, core.wid, core);
	}

	/**
	 * Expect.
	 * 
	 * @param filter
	 *            the filter
	 */
	protected final void expect(Filter<?>... filter) {
		// TODO
	}

	/**
	 * Subscribe.
	 * 
	 * @param handler
	 *            the handler
	 * @param port
	 *            the port
	 */
	protected final <E extends Event, P extends PortType> void subscribe(
			Handler<E> handler, Port<P> port) {
		((PortCore<P>) port).doSubscribe(handler);
	}

	/**
	 * Unsubscribe.
	 * 
	 * @param handler
	 *            the handler
	 * @param port
	 *            the port
	 */
	protected final <E extends Event, P extends PortType> void unsubscribe(
			Handler<E> handler, Port<P> port) {
		((PortCore<P>) port).doUnsubscribe(handler);
	}

	/**
	 * Creates the.
	 * 
	 * @param definition
	 *            the definition
	 * 
	 * @return the component
	 */
	protected final Component create(Class<? extends ComponentDefinition> definition) {
		return core.doCreate(definition);
	}

	/**
	 * Connect.
	 * 
	 * @param positive
	 *            the positive
	 * @param negative
	 *            the negative
	 * 
	 * @return the channel
	 *         < p>
	 */
	protected final <P extends PortType> Channel<P> connect(Positive<P> positive,
			Negative<P> negative) {
		return core.doConnect(positive, negative);
	}

	/**
	 * Connect.
	 * 
	 * @param negative
	 *            the negative
	 * @param positive
	 *            the positive
	 * 
	 * @return the channel
	 *         < p>
	 */
	protected final <P extends PortType> Channel<P> connect(Negative<P> negative,
			Positive<P> positive) {
		return core.doConnect(positive, negative);
	}

	// protected <E extends PortType> Channel<E> connect(Positive<E> p,
	// Negative<E> q, ChannelFilter filter) {
	// return null;
	// } TODO
	//
	// protected <E extends PortType> Channel<E> connect(Negative<E> p,
	// Positive<E> q, ChannelFilter filter) {
	// return null;
	// }

	protected final Negative<ControlPort> control;

	/* === PRIVATE === */

	private final ComponentCore core;

	/**
	 * Instantiates a new component definition.
	 */
	protected ComponentDefinition() {
		core = new ComponentCore(this);
		control = core.createControlPort();
	}

	final ComponentCore getComponentCore() {
		return core;
	}
}
