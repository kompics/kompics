package se.sics.kompics.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentStartMethod;
import se.sics.kompics.api.annotation.ComponentStopMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventGuardMethod;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.core.scheduler.Scheduler;

/**
 * The core of a component factory for a given component type. It contains the
 * component's event handler methods, creation/destruction methods and
 * life-cycle methods.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
/**
 * @author cosmin
 * 
 */
public class FactoryCore {

	/**
	 * The name of the implementation class of the component type.
	 */
	private String handlerComponentClassName;

	/**
	 * The implementation class of the component type.
	 */
	private Class<?> handlerComponentClass;

	/**
	 * The event handler methods indexed by name.
	 */
	private HashMap<String, Method> eventHandlerMethods = null;

	/**
	 * The number of guarded event handlers.
	 */
	private int guardedHandlersCount;

	/**
	 * The event handler methods indexed by name.
	 */
	private HashMap<String, Class<? extends Event>> eventHandlerInputEventTypes = null;

	/**
	 * The names of the guard methods (if they exist) indexed by event handler
	 * names.
	 */
	private HashMap<String, String> eventHandlerGuardNames = null;

	/**
	 * The arrays of event types possibly triggered by each event handler,
	 * indexed by event handler name.
	 */
	private HashMap<String, Class<? extends Event>[]> eventHandlerOutputEventTypes = null;

	/**
	 * The event handler guard methods indexed by guard name.
	 */
	private HashMap<String, Method> eventGuardMethods = null;

	/**
	 * The constructor of the component implementation class.
	 */
	private Constructor<?> constructor;

	/**
	 * The <code>create</code> method of the component implementation class.
	 */
	private Method createMethod;

	/**
	 * The number of Channel parameters taken by the <code>create</code> method.
	 */
	private int createParameterCount;

	/**
	 * The <code>share</code> method of the component implementation class.
	 */
	private Method shareMethod;

	/**
	 * The <code>initialize</code> method of the component implementation class.
	 */
	private Method initializeMethod;

	/**
	 * The name of the file containing properties used to initialize the
	 * component.
	 */
	private String initializeConfigFileName;

	/**
	 * The <code>destroy</code> method of the component implementation class.
	 */
	private Method destroyMethod;

	/**
	 * The <code>start</code> method of the component implementation class.
	 */
	private Method startMethod;

	/**
	 * The <code>stop</code> method of the component implementation class.
	 */
	private Method stopMethod;

	/**
	 * Constructs a new component factory.
	 * 
	 * @param scheduler
	 *            a reference to the Kompics scheduler.
	 * @param handlerComponentClassName
	 *            the name of the implementation class of the component type.
	 */
	public FactoryCore(String handlerComponentClassName) {
		super();
		this.handlerComponentClassName = handlerComponentClassName;

		try {
			this.handlerComponentClass = Class
					.forName(handlerComponentClassName);
			// this.handlerComponentClass = Class.forName(
			// handlerComponentClassName, true, getClass()
			// .getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"Cannot find component implementation class "
							+ handlerComponentClassName, e);
		}

