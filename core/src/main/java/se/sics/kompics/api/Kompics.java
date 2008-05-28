package se.sics.kompics.api;

import java.util.HashSet;

import se.sics.kompics.core.ChannelCore;
import se.sics.kompics.core.ComponentReference;
import se.sics.kompics.core.FactoryCore;
import se.sics.kompics.core.FactoryRegistry;
import se.sics.kompics.core.scheduler.Scheduler;

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

	private int fairnessRate;

	private ComponentReference bootstrapComponent;

	private ComponentRegistry componentRegistry;

	private static Kompics globalKompics = null;

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
	 */
	public Kompics(int workers, int fairnessRate) {
		super();
		this.workers = workers;
		this.fairnessRate = fairnessRate;
		this.bootstrapComponent = null;
		this.componentRegistry = new ComponentRegistry();
	}

	/**
	 * Returns the bootstrap component, the parent of all other components in
	 * the current Kompics system.
	 * 
	 * @return the bootstrap component.
	 */
	public Component getBootstrapComponent() {
		if (bootstrapComponent == null) {
			Scheduler scheduler = new Scheduler(workers, fairnessRate);
			ChannelCore bootFaultChannelCore = new ChannelCore(
					new HashSet<Class<? extends Event>>());
			bootFaultChannelCore.addEventType(FaultEvent.class);

			FactoryRegistry factoryRegistry = new FactoryRegistry();

			FactoryCore factoryCore = factoryRegistry
					.getFactory("se.sics.kompics.core.BootstrapComponent");

			Channel faultChannel = bootFaultChannelCore.createReference();

			bootstrapComponent = factoryCore.createComponent(scheduler,
					factoryRegistry, null, faultChannel, faultChannel)
					.createReference();
		}
		return bootstrapComponent;
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
