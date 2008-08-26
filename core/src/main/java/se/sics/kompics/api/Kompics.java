package se.sics.kompics.api;

import java.lang.management.ManagementFactory;
import java.util.HashSet;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.ComponentCore;
import se.sics.kompics.core.ComponentReference;
import se.sics.kompics.core.FactoryCore;
import se.sics.kompics.core.FactoryRegistry;
import se.sics.kompics.core.scheduler.Scheduler;
import se.sics.kompics.management.ComponentMXBean;

/**
 * A Kompics system with a scheduler and an associated set of worker threads.
 * Provides a method to get a bootstrap component, the parent of all components
 * in a Kompics system.
 * 
 * @author Cosmin Arad
 * @since Kompics 0.1
 * @version $Id$
 */
public class Kompics {

	private int workers;

	private ComponentReference bootstrapComponent;

	private ComponentRegistry componentRegistry;

	private static Kompics globalKompics = null;

	public static se.sics.kompics.management.Kompics mbean;
	public static se.sics.kompics.management.Component bootMbean;

	private Scheduler scheduler;

	/**
	 * Creates a Kompics system or virtual node.
	 * 
	 * @param workers
	 *            the number of worker threads for running components.
	 * @param fairnessRate
	 *            the fairness rate of the scheduler. A fairness rate of 0 means
	 *            that the scheduler is not fair. A fairness rate of <i>k</i>,
	 *            for example, means that under contention the scheduler tries
	 *            to execute a lower priority event between every <i>k</i>
	 *            higher priority events.
	 * @throws NullPointerException
	 * @throws MalformedObjectNameException
	 * @throws NotCompliantMBeanException
	 * @throws MBeanRegistrationException
	 * @throws InstanceAlreadyExistsException
	 */
	public Kompics(int workers, int fairnessRate) {
		super();
		this.workers = workers;
		this.bootstrapComponent = null;
		this.componentRegistry = new ComponentRegistry();
		this.scheduler = new Scheduler(workers, fairnessRate);

		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			// Construct the ObjectName for the MBean we will register
			ObjectName name = new ObjectName("se.sics.kompics:type=Kompics");

			// Create the Hello World MBean
			mbean = new se.sics.kompics.management.Kompics(this);
			// Register the Hello World MBean
			mbs.registerMBean(mbean, name);
		} catch (Exception e) {
			throw new RuntimeException("Management exception", e);
		}
	}

	public int getWorkerCount() {
		return workers;
	}

	public void setWorkerCount(int workerCount) {
		this.workers = scheduler.setWorkerCount(workerCount);
	}

	/**
	 * Returns the bootstrap component, the parent of all other components in
	 * the current Kompics system.
	 * 
	 * @return the bootstrap component.
	 */
	public Component getBootstrapComponent() {
		if (bootstrapComponent == null) {
			HashSet<Class<? extends Event>> eventTypes = new HashSet<Class<? extends Event>>();
			eventTypes.add(FaultEvent.class);
			ChannelCore bootFaultChannelCore = new ChannelCore(eventTypes);

			FactoryRegistry factoryRegistry = new FactoryRegistry();

			FactoryCore factoryCore = factoryRegistry
					.getFactory("se.sics.kompics.core.BootstrapComponent");

			Channel faultChannel = bootFaultChannelCore.createReference();

			ComponentCore bootstrapCore = factoryCore.createComponent(
					scheduler, factoryRegistry, null, null, faultChannel,
					faultChannel);
			bootMbean = bootstrapCore.mbean;

			bootstrapComponent = bootstrapCore.createReference();
		}
		return bootstrapComponent;
	}

	public ComponentMXBean getBootstrapMbean() {
		return bootMbean;
	}

	/**
	 * Returns the global Kompics system.
	 * 
	 * @return the global Kompics system.
	 */
	public static Kompics getGlobalKompics() {
		return globalKompics;
	}

	/**
	 * Sets the global Kompics system. This method has the effect of setting the
	 * global reference to a Kompics system only the first time it is called.
	 * 
	 * @param kompics
	 *            a newly created Kompics system.
	 * 
	 */
	public static void setGlobalKompics(Kompics kompics) {
		if (Kompics.globalKompics == null) {
			Kompics.globalKompics = kompics;
		}
	}

	public ComponentRegistry getComponentRegistry() {
		return componentRegistry;
	}
}
