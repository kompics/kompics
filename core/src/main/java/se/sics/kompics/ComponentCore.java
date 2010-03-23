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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The <code>ComponentCore</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class ComponentCore implements Component {

	/* outside ports */
	private HashMap<Class<? extends PortType>, PortCore<? extends PortType>> positivePorts;
	private HashMap<Class<? extends PortType>, PortCore<? extends PortType>> negativePorts;

	private PortCore<ControlPort> positiveControl, negativeControl;

	ComponentCore parent;

	private static ThreadLocal<ComponentCore> parentThreadLocal = new ThreadLocal<ComponentCore>();

	private List<ComponentCore> children;

	ComponentDefinition component;

	private Scheduler scheduler;

	boolean initSubscriptionInConstructor;
	AtomicBoolean initDone, initReceived;
	private AtomicReference<Event> firstInitEvent;

	int wid;

	/**
	 * Instantiates a new component core.
	 * 
	 * @param componentDefinition
	 *            the component definition
	 */
	public ComponentCore(ComponentDefinition componentDefinition) {
		this.positivePorts = new HashMap<Class<? extends PortType>, PortCore<? extends PortType>>();
		this.negativePorts = new HashMap<Class<? extends PortType>, PortCore<? extends PortType>>();
		this.parent = parentThreadLocal.get();
		this.component = componentDefinition;
		this.initSubscriptionInConstructor = false;
		this.initDone = new AtomicBoolean(false);
		this.initReceived = new AtomicBoolean(false);
		this.firstInitEvent = new AtomicReference<Event>(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Component#getControl()
	 */
	public Positive<ControlPort> getControl() {
		return positiveControl;
	}
	
	public Positive<ControlPort> control() {
		return positiveControl;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Component#getNegative(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <P extends PortType> Negative<P> getNegative(Class<P> portType) {
		Negative<P> port = (Negative<P>) negativePorts.get(portType);
		if (port == null)
			throw new RuntimeException(component + " has no negative "
					+ portType.getCanonicalName());
		return port;
	}

	public <P extends PortType> Negative<P> required(Class<P> portType) {
		return getNegative(portType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Component#getPositive(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <P extends PortType> Positive<P> getPositive(Class<P> portType) {
		Positive<P> port = (Positive<P>) positivePorts.get(portType);
		if (port == null)
			throw new RuntimeException(component + " has no positive "
					+ portType.getCanonicalName());
		return port;
	}

	public <P extends PortType> Positive<P> provided(Class<P> portType) {
		return getPositive(portType);
	}

	<P extends PortType> Negative<P> createNegativePort(Class<P> portType) {
		PortCore<P> negativePort = new PortCore<P>(false, PortType
				.getPortType(portType), this);
		PortCore<P> positivePort = new PortCore<P>(true, PortType
				.getPortType(portType), parent);

		negativePort.setPair(positivePort);
		positivePort.setPair(negativePort);

		Positive<?> existing = positivePorts.put(portType, positivePort);
		if (existing != null)
			throw new RuntimeException("Cannot create multiple negative "
					+ portType.getCanonicalName());
		return negativePort;
	}

	<P extends PortType> Positive<P> createPositivePort(Class<P> portType) {
		PortCore<P> negativePort = new PortCore<P>(false, PortType
				.getPortType(portType), parent);
		PortCore<P> positivePort = new PortCore<P>(true, PortType
				.getPortType(portType), this);

		negativePort.setPair(positivePort);
		positivePort.setPair(negativePort);

		Negative<?> existing = negativePorts.put(portType, negativePort);
		if (existing != null)
			throw new RuntimeException("Cannot create multiple positive "
					+ portType.getCanonicalName());
		return positivePort;
	}

	Negative<ControlPort> createControlPort() {
		negativeControl = new PortCore<ControlPort>(false, PortType
				.getPortType(ControlPort.class), this);
		positiveControl = new PortCore<ControlPort>(true, PortType
				.getPortType(ControlPort.class), parent);

		positiveControl.setPair(negativeControl);
		negativeControl.setPair(positiveControl);

		negativeControl.doSubscribe(handleStart);
		negativeControl.doSubscribe(handleStop);

		return negativeControl;
	}

	Component doCreate(Class<? extends ComponentDefinition> definition) {
		// create an instance of the implementing component type
		ComponentDefinition component;
		try {
			parentThreadLocal.set(this);
			component = definition.newInstance();
			ComponentCore child = component.getComponentCore();

			if (!child.initSubscriptionInConstructor) {
				child.initDone.set(true);
			}

			child.setScheduler(scheduler);
			if (children == null) {
				children = new LinkedList<ComponentCore>();
			}
			children.add(child);

			return child;
		} catch (InstantiationException e) {
			throw new RuntimeException("Cannot create component "
					+ definition.getCanonicalName(), e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot create component "
					+ definition.getCanonicalName(), e);
		}
	}

	void doDestroy(Component component) {
		ComponentCore child = (ComponentCore) component;
		children.remove(child);
	}

	<P extends PortType> Channel<P> doConnect(Positive<P> positive,
			Negative<P> negative) {
		PortCore<P> positivePort = (PortCore<P>) positive;
		PortCore<P> negativePort = (PortCore<P>) negative;
		ChannelCore<P> channel = new ChannelCore<P>(positivePort, negativePort,
				negativePort.getPortType());

		positivePort.addChannel(channel);
		negativePort.addChannel(channel);

		return channel;
	}

	<P extends PortType> Channel<P> doConnect(Positive<P> positive,
			Negative<P> negative, ChannelFilter<?, ?> filter) {
		PortCore<P> positivePort = (PortCore<P>) positive;
		PortCore<P> negativePort = (PortCore<P>) negative;
		ChannelCore<P> channel = new ChannelCore<P>(positivePort, negativePort,
				negativePort.getPortType());

		Class<? extends Event> eventType = filter.getEventType();
		P portType = positivePort.getPortType();
		if (filter.isPositive()) {
			if (!portType.hasPositive(eventType)) {
				throw new RuntimeException("Port type " + portType
						+ " has no positive " + eventType);
			}
			positivePort.addChannel(channel, filter);
			negativePort.addChannel(channel);
		} else {
			if (!portType.hasNegative(eventType)) {
				throw new RuntimeException("Port type " + portType
						+ " has no negative " + eventType);
			}
			positivePort.addChannel(channel);
			negativePort.addChannel(channel, filter);
		}

		return channel;
	}

	<P extends PortType> void doDisconnect(Positive<P> positive,
			Negative<P> negative) {
		PortCore<P> positivePort = (PortCore<P>) positive;
		PortCore<P> negativePort = (PortCore<P>) negative;

		positivePort.removeChannelTo(negativePort);
		negativePort.removeChannelTo(positivePort);
	}

	/* === SCHEDULING === */

	private AtomicInteger workCount = new AtomicInteger(0);

	private SpinlockQueue<PortCore<?>> readyPorts = new SpinlockQueue<PortCore<?>>();

	/**
	 * Sets the scheduler.
	 * 
	 * @param scheduler
	 *            the new scheduler
	 */
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	void eventReceived(PortCore<?> port, int wid, boolean isInitEvent) {
		// upon the first event received, we schedule the component. However, if
		// the component needs to execute an Init event first, we don't schedule
		// the component until it receives and handles an Init event.
		if (initDone.get()) {
			// default case
			readyPorts.offer(port);
			int wc = workCount.getAndIncrement();
			if (wc == 0) {
				if (scheduler == null)
					scheduler = Kompics.getScheduler();
				scheduler.schedule(this, wid);
			}
		} else {
			// init is not yet done. we only schedule this component for
			// execution if the received event is an Init event
			if (isInitEvent) {
				if (!initReceived.get()) {
					initReceived.set(true);
					// for the first Init received
					// we schedule the component directly
					if (scheduler == null)
						scheduler = Kompics.getScheduler();

					firstInitEvent.set(port.pickFirstEvent());
					scheduler.schedule(this, wid);
				} else {
					// next Init event we schedule in the regular way
					readyPorts.offer(port);
					workCount.incrementAndGet();
				}
			} else {
				// we received some other event before Init
				readyPorts.offer(port);
				workCount.incrementAndGet();
			}
		}
	}

	void execute(int wid) {
		this.wid = wid;

		// if init is not yet done it means we were scheduled to run the first
		// Init event. We do not touch readyPorts and workCount.
		if (!initDone.get()) {
			ArrayList<Handler<?>> handlers = negativeControl
					.getSubscribedHandlers(firstInitEvent.get());
			if (handlers != null) {
				for (int i = 0; i < handlers.size(); i++) {
					executeEvent(firstInitEvent.get(), handlers.get(i));
				}
			}
			initDone.set(true);

			// if other events arrived before the Init we schedule the component
			// to execute them
			if (workCount.get() > 0) {
				if (scheduler == null)
					scheduler = Kompics.getScheduler();
				scheduler.schedule(this, wid);
			}
			return;
		}

		// 1. pick a port with a non-empty event queue
		// 2. execute the first event
		// 3. make component ready

		PortCore<?> nextPort = readyPorts.poll();

		Event event = nextPort.pickFirstEvent();

		if (event == null) {
			System.err.println("TTTTTTTTTTTT " + nextPort + " in " + this);
		}

		ArrayList<Handler<?>> handlers = nextPort.getSubscribedHandlers(event);

		if (handlers != null) {
			for (int i = 0; i < handlers.size(); i++) {
				executeEvent(event, handlers.get(i));
			}
		}

		int wc = workCount.decrementAndGet();
		if (wc > 0) {
			if (scheduler == null)
				scheduler = Kompics.getScheduler();
			scheduler.schedule(this, wid);
		}
	}

	@SuppressWarnings("unchecked")
	private void executeEvent(Event event, Handler<?> handler) {
		try {
			((Handler<Event>) handler).handle(event);
		} catch (Throwable throwable) {
			handleFault(throwable);
		}

	}

	void handleFault(Throwable throwable) {
		if (parent != null) {
			negativeControl.doTrigger(new Fault(throwable), wid, this);
		} else {
			// StackTraceElement[] stackTrace = throwable.getStackTrace();
			// System.err.println("Kompics isolated fault: "
			// + throwable.getMessage());
			// do {
			// for (int i = 0; i < stackTrace.length; i++) {
			// System.err.println("    " + stackTrace[i]);
			// }
			// throwable = throwable.getCause();
			// if (throwable != null) {
			// stackTrace = throwable.getStackTrace();
			// System.err.println("Caused by: " + throwable + ": "
			// + throwable.getMessage());
			// }
			// } while (throwable != null);
			throw new RuntimeException("Kompics isolated fault ", throwable);
			// System.exit(1);
		}
	}

	// fault handler that swallows exceptions and logs them instead of printing
	// them to stderr and halting

	// void handleFault(Throwable throwable) {
	// if (parent != null) {
	// negativeControl.doTrigger(new Fault(throwable), wid, this);
	// } else {
	// StackTraceElement[] stackTrace = throwable.getStackTrace();
	// Kompics.logger.error("Kompics isolated fault: {}", throwable
	// .getMessage());
	// do {
	// for (int i = 0; i < stackTrace.length; i++) {
	// Kompics.logger.error("    {}", stackTrace[i]);
	// }
	// throwable = throwable.getCause();
	// if (throwable != null) {
	// stackTrace = throwable.getStackTrace();
	// Kompics.logger.error("Caused by: {}: {}", throwable,
	// throwable.getMessage());
	// }
	// } while (throwable != null);
	// }
	// }

	/* === LIFECYCLE === */

	Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			// System.err.println(component + " defaultStart");
			if (children != null) {
				for (ComponentCore child : children) {
					// start child
					((PortCore<ControlPort>) child.getControl()).doTrigger(
							Start.event, wid, component.getComponentCore());
				}
			}
		}
	};

	Handler<Stop> handleStop = new Handler<Stop>() {
		public void handle(Stop event) {
			// System.err.println(component + " defaultStop");
			if (children != null) {
				for (ComponentCore child : children) {
					// stop child
					((PortCore<ControlPort>) child.getControl()).doTrigger(
							Stop.event, wid, component.getComponentCore());
				}
			}
		}
	};

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
}
