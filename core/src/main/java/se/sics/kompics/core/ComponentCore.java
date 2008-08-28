package se.sics.kompics.core;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.EventAttributeFilter;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.Kompics;
import se.sics.kompics.api.Priority;
import se.sics.kompics.api.capability.ComponentCapabilityFlags;
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
public class ComponentCore implements Runnable {

	private ComponentUUID componentIdentifier;

	private String componentName;

	private FactoryCore factoryCore;

	private FactoryRegistry factoryRegistry;

	private Channel faultChannel;

	private Object handlerObject;

	private HashMap<String, EventHandler> eventHandlers;

	private HashSet<EventHandler> guardedHandlersWithBlockedEvents;

	private int blockedEventsCount;

	/* =============== COMPONENT COMPOSITION =============== */

	private Component superComponent;

	private ComponentCore superComponentCore;

	private LinkedList<Component> subComponents;

	private LinkedList<ComponentCore> subComponentCores;

	private LinkedList<Channel> localChannels;

	private LinkedList<ChannelCore> localChannelCores;

	/* =============== COMPONENT CONFIGURATION =============== */

	private LinkedList<Subscription> subscriptions;

	/* =============== EVENT SCHEDULING =============== */

	private Scheduler scheduler;

	private AtomicInteger workCounter;

	private HashMap<ChannelCore, WorkQueue> channelWorkQueues;

	private ConcurrentLinkedQueue<WorkQueue> workQueuePool;

	public se.sics.kompics.management.Component mbean;

	public ComponentCore(Scheduler scheduler, FactoryRegistry factoryRegistry,
			FactoryCore factoryCore, ComponentReference parent,
			ComponentCore parentCore, Channel faultChannel) {
		super();
		this.scheduler = scheduler;
		this.factoryRegistry = factoryRegistry;
		this.factoryCore = factoryCore;

		this.superComponent = parent;
		this.superComponentCore = parentCore;
		this.subComponents = new LinkedList<Component>();
		this.localChannels = new LinkedList<Channel>();
		this.subComponentCores = new LinkedList<ComponentCore>();
		this.localChannelCores = new LinkedList<ChannelCore>();

		// check fault channel
		if (!faultChannel.hasEventType(FaultEvent.class)) {
			throw new RuntimeException(
					"Provided fault channel does not have the FaultEvent type");
		}
		this.faultChannel = faultChannel;
		this.componentIdentifier = new ComponentUUID();

		this.subscriptions = new LinkedList<Subscription>();

		this.workCounter = new AtomicInteger(0);

		this.channelWorkQueues = new HashMap<ChannelCore, WorkQueue>();

		this.workQueuePool = new ConcurrentLinkedQueue<WorkQueue>();
	}

	public void setHandlerObject(Object handlerObject) {
		this.handlerObject = handlerObject;
		this.componentName = handlerObject.getClass().getSimpleName() + "@"
				+ Integer.toHexString(this.hashCode());

		if (Kompics.jmxEnabled) {
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				// // Construct the ObjectName for the MBean we will register
				ObjectName name = new ObjectName(
						"se.sics.kompics:type=Component,name=" + componentName);

				// Create the Hello World MBean
				mbean = new se.sics.kompics.management.Component(this,
						componentName);
				// Register the Hello World MBean
				mbs.registerMBean(mbean, name);
			} catch (Exception e) {
				throw new RuntimeException("Management exception", e);
			}
		}
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