		this.guardedHandlersCount = 0;
		this.reflectHandlerComponentClass();
	}

	/**
	 * Reflects the implementation class of the component type.
	 */
	private void reflectHandlerComponentClass() {
		if (!handlerComponentClass.isAnnotationPresent(ComponentSpecification.class)) {
			// Annotation[] annotations =
			// handlerComponentClass.getAnnotations();
			throw new RuntimeException("Class " + handlerComponentClassName
					+ " is not an annotated component class.");
		}

		// TODO also reflect declared methods and raise exception if there exist
		// non-public annotated methods

		Method methods[] = handlerComponentClass.getMethods();
		// reflect every method of the class
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];

			// reflect event handler
			if (method.isAnnotationPresent(EventHandlerMethod.class)) {
				// check that event handler method accepts one Event argument
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length != 1) {
					throw new RuntimeException(
							"Event handler method "
									+ method.getName()
									+ " should take one se.sics.kompics.api.Event argument");
				}
				Class<? extends Event> eventType = null;
				if (!Event.class.isAssignableFrom(parameterTypes[0])) {
					throw new RuntimeException(
							"Event handler method "
									+ method.getName()
									+ " should take one se.sics.kompics.api.Event argument");
				} else {
					eventType = parameterTypes[0].asSubclass(Event.class);
				}

				if (eventHandlerMethods == null) {
					eventHandlerMethods = new HashMap<String, Method>();
				}
				eventHandlerMethods.put(method.getName(), method);
				if (eventHandlerInputEventTypes == null) {
					eventHandlerInputEventTypes = new HashMap<String, Class<? extends Event>>();
				}
				eventHandlerInputEventTypes.put(method.getName(), eventType);

				EventHandlerMethod handlerAnnotation = method
						.getAnnotation(EventHandlerMethod.class);
				if (handlerAnnotation.guarded()) {
					if (eventHandlerGuardNames == null) {
						eventHandlerGuardNames = new HashMap<String, String>();
					}
					guardedHandlersCount++;
					eventHandlerGuardNames.put(method.getName(),
							handlerAnnotation.guard());
				}

				// reflect the event types that may be triggered by this handler
				MayTriggerEventTypes eventTypes = method
						.getAnnotation(MayTriggerEventTypes.class);
				if (eventTypes != null) {
					if (eventHandlerOutputEventTypes == null) {
						eventHandlerOutputEventTypes = new HashMap<String, Class<? extends Event>[]>();
					}
					eventHandlerOutputEventTypes.put(method.getName(),
							eventTypes.value());
				}
			}

			// reflect event guard
			if (method.isAnnotationPresent(EventGuardMethod.class)) {
				// check that event handler guard method accepts one Event
				// argument
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length != 1) {
					throw new RuntimeException(
							"Event handler guard method "
									+ method.getName()
									+ " should take one se.sics.kompics.api.Event argument");
				}
				if (!Event.class.isAssignableFrom(parameterTypes[0])) {
					throw new RuntimeException(
							"Event handler guard method "
									+ method.getName()
									+ " should take one se.sics.kompics.api.Event argument");
				}

				if (eventGuardMethods == null) {
					eventGuardMethods = new HashMap<String, Method>();
				}
				eventGuardMethods.put(method.getName(), method);
			}

			// reflect the component's start method
			if (method.isAnnotationPresent(ComponentStartMethod.class)) {
				startMethod = method;
			}
			// reflect the component's stop method
			if (method.isAnnotationPresent(ComponentStopMethod.class)) {
				stopMethod = method;
			}
			// reflect the component's create method
			if (method.isAnnotationPresent(ComponentCreateMethod.class)) {
				// type check the create method
				Class<?>[] parameterTypes = method.getParameterTypes();
				for (int j = 0; j < parameterTypes.length; j++) {
					Class<?> parameterType = parameterTypes[j];
					if (!Channel.class.isAssignableFrom(parameterType)) {
						throw new RuntimeException(
								"The create method for component class "
										+ handlerComponentClassName
										+ " should take 0 or more arguments of type"
										+ " se.sics.kompics.api.Channel.");
					}
				}
				createMethod = method;
				createParameterCount = parameterTypes.length;
			}
			// reflect the component's share method
			if (method.isAnnotationPresent(ComponentShareMethod.class)) {
				// type check the share method
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length != 1) {
					throw new RuntimeException(
							"The share method for component class "
									+ handlerComponentClassName
									+ " should take one java.lang.String argument.");
				}

				if (!String.class.isAssignableFrom(parameterTypes[0])) {
					throw new RuntimeException(
							"The share method for component class "
									+ handlerComponentClassName
									+ " should take one java.lang.String argument.");
				}
				Class<?> returnType = method.getReturnType();
				if (!ComponentMembrane.class.isAssignableFrom(returnType)) {
					throw new RuntimeException(
							"The share method for component class "
									+ handlerComponentClassName
									+ " should return a se.sics.kompics.api.ComponentMembrane argument.");
				}
				shareMethod = method;
			}
			// reflect the component's initialize method
			if (method.isAnnotationPresent(ComponentInitializeMethod.class)) {
				initializeMethod = method;

				ComponentInitializeMethod initializeMethodAnnotation = method
						.getAnnotation(ComponentInitializeMethod.class);
				initializeConfigFileName = initializeMethodAnnotation.value();
			}
			// reflect the component's destroy method
			if (method.isAnnotationPresent(ComponentDestroyMethod.class)) {
				destroyMethod = method;
			}
		}

		// check that the component implementation class declared event handlers
		if (eventHandlerMethods == null) {
			throw new RuntimeException(
					"Component class declared no event handlers found");
		}
		// check that all declared guard names exist and their argument types
		// match the event handler argument types
		if (eventHandlerGuardNames != null) {
			if (eventGuardMethods == null) {
				throw new RuntimeException(
						"Guard names declared but no guard method present.");
			}

			for (Map.Entry<String, String> entry : eventHandlerGuardNames
					.entrySet()) {
				String handlerName = entry.getKey();
				String guardName = entry.getValue();
				Method guardMethod = eventGuardMethods.get(guardName);
				if (guardMethod == null) {
					throw new RuntimeException("Guard name " + guardName
							+ " declared, but no guard method found");
				}
				Method handlerMethod = eventHandlerMethods.get(handlerName);
				Class<?>[] guardParameterTypes = guardMethod
						.getParameterTypes();
				Class<?>[] handlerParameterTypes = handlerMethod
						.getParameterTypes();

				if (!handlerParameterTypes[0].equals(guardParameterTypes[0])) {
					throw new RuntimeException("Arguments for event handler "
							+ handlerName + " and associated guard "
							+ guardName + " do not match.");
				}
			}
		}

		// reflect the constructor of the component implementation instance
		Class<?>[] paramsClasses = new Class<?>[1];
		paramsClasses[0] = Component.class;
		try {
			constructor = handlerComponentClass.getConstructor(paramsClasses);
		} catch (SecurityException e) {
			throw new RuntimeException(
					"Cannot create component factory from class "
							+ handlerComponentClassName, e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(
					"Cannot create component factory from class "
							+ handlerComponentClassName, e);
		}
	}

	public ComponentCore createComponent(Scheduler scheduler,
			FactoryRegistry factoryRegistry, ComponentReference parent,
			Channel faultChannel, Channel... channelParameters) {

		try {
			if (faultChannel == null)
				throw new RuntimeException("FaultChannel cannot be null.");

			// create a component core
			ComponentCore componentCore = new ComponentCore(scheduler,
					factoryRegistry, this, parent, faultChannel);
			ComponentReference componentReference = componentCore
					.createReference();

			// create an instance of the implementing component type
			Object handlerObject = constructor.newInstance(componentReference);

			// create the event handlers
			HashMap<String, EventHandler> eventHandlers = new HashMap<String, EventHandler>();
			for (Map.Entry<String, Method> handlerEntry : eventHandlerMethods
					.entrySet()) {
				String handlerName = handlerEntry.getKey();
				Method handlerMethod = handlerEntry.getValue();
				Class<? extends Event> eventType = eventHandlerInputEventTypes
						.get(handlerName);

				EventHandler eventHandler;
				String guardName;
				if (guardedHandlersCount > 0
						&& (guardName = eventHandlerGuardNames.get(handlerName)) != null) {
					Method guardMethod = eventGuardMethods.get(guardName);
					eventHandler = new EventHandler(handlerObject,
							handlerMethod, guardMethod, eventType);
				} else {
					eventHandler = new EventHandler(handlerObject,
							handlerMethod, eventType);
				}
				eventHandlers.put(handlerName, eventHandler);
			}

			componentCore.setHandlerObject(handlerObject);
			componentCore.setEventHandlers(eventHandlers,
					guardedHandlersCount > 0);

			// invoke the create method
			if (createMethod != null) {
				if (createParameterCount == channelParameters.length) {
					createMethod.invoke(handlerObject,
							(Object[]) channelParameters);
				} else {
					throw new RuntimeException(
							"The create method for component "
									+ handlerComponentClassName
									+ " takes "
									+ createParameterCount
									+ " channel parameters but it was invoked with "
									+ channelParameters.length
									+ " channel parameters");
				}
			}
			return componentCore;
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(
					"Cannot create component instance of type "
							+ handlerComponentClassName, e);
		} catch (InstantiationException e) {
			throw new RuntimeException(
					"Cannot create component instance of type "
							+ handlerComponentClassName, e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(
					"Cannot create component instance of type "
							+ handlerComponentClassName, e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(
					"Cannot create component instance of type "
							+ handlerComponentClassName, e);
		}
	}

	/**
	 * @return the <code>create</code> method of the component implementation
	 *         class.
	 */
	public Method getCreateMethod() {
		return createMethod;
	}

	/**
	 * @return the <code>initialize</code> method of the component
	 *         implementation class.
	 */
	public Method getInitializeMethod() {
		return initializeMethod;
	}

	/**
	 * @return the name of the component configuration properties file if
	 *         annotated in the component class.
	 */
	public String getConfigurationFileName() {
		return initializeConfigFileName;
	}

	/**
	 * @return the <code>share</code> method of the component implementation
	 *         class.
	 */
	public Method getShareMethod() {
		return shareMethod;
	}

	/**
	 * @return the <code>destroy</code> method of the component implementation
	 *         class.
	 */
	public Method getDestroyMethod() {
		return destroyMethod;
	}

	/**
	 * @return the <code>start</code> method of the component implementation
	 *         class.
	 */
	public Method getStartMethod() {
		return startMethod;
	}

	/**
	 * @return the <code>stop</code> method of the component implementation
	 *         class.
	 */
	public Method getStopMethod() {
		return stopMethod;
	}
}
