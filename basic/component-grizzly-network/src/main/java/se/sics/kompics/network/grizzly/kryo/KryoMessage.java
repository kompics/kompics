package se.sics.kompics.network.grizzly.kryo;

import java.net.Inet4Address;
import java.util.HashSet;

import se.sics.kompics.network.Message;

import com.esotericsoftware.kryo.Kryo;

public class KryoMessage {

	// registered message types
	private static HashSet<Class<?>> registeredMessageTypes = new HashSet<Class<?>>();

	private static boolean registered = false;

	static void registerMessages(Kryo kryo) {
		kryo.register(Inet4Address.class);
		kryo.register(Message.class);
		synchronized (registeredMessageTypes) {
			for (Class<?> type : registeredMessageTypes) {
				kryo.register(type);
			}
			registered = true;
		}
		kryo.setRegistrationOptional(true);
	}

	public static void register(Class<?> type) {
		synchronized (registeredMessageTypes) {
			if (registered) {
				throw new RuntimeException(
						"Message type registration already done");
			}
			registeredMessageTypes.add(type);
		}
	}

}