		if (Kompics.jmxEnabled) {
			Kompics.mbean.publishedEvent(eventCore.getEvent());
			mbean.publishedEvent(eventCore.getEvent());
		}
	}

	/* =============== EVENT SCHEDULING =============== */

	public void handleWork(Work work) {
		WorkQueue workQueue = channelWorkQueues.get(work.getChannelCore());
		workQueue.add(work);
		workQueuePool.add(workQueue);

		// we make the component ready, if passive
		int oldWorkCounter = workCounter.getAndIncrement();
		if (oldWorkCounter == 0) {
			scheduler.componentReady(this);
		}
	}

	/*
	 * only one thread at a time calls this method. causes the component to
	 * execute one event of the given priority. it is guaranteed that the
	 * component has such an event. If an event of the given priority is not
	 * found at the head of a channel queue, a lower priority event is executed,
	 * if one is available, otherwise a higher priority one.
	 */
	public void run() {
		// set the thread local component identifier. The thread executes now on
		// behalf of this component.
		ComponentUUID.set(componentIdentifier);
		// Thread.currentThread().setName(componentName);

		// pick a work queue, round-robin
		WorkQueue workQueue = workQueuePool.poll();

		// take from it
		Work work = workQueue.take();

		// execute the taken work
		EventHandler eventHandler = work.getEventHandler();
		Event event = work.getEventCore().getEvent();

		Work.release(work);

		// isolate any possible errors or exceptions while executing event
		// handlers and guard methods
		try {
			boolean handled = eventHandler.handleEvent(event);

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

		if (Kompics.jmxEnabled) {
			Kompics.mbean.handledEvent(event);
			mbean.handledEvent(event);
		}

		// make the component passive or ready
		int newWorkCounter = workCounter.decrementAndGet();
		if (newWorkCounter > 0) {
			scheduler.componentReady(this);
		}
	}

	/**
	 * Tries to execute one guarded event handler.
	 * 
	 * @return <code>true</code> if one blocked event was executed from any
	 *         guarded event handler and <code>false</code> if no blocked event
	 *         could be executed due to no satisfied guard
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
	 * called by the WorkQueue to move itself to the end of the priority pool,
	 * maybe to a different priority pool. Both from and to can be null.
	 */
	public void moveWorkQueueToPool(WorkQueue workQueue, boolean remove) {
		if (remove) {
			// constant-time removal
			workQueuePool.remove(workQueue);
		} else {
			// constant-time addition
			workQueuePool.add(workQueue);
		}
	}

	/* =============== COMPONENT COMPOSITION =============== */

	@SuppressWarnings("unchecked")
	ChannelReference createChannel(Class<?>... eventTypes) {
		// type check eventTypes and pass correct types to ChannelCore
		HashSet<Class<? extends Event>> types = new HashSet<Class<? extends Event>>();

		for (Class<?> type : eventTypes) {
			if (Event.class.isAssignableFrom(type)) {
				types.add((Class<? extends Event>) type);
			} else {
				throw new RuntimeException(type.getName() + " is not an event "
						+ "type and it cannot be carried by a channel");
			}
		}

		ChannelCore channelCore = new ChannelCore(types);

		synchronized (localChannelCores) {
			localChannelCores.add(channelCore);
		}

		synchronized (localChannels) {
			localChannels.add(channelCore.createReference());
		}
		return channelCore.createReference();
	}

	ChannelReference getFaultChannel() {
		return (ChannelReference) faultChannel;
	}

	ComponentReference createComponent(String componentClassName,
			Channel faultChannel, Channel... channelParameters) {

		FactoryCore factoryCore = factoryRegistry
				.getFactory(componentClassName);
		ComponentCore newComponentCore = factoryCore.createComponent(scheduler,
				factoryRegistry, createReference(), this, faultChannel,
				channelParameters);

		synchronized (subComponentCores) {
			subComponentCores.add(newComponentCore);
		}

		synchronized (subComponents) {
			subComponents.add(newComponentCore.createReference());
		}

		return newComponentCore.createReference();
	}

	LinkedList<Component> getSubComponents() {
		return subComponents;
	}

	public LinkedList<ComponentCore> getSubComponentCores() {
		return subComponentCores;
	}

	LinkedList<Channel> getLocalChannels() {
		return localChannels;
	}

	public LinkedList<ChannelCore> getLocalChannelCores() {
		return localChannelCores;
	}

	Component getSuperComponent() {
		return superComponent;
	}

	public ComponentCore getParentCore() {
		return superComponentCore;
	}

	public String getName() {
		return componentName;
	}

	/* =============== COMPONENT CONFIGURATION =============== */

	public void subscribe(ComponentReference componentReference,
			Channel channel, String eventHandlerName,
			EventAttributeFilter... filters) {
		ChannelReference channelReference = (ChannelReference) channel;
		EventHandler eventHandler = eventHandlers.get(eventHandlerName);
		if (eventHandler != null) {
			EventAttributeFilterCore[] filterCores;

			Class<? extends Event> eventType = eventHandler.getEventType();

			if (filters.length > 0) {
				filterCores = new EventAttributeFilterCore[filters.length];
				for (int i = 0; i < filterCores.length; i++) {
					try {
						Field attribute = eventType.getField(filters[i]
								.getAttribute());
						filterCores[i] = new EventAttributeFilterCore(
								attribute, filters[i].getValue());
					} catch (SecurityException e) {
						throw new RuntimeException("Subscription by attribute "
								+ "failed: no attribute "
								+ filters[i].getAttribute() + " in event type "
								+ eventType);
					} catch (NoSuchFieldException e) {
						throw new RuntimeException("Subscription by attribute "
								+ "failed: no attribute "
								+ filters[i].getAttribute() + " in event type "
								+ eventType);
					}
				}
			} else {
				filterCores = new EventAttributeFilterCore[0];
			}

			Subscription subscription = new Subscription(componentReference,
					channelReference, eventHandler, filterCores);

			ChannelCore channelCore = channelReference
					.addSubscription(subscription);
			subscriptions.add(subscription);

			// create a local work queue if one does not already exist
			if (!channelWorkQueues.containsKey(channelCore)) {
				WorkQueue workQueue = new WorkQueue();
				channelWorkQueues.put(channelCore, workQueue);
			}
		} else {
			throw new RuntimeException("I have no eventHandler named "
					+ eventHandlerName);
		}
	}

	public void unsubscribe(ComponentReference componentReference,
			Channel channel, String eventHandlerName) {
		ChannelReference channelReference = (ChannelReference) channel;
		EventHandler eventHandler = eventHandlers.get(eventHandlerName);
		if (eventHandler != null) {
			Subscription subscription = null;
			for (Subscription sub : subscriptions) {
				if (sub.getComponent().equals(componentReference)
						&& sub.getChannel().equals(channelReference)
						&& sub.getEventHandler().equals(eventHandler)) {
					subscription = sub;
					break;
				}
			}

			if (subscription != null) {
				// ChannelCore channelCore =
				channelReference.removeSubscription(subscription);
				subscriptions.remove(subscription);
				// TODO refcount work queues and remove them on unsubscription
			} else {
				throw new RuntimeException("I have no subscription of "
						+ "eventHandler " + eventHandlerName
						+ " to this channel");
			}
		} else {
			throw new RuntimeException("I have no eventHandler named "
					+ eventHandlerName);
		}
	}

	/* =============== COMPONENT LIFE-CYCLE =============== */

	public void initialize(Object... args) {
		// TODO uniform fault handling in share and init
		Method initializeMethod = factoryCore.getInitializeMethod();
		Properties initializeConfigProperties = factoryCore
				.getConfigurationProperties();

		if (initializeMethod != null) {
			try {
				// invoke the initialize method ...
				if (args == null) {
					if (initializeConfigProperties != null) {
						// ... with a properties argument
						initializeMethod.invoke(handlerObject,
								initializeConfigProperties);
					} else {
						// ... with no argument
						initializeMethod.invoke(handlerObject);
					}
				} else {
					if (initializeConfigProperties != null) {
						// ... with a properties and init parameters
						// arguments
						Object[] arguments = new Object[args.length + 1];
						for (int i = 0; i < args.length; i++) {
							arguments[i + 1] = args[i];
						}
						arguments[0] = initializeConfigProperties;
						initializeMethod.invoke(handlerObject, arguments);
					} else {
						// ... with init parameters arguments
						initializeMethod.invoke(handlerObject, args);
					}
				}
			} catch (Throwable t) {
				throw new RuntimeException("Cannot initialize component instan"
						+ "ce of type " + handlerObject.getClass(), t);
			}
		} else {
			throw new RuntimeException(
					"Component does not declare an initialize method.");
		}
	}

	public void start() {
		Method startMethod = factoryCore.getStartMethod();
		if (startMethod != null) {
			try {
				startMethod.invoke(handlerObject);
			} catch (Throwable throwable) {
				handleFault(throwable);
			}
		}
		// TODO start
	}

	public void stop() {
		Method stopMethod = factoryCore.getStopMethod();
		if (stopMethod != null) {
			try {
				stopMethod.invoke(handlerObject);
			} catch (Throwable throwable) {
				handleFault(throwable);
			}
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

	/* =============== SHARING =============== */
	public ComponentMembrane registerSharedComponentMembrane(String name,
			ComponentMembrane membrane) {
		return Kompics.getGlobalKompics().getComponentRegistry().register(name,
				membrane);
	}

	public ComponentMembrane getSharedComponentMembrane(String name) {
		return Kompics.getGlobalKompics().getComponentRegistry().getMembrane(
				name);
	}

	public ComponentMembrane share(String name) {
		Method shareMethod = factoryCore.getShareMethod();
		if (shareMethod != null) {
			try {
				Object ret;
				ret = shareMethod.invoke(handlerObject, name);
				ComponentMembrane membrane = (ComponentMembrane) ret;
				return membrane;
			} catch (Throwable t) {
				throw new RuntimeException("Exception in share method", t);
			}
		} else {
			throw new RuntimeException(
					"Component does not declare a share method.");
		}
	}
}
