package se.sics.kompics.core;

import java.lang.reflect.Method;
import java.util.HashMap;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.Factory;
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

	private Method createMethod;

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

		// load the handlers
	}

	public Component createComponent(Channel... channelParameters) {
		// TODO test number of parameters
		ComponentCore componentCore = new ComponentCore(scheduler, null);
		// TODO load and create the event handlers
		return componentCore;
	}
}
