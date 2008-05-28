package se.sics.kompics.core;

import java.util.HashMap;

public class FactoryRegistry {

	private final HashMap<String, FactoryCore> factories;

	public FactoryRegistry() {
		this.factories = new HashMap<String, FactoryCore>();
	}

	public synchronized FactoryCore getFactory(String className) {
		FactoryCore factoryCore = factories.get(className);

		if (factoryCore != null) {
			return factoryCore;
		}

		factoryCore = new FactoryCore(className);
		factories.put(className, factoryCore);
		return factoryCore;
	}
}
