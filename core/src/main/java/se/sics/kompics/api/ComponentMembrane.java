package se.sics.kompics.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ComponentMembrane {

	private final HashMap<Class<? extends Event>, Channel> channels;

	public ComponentMembrane(Map<Class<? extends Event>, Channel> map) {
		channels = new HashMap<Class<? extends Event>, Channel>();
		Set<Map.Entry<Class<? extends Event>, Channel>> set = map.entrySet();
		for (Map.Entry<Class<? extends Event>, Channel> entry : set) {
			channels.put(entry.getKey(), entry.getValue());
		}
	}

	public Channel getChannel(Class<? extends Event> eventType) {
		return channels.get(eventType);
	}
}
