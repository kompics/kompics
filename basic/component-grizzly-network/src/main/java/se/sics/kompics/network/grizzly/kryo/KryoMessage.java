package se.sics.kompics.network.grizzly.kryo;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.UUID;

import se.sics.kompics.network.Message;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.compress.DeflateCompressor;
import com.esotericsoftware.kryo.serialize.FieldSerializer;
import com.esotericsoftware.kryo.serialize.SimpleSerializer;

public class KryoMessage {

	// registered message types
	private static HashSet<Class<?>> registeredMessageTypes = new HashSet<Class<?>>();

	private static boolean registered = false;

	public static void register(Class<?> type) {
		synchronized (registeredMessageTypes) {
			if (registered) {
				throw new RuntimeException(
						"Message type registration already done");
			}
			registeredMessageTypes.add(type);
		}
	}

	static void registerMessages(Kryo kryo, boolean compress) {
		registerDefaultClasses(kryo, compress);
		synchronized (registeredMessageTypes) {
			for (Class<?> type : registeredMessageTypes) {
				kryo.register(type);
			}
			registered = true;
		}
		kryo.setRegistrationOptional(true);
	}

	private static void registerDefaultClasses(Kryo kryo, boolean compress) {
		kryo.register(UUID.class, new SimpleSerializer<UUID>() {
			@Override
			public UUID read(ByteBuffer buffer) {
				return new UUID(buffer.getLong(), buffer.getLong());
			}

			@Override
			public void write(ByteBuffer buffer, UUID id) {
				buffer.putLong(id.getMostSignificantBits());
				buffer.putLong(id.getLeastSignificantBits());
			}
		});
		kryo.register(Inet4Address.class);
		if (compress) {
			kryo.register(Message.class, new DeflateCompressor(
					new FieldSerializer(kryo, Message.class)));
		} else {
			kryo.register(Message.class);
		}
	}

}
