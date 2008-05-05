package se.sics.kompics.core;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.Factory;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.capability.ChannelCapabilityFlags;
import se.sics.kompics.api.capability.ComponentCapabilityFlags;
import se.sics.kompics.core.config.ConfigurationException;
import se.sics.kompics.core.scheduler.ComponentState;
import se.sics.kompics.core.scheduler.ReadyComponent;
import se.sics.kompics.core.scheduler.Scheduler;
import se.sics.kompics.core.scheduler.Work;
import se.sics.kompics.core.scheduler.WorkQueue;

/**
 * The core of a component. It contains scheduling data, configuration data,
 * life-cycle data, and methods for triggering events, reconfiguration and
 * life-cycle.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
public class ComponentCore {

	private ComponentUUID componentIdentifier;

	private String componentName;

	private FactoryCore factoryCore;

	private Channel faultChannel;

	private Object handlerObject;

	private HashMap<String, EventHandler> eventHandlers;

	private HashSet<EventHandler> guardedHandlersWithBlockedEvents;

	private int blockedEventsCount;

	/* =============== COMPONENT CONFIGURATION =============== */

	private HashMap<Class<? extends Event>, Binding> bindings;

	private LinkedList<Subscription> subscriptions;

	/* =============== EVENT SCHEDULING =============== */

	private Scheduler scheduler;

	private ComponentState componentState;
	private int allWorkCounter;
	private int highWorkCounter;
	private int mediumWorkCounter;
	private int lowWorkCounter;
	private Object componentStateLock;

	private HashMap<ChannelCore, WorkQueue> channelWorkQueues;

	private LinkedHashSet<WorkQueue> highWorkQueuePool;
	private LinkedHashSet<WorkQueue> mediumWorkQueuePool;
	private LinkedHashSet<WorkQueue> lowWorkQueuePool;

	// to sync executing thread with publishing thread for pool selection
	private int highPoolCounter;
	private int mediumPoolCounter;
	private int lowPoolCounter;

	public ComponentCore(Scheduler scheduler, FactoryCore factoryCore,
			Channel faultChannel) {
		super();
		this.scheduler = scheduler;
		this.factoryCore = factoryCore;

		// check fault channel
		if (!faultChannel.hasEventType(FaultEvent.class)) {
			throw new RuntimeException(
					"Provided fault channel does not have the FaultEvent type");
		}
		this.faultChannel = faultChannel;
		this.componentIdentifier = new ComponentUUID();

		this.bindings = new HashMap<Class<? extends Event>, Binding>();
		this.subscriptions = new LinkedList<Subscription>();

		this.componentState = ComponentState.ASLEEP;
		this.allWorkCounter = 0;
		this.highWorkCounter = 0;
		this.mediumWorkCounter = 0;
		this.lowWorkCounter = 0;
		this.componentStateLock = new Object();

		this.channelWorkQueues = new HashMap<ChannelCore, WorkQueue>();

		this.highWorkQueuePool = new LinkedHashSet<WorkQueue>();
		this.mediumWorkQueuePool = new LinkedHashSet<WorkQueue>();
		this.lowWorkQueuePool = new LinkedHashSet<WorkQueue>();

		this.highPoolCounter = 0;
		this.mediumPoolCounter = 0;
		this.lowPoolCounter = 0;
	}

	public void setHandlerObject(Object handlerObject) {
		this.handlerObject = handlerObject;
		this.componentName = handlerObject.getClass().getSimpleName() + "@"
				+ Integer.toHexString(this.hashCode());
	}

	public void setEventHandlers(HashMap<String, EventHandler> eventHandlers,
			boolean hasGuarded) {
		this.eventHandlers = eventHandlers;
		if (hasGuarded) {
			this.guardedHandlersWithBlockedEvents = new HashSet<EventHandler>();
			this.blockedEventsCount = 0;
		}
	}

	/* =============== EVENT TRIGGERING =============== */

	/**
	 * triggers an event
	 * 
	 * @param event
	 *            the triggered event
	 */
	public void triggerEvent(Event event) {
		Binding binding = bindings.get(event.getClass());

		if (binding == null) {
			Class<?> eventType = event.getClass();

			while (binding == null && eventType != Object.class) {
				eventType = eventType.getSuperclass();
				binding = bindings.get(eventType);
			}

			if (binding != null) {
				Binding newBinding = new Binding(binding.getComponent(),
						binding.getChannel(), event.getClass());
				bindings.put(event.getClass(), newBinding);
				newBinding.getChannel().addBinding(newBinding);
			} else if (eventType == Object.class) {
				throw new ConfigurationException("Event type "
						+ event.getClass().getCanonicalName() + " not bound");
			}
		}

		EventCore eventCore = new EventCore(event, binding.getChannel(),
				Priority.MEDIUM);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, Priority priority) {
		Binding binding = bindings.get(event.getClass());

		if (priority == null)
			throw new RuntimeException("triggered event with null priority");

		if (binding == null)
			throw new ConfigurationException("Event type "
					+ event.getClass().getCanonicalName() + " not bound");

		EventCore eventCore = new EventCore(event, binding.getChannel(),
				priority);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, Channel channel) {
		EventCore eventCore = new EventCore(event, (ChannelReference) channel,
				Priority.MEDIUM);
		triggerEventCore(eventCore);
	}

	public void triggerEvent(Event event, Channel channel, Priority priority) {
		if (priority == null)
			throw new RuntimeException("triggered event with null priority");

		EventCore eventCore = new EventCore(event, (ChannelReference) channel,
				priority);
		triggerEventCore(eventCore);
	}

	private void triggerEventCore(EventCore eventCore) {
		ChannelReference channelReference = eventCore.getChannel();
		channelReference.publishEventCore(eventCore);
	}

	/* =============== EVENT SCHEDULING =============== */

	// many publisher threads can call this method but they shall synchronize on
	// the work queue and on the component state lock
	public void handleWork(Work work) {
		WorkQueue workQueue = channelWorkQueues.get(work.getChannelCore());
		workQueue.add(work);

		// we make the component ready, if passive
		synchronized (componentStateLock) {
			allWorkCounter++;
			switch (work.getPriority()) {
			case HIGH:
				highWorkCounter++;
				break;
			case MEDIUM:
				mediumWorkCounter++;
				break;
			case LOW:
				lowWorkCounter++;
				break;
			}

			if (componentState == ComponentState.ASLEEP) {
				componentState = ComponentState.AWAKE;

				// System.out.println("HCR:" + highWorkCounter + ":"
				// + mediumWorkCounter + ":" + lowWorkCounter + "=="
				// + highPoolCounter + ":" + mediumPoolCounter + ":"
				// + lowPoolCounter);

				scheduler.componentReady(new ReadyComponent(this,
						highWorkCounter, mediumWorkCounter, lowWorkCounter,
						work.getPriority(), null));
			} else {
				// System.out.println("HPE:" + highWorkCounter + ":"
				// + mediumWorkCounter + ":" + lowWorkCounter + "=="
				// + highPoolCounter + ":" + mediumPoolCounter + ":"
				// + lowPoolCounter);

				scheduler.publishedEvent(work.getPriority());
			}
		}
	}

	/*
	 * only one thread at a time calls this method. causes the component to
	 * execute one event of the given priority. it is guaranteed that the
	 * component has such an event. If an event of the given priority is not
	 * found at the head of a channel queue, a lower priority event is executed,
	 * if one is available, otherwise a higher priority one.
	 */
	public void schedule(Priority priority) {
		// set the thread local component identifier. The thread executes now on
		// behalf of this component.
		ComponentUUID.set(componentIdentifier);
		Thread.currentThread().setName(componentName);

		// pick a work queue, if possible from the given priority pool
		WorkQueue workQueue = pickWorkQueue(priority);

		// take from it
		Work work = workQueue.take();

		// execute the taken work
		EventHandler eventHandler = work.getEventHandler();
		Event event = work.getEventCore().getEvent();
		boolean handled = false;

		// isolate any possible errors or exceptions while executing event
		// handlers and guard methods
		try {
			handled = eventHandler.handleEvent(event);

			if (!handled) {
				// the event handler was guarded and the guard was not satisfied
				blockedEventsCount++;
				guardedHandlersWithBlockedEvents.add(eventHandler);
			}

			// try to execute blocked event handlers until no more possible
			while (handled && hasBlockedEvents()) {
				handled = handleOneBlockedEvent();
			}

		} catch (Throwable throwable) {
			handleFault(throwable);
		}

		// make the component passive or ready
		synchronized (componentStateLock) {
			allWorkCounter--;
			switch (work.getPriority()) {
			case HIGH:
				highWorkCounter--;
				break;
			case MEDIUM:
				mediumWorkCounter--;
				break;
			case LOW:
				lowWorkCounter--;
				break;
			}

			if (allWorkCounter == 0) {
				// System.out.println("SEE:" + highWorkCounter + ":"
				// + mediumWorkCounter + ":" + lowWorkCounter + "=="
				// + highPoolCounter + ":" + mediumPoolCounter + ":"
				// + lowPoolCounter);

				componentState = ComponentState.ASLEEP;
				scheduler.executedEvent(work.getPriority());
			} else if (allWorkCounter > 0) {
				// System.out.println("SCR:" + highWorkCounter + ":"
				// + mediumWorkCounter + ":" + lowWorkCounter + "=="
				// + highPoolCounter + ":" + mediumPoolCounter + ":"
				// + lowPoolCounter);

				componentState = ComponentState.AWAKE;
				scheduler.componentReady(new ReadyComponent(this,
						highWorkCounter, mediumWorkCounter, lowWorkCounter,
						null, work.getPriority()));
			} else {
				throw new RuntimeException("Negative work counter");
			}
		}
	}

	/**
	 * Tries to execute one guarded event handler.
	 * 
	 * @return <code>true</code> if one blocked event was executed from any
	 *         guarded event handler and <code>false</code> if no blocked
	 *         event could be executed due to no satisfied guard
	 */
	private boolean handleOneBlockedEvent() throws Throwable {
		Iterator<EventHandler> iterator = guardedHandlersWithBlockedEvents
				.iterator();

		// try every guarded event handler with blocked events until one
		// bocked event is handled
		while (iterator.hasNext()) {
			EventHandler eventHandler = iterator.next();

			boolean handled = eventHandler.handleOneBlockedEvent();
			if (handled) {
				if (!eventHandler.hasBlockedEvents()) {
					iterator.remove();
				}
				return true;
			}
		}
		return false;
	}

	private boolean hasBlockedEvents() {
		return blockedEventsCount > 0;
	}

	/*
	 * synchronized with moveWorkQueue between executing thread (calling
	 * pickWorkQueue) and publisher thread (calling move...)
	 */
	private synchronized WorkQueue pickWorkQueue(Priority priority) {
		if (priority == Priority.MEDIUM) {
			// this component has been scheduled to execute a MEDIUM event, so
			// it must have a MEDIUM event, in a channel that can only be in the
			// MEDIUM or HIGH pools.
			if (mediumPoolCounter > 0) {
				Iterator<WorkQueue> iterator = mediumWorkQueuePool.iterator();
				return iterator.next();
			} else if (highPoolCounter > 0) {
				Iterator<WorkQueue> iterator = highWorkQueuePool.iterator();
				return iterator.next();
			} else {
				throw new RuntimeException(
						"scheduled MEDIUM but both MEDIUM and HIGH pools empty");
			}
		} else if (priority == Priority.HIGH) {
			// this component has been scheduled to execute a HIGH event, so it
			// must have a HIGH event in a channel that can only be in the HIGH
			// pool.
			if (highPoolCounter > 0) {
				Iterator<WorkQueue> iterator = highWorkQueuePool.iterator();
				return iterator.next();
			} else {
				throw new RuntimeException("scheduled HIGH but HIGH pool empty");
			}
		} else if (priority == Priority.LOW) {
			// this component has been scheduled to execute a LOW event, so it
			// must have a LOW event in a channel that can be in the LOW, MEDIUM
			// or HIGH pools.
			if (lowPoolCounter > 0) {
				Iterator<WorkQueue> iterator = lowWorkQueuePool.iterator();
				return iterator.next();
			} else if (highPoolCounter > 0) {
				Iterator<WorkQueue> iterator = highWorkQueuePool.iterator();
				return iterator.next();
			} else if (mediumPoolCounter > 0) {
				Iterator<WorkQueue> iterator = mediumWorkQueuePool.iterator();
				return iterator.next();
			} else {
				throw new RuntimeException("scheduled LOW but all pools empty");
			}
		} else {
			throw new RuntimeException("Bad priority");
		}
	}

	/*
	 * called by the WorkQueue to move itself to the end of the priority pool,
	 * maybe to a different priority pool. Both from and to can be null.
	 */
	public synchronized void moveWorkQueueToPriorityPool(WorkQueue workQueue,
			Priority from, Priority to) {

		if (from != null) {
			// constant-time removal
			switch (from) {
			case LOW:
				lowWorkQueuePool.remove(workQueue);
				lowPoolCounter--;
				break;
			case MEDIUM:
				mediumWorkQueuePool.remove(workQueue);
				mediumPoolCounter--;
				break;
			case HIGH:
				highWorkQueuePool.remove(workQueue);
				highPoolCounter--;
				break;
			}
		}

		if (to != null) {
			// constant-time addition
			switch (to) {
			case LOW:
				lowWorkQueuePool.add(workQueue);
				lowPoolCounter++;
				break;
			case MEDIUM:
				mediumWorkQueuePool.add(workQueue);
				mediumPoolCounter++;
				break;
			case HIGH:
				highWorkQueuePool.add(workQueue);
				highPoolCounter++;
				break;
			}
		}
	}

	/* =============== COMPONENT COMPOSITION =============== */

	public ChannelReference createChannel() {
		ChannelCore channelCore = new ChannelCore();
		return new ChannelReference(channelCore, EnumSet
				.allOf(ChannelCapabilityFlags.class));
	}

	public ChannelReference getFaultChannel() {
		return (ChannelReference) faultChannel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.api.Component#createFactory(java.lang.String)
	 */
	public Factory createFactory(String handlerComponentClassName) {
		return new FactoryCore(scheduler, handlerComponentClassName);
	}

	/* =============== COMPONENT CONFIGURATION =============== */

	public void bind(ComponentReference componentReference,
			Class<? extends Event> eventType, Channel channel) {
		// TODO bind synchronization
		// TODO bind type check
		// TODO check MayTriggerEvents

		ChannelReference channelReference = (ChannelReference) channel;
		Binding binding = new Binding(componentReference, channelReference,
				eventType);

		bindings.put(eventType, binding);
		channelReference.addBinding(binding);
	}

	public void unbind(Class<? extends Event> eventType, Channel channel) {
		// TODO unbind
	}

	public void subscribe(ComponentReference componentReference,
			Channel channel, String eventHandlerName) {
		ChannelReference channelReference = (ChannelReference) channel;
		EventHandler eventHandler = eventHandlers.get(eventHandlerName);
		if (eventHandler != null) {
			// TODO subscribe type check

			Subscription subscription = new Subscription(componentReference,
					channelReference, eventHandler);

			// TODO subscribe synchronization

			subscriptions.add(subscription);
			ChannelCore channelCore = channelReference
					.addSubscription(subscription);
			if (!channelWorkQueues.containsKey(channelCore)) {
				WorkQueue workQueue = new WorkQueue(this);
				channelWorkQueues.put(channelCore, workQueue);
			}
		} else {
			throw new RuntimeException("I have no eventHandler named "
					+ eventHandlerName);
		}
	}

	public void unsubscribe(Channel channel, String eventHandlerName) {
		// TODO unsubscribe
	}

	/* =============== COMPONENT LIFE-CYCLE =============== */
	public void start() {
		try {
			Method startMethod = factoryCore.getStartMethod();
			if (startMethod != null) {
				startMethod.invoke(handlerObject);
			}
		} catch (Throwable throwable) {
			handleFault(throwable);
		}
		// TODO start
	}

	public void stop() {
		try {
			Method stopMethod = factoryCore.getStopMethod();
			if (stopMethod != null) {
				stopMethod.invoke(handlerObject);
			}
		} catch (Throwable throwable) {
			handleFault(throwable);
		}
		// TODO stop
	}

	/* =============== COMPONENT FAULT-HANDLING =============== */
	private void handleFault(Throwable throwable) {
		System.out.println("ISOLATED EXCEPTION");

		// filter out stack frames showing Kompics internals
		StackTraceElement stackTrace[] = throwable.getCause().getStackTrace();
		int i;
		boolean hitInvoke = false;
		for (i = stackTrace.length - 1; i >= 0; i--) {
			if (!hitInvoke
					&& stackTrace[i].getClassName().equals(
							"se.sics.kompics.core.EventHandler")
					&& stackTrace[i].getMethodName().equals("handleEvent")) {
				hitInvoke = true;
				continue;
			}
			if (hitInvoke && !stackTrace[i].getMethodName().equals("invoke0")
					&& !stackTrace[i].getMethodName().equals("invoke")) {
				i++;
				break;
			}
		}
		if (i > 0) {
			StackTraceElement[] newStackTrace = new StackTraceElement[i];
			for (int j = 0; j < i; j++) {
				newStackTrace[j] = stackTrace[j];
			}
			throwable.getCause().setStackTrace(newStackTrace);
		}
		triggerEvent(new FaultEvent(throwable.getCause()), faultChannel);
	}

	public ComponentReference createReference() {
		return new ComponentReference(this, componentIdentifier, EnumSet
				.allOf(ComponentCapabilityFlags.class));
	}
}
