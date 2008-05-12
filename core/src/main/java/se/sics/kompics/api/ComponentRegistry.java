package se.sics.kompics.api;

import java.util.HashMap;

public class ComponentRegistry {

	private final HashMap<String, ComponentMembrane> membranes;

	ComponentRegistry() {
		membranes = new HashMap<String, ComponentMembrane>();
	}

	public ComponentMembrane register(String name, ComponentMembrane membrane) {
		return membranes.put(name, membrane);
	}

	public ComponentMembrane getMembrane(String name) {
		return membranes.get(name);
	}
}
