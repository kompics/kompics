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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The <code>ComponentCore</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id$
 */
public class JavaComponent extends ComponentCore {

	/* outside ports */
	private HashMap<Class<? extends PortType>, JavaPort<? extends PortType>> positivePorts;
	private HashMap<Class<? extends PortType>, JavaPort<? extends PortType>> negativePorts;

	private JavaPort<ControlPort> positiveControl, negativeControl;

	ComponentDefinition component;

	

	/**
	 * Instantiates a new component core.
	 * 
	 * @param componentDefinition
	 *            the component definition
	 */
	public JavaComponent(ComponentDefinition componentDefinition) {
		this.positivePorts = new HashMap<Class<? extends PortType>, JavaPort<? extends PortType>>();
		this.negativePorts = new HashMap<Class<? extends PortType>, JavaPort<? extends PortType>>();
		this.parent = parentThreadLocal.get();
		this.component = componentDefinition;
		this.initSubscriptionInConstructor = false;
		this.initDone = new AtomicBoolean(false);
		this.initReceived = new AtomicBoolean(false);
		this.firstInitEvent = new AtomicReference<Event>(null);
		parentThreadLocal.set(null);
	}
	
	public JavaComponent(JavaComponent other) {
		this.positivePorts = other.positivePorts;
		this.negativePorts = other.negativePorts;
		this.parent = other.parent;
		this.component = other.component;
		this.initSubscriptionInConstructor = other.initSubscriptionInConstructor;
		this.initDone = other.initDone;
		this.initReceived = other.initReceived;
		this.firstInitEvent = other.firstInitEvent;
		parentThreadLocal.set(null);
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

	public <P extends PortType> Negative<P> createNegativePort(Class<P> portType) {
		JavaPort<P> negativePort = new JavaPort<P>(false,
				PortType.getPortType(portType), this);
		JavaPort<P> positivePort = new JavaPort<P>(true,
				PortType.getPortType(portType), parent);

		negativePort.setPair(positivePort);
		positivePort.setPair(negativePort);

		Positive<?> existing = positivePorts.put(portType, positivePort);
		if (existing != null)
			throw new RuntimeException("Cannot create multiple negative "
					+ portType.getCanonicalName());
		return negativePort;
	}

	public <P extends PortType> Positive<P> createPositivePort(Class<P> portType) {
		JavaPort<P> negativePort = new JavaPort<P>(false,
				PortType.getPortType(portType), parent);
		JavaPort<P> positivePort = new JavaPort<P>(true,
				PortType.getPortType(portType), this);

		negativePort.setPair(positivePort);
		positivePort.setPair(negativePort);

		Negative<?> existing = negativePorts.put(portType, negativePort);
		if (existing != null)
			throw new RuntimeException("Cannot create multiple positive "
					+ portType.getCanonicalName());
		return positivePort;
	}

	public Negative<ControlPort> createControlPort() {
		negativeControl = new JavaPort<ControlPort>(false,
				PortType.getPortType(ControlPort.class), this);
		positiveControl = new JavaPort<ControlPort>(true,
				PortType.getPortType(ControlPort.class), parent);

		positiveControl.setPair(negativeControl);
		negativeControl.setPair(positiveControl);

		negativeControl.doSubscribe(handleStart);
		negativeControl.doSubscribe(handleStop);

		return negativeControl;
	}

	public Component doCreate(Class<? extends ComponentDefinition> definition) {
		// create an instance of the implementing component type
		ComponentDefinition component;
		try {
			parentThreadLocal.set(this);
			component = definition.newInstance();
			ComponentCore child = component.getComponentCore();

			if (!child.initSubscriptionInConstructor) {
				child.initDone.set(true);
			} else {
				child.workCount.incrementAndGet();
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

	

	

	

	

	

	

	public void execute(int wid) {
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
			int wc = workCount.decrementAndGet();
			if (wc > 0) {
				if (scheduler == null)
					scheduler = Kompics.getScheduler();
				scheduler.schedule(this, wid);
			}
			return;
		}
		
//		New scheduling code: Run n and move to end of schedule
//		
		int n = Kompics.maxNumOfExecutedEvents.get();
		int count = 0;
		int wc = workCount.get();
		
		while ((count < n) && wc > 0) {
			JavaPort<?> nextPort = (JavaPort<?>) readyPorts.poll();
			
			Event event = nextPort.pickFirstEvent();
			
			ArrayList<Handler<?>> handlers = nextPort.getSubscribedHandlers(event);

			if (handlers != null) {
				for (int i = 0; i < handlers.size(); i++) {
					executeEvent(event, handlers.get(i));
				}
			}
			wc = workCount.decrementAndGet();
			count++;
		}
		
		if (wc > 0) {
			if (scheduler == null)
				scheduler = Kompics.getScheduler();
			scheduler.schedule(this, wid);
		}
		
//		Classic scheduling code: Run once and move to end of schedule
//		
//		// 1. pick a port with a non-empty event queue
//		// 2. execute the first event
//		// 3. make component ready
//
//		JavaPort<?> nextPort = (JavaPort<?>) readyPorts.poll();
//
//		Event event = nextPort.pickFirstEvent();
//
//		ArrayList<Handler<?>> handlers = nextPort.getSubscribedHandlers(event);
//
//		if (handlers != null) {
//			for (int i = 0; i < handlers.size(); i++) {
//				executeEvent(event, handlers.get(i));
//			}
//		}
//
//		int wc = workCount.decrementAndGet();
//		if (wc > 0) {
//			if (scheduler == null)
//				scheduler = Kompics.getScheduler();
//			scheduler.schedule(this, wid);
//		}
	}

	@SuppressWarnings("unchecked")
	private void executeEvent(Event event, Handler<?> handler) {
		try {
			((Handler<Event>) handler).handle(event);
		} catch (Throwable throwable) {
			handleFault(throwable);
		}

	}

	public void handleFault(Throwable throwable) {
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

		public java.lang.Class<Start> getEventType() {
			return Start.class;
		};
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

		public java.lang.Class<Stop> getEventType() {
			return Stop.class;
		};
	};

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public ComponentDefinition getComponent() {
		return component;
	}
}
