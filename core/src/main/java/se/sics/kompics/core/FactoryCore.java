package se.sics.kompics.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Factory;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentDestroyMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentStartMethod;
import se.sics.kompics.api.annotation.ComponentStopMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerGuardMethod;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.core.sched.Scheduler;

/**
 * The core of a component factory for a given component type. It contains the
 * component's event handler methods, creation/destruction methods and
 * life-cycle methods.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
public class FactoryCore implements Factory {

	private Scheduler scheduler;

	private String handlerComponentClassName;

	private Class<?> handlerComponentClass;

	private HashMap<String, Method> eventHandlerMethods;

	private HashMap<String, Method> eventHandlerGuardMethods;

	private Constructor<?> constructor;

	private Method createMethod;

	private Method initializeMethod;

	private Method destroyMethod;

	private Method startMethod;

	private Method stopMethod;

	public FactoryCore(Scheduler scheduler, String handlerComponentClassName)
			throws ClassNotFoundException {
		super();
		this.scheduler = scheduler;
		this.handlerComponentClassName = handlerComponentClassName;
		this.handlerComponentClass = Class.forName(handlerComponentClassName,
				true, getClass().getClassLoader());

		this.reflectHandlerComponentClass();
	}

	private void reflectHandlerComponentClass() {
		if (!handlerComponentClass.isAnnotationPresent(ComponentType.class)) {
			throw new RuntimeException("not an annotated component class");
		}

		Method methods[] = handlerComponentClass.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];

			if (method.isAnnotationPresent(EventHandlerMethod.class)) {
				;
			}
			if (method.isAnnotationPresent(EventHandlerGuardMethod.class)) {
				;
			}
			if (method.isAnnotationPresent(ComponentStartMethod.class)) {
				startMethod = method;
			}
			if (method.isAnnotationPresent(ComponentStopMethod.class)) {
				stopMethod = method;
			}
			if (method.isAnnotationPresent(ComponentCreateMethod.class)) {
				createMethod = method;
			}
			if (method.isAnnotationPresent(ComponentInitializeMethod.class)) {
				initializeMethod = method;
			}
			if (method.isAnnotationPresent(ComponentDestroyMethod.class)) {
				destroyMethod = method;
			}
		}
	}

	public Component createComponent(Channel... channelParameters) {
		// TODO test number of parameters
		ComponentCore componentCore = new ComponentCore(scheduler, this, null);
		// TODO load and create the event handlers
		return componentCore;
	}

	public Method getCreateMethod() {
		return createMethod;
	}

	public Method getDestroyMethod() {
		return destroyMethod;
	}

	public Method getStartMethod() {
		return startMethod;
	}

	public Method getStopMethod() {
		return stopMethod;
	}
}
