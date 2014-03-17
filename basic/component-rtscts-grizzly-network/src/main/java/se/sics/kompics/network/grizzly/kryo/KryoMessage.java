package se.sics.kompics.network.grizzly.kryo;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.UUID;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.compress.DeflateCompressor;
import com.esotericsoftware.kryo.serialize.FieldSerializer;
import com.esotericsoftware.kryo.serialize.SimpleSerializer;
import com.google.common.collect.ImmutableSet;

public class KryoMessage {

	// registered message types
	private static LinkedList<Class<?>> registeredMessageTypes = new LinkedList<Class<?>>();

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
                //ImmutableCollectionsSerializer.registerSerializers(kryo);
		kryo.register(InetAddress.class, new SimpleSerializer<InetAddress>() {
			@Override
			public InetAddress read(ByteBuffer buffer) {
				
				byte[] adrbytes = new byte[4];
				buffer.get(adrbytes);
				try {
					InetAddress adr = InetAddress.getByAddress(adrbytes);
					//System.out.println("Used InetDeSerialiser for " + adr.getHostAddress());
					return adr;
				} catch(Exception ex) {
					throw new RuntimeException(ex);
				}

			}

			@Override
			public void write(ByteBuffer buffer, InetAddress adr) {
				//System.out.println("Using InetSerialiser for " + adr.getHostAddress());
				byte[] adrbytes = adr.getAddress();
				if (adrbytes.length != 4) {
					throw new RuntimeException("Address " + adr.getHostAddress() + " has a weird format ("+adrbytes.length+") : " + adrbytes);
				}
				buffer.put(adrbytes);
			}
		});
		kryo.register(Inet4Address.class, new SimpleSerializer<Inet4Address>() {
			@Override
			public Inet4Address read(ByteBuffer buffer) {
				
				byte[] adrbytes = new byte[4];
				buffer.get(adrbytes);
				try {
					Inet4Address adr = (Inet4Address) Inet4Address.getByAddress(adrbytes);
					//System.out.println("Used Inet4DeSerialiser for " + adr.getHostAddress());
					return adr;
				} catch(Exception ex) {
					throw new RuntimeException(ex);
				}

			}

			@Override
			public void write(ByteBuffer buffer, Inet4Address adr) {
				//System.out.println("Using Inet4Serialiser for " + adr.getHostAddress());
				byte[] adrbytes = adr.getAddress();
				if (adrbytes.length != 4) {
					throw new RuntimeException("Address " + adr.getHostAddress() + " has a weird format ("+adrbytes.length+") : " + adrbytes);
				}
				buffer.put(adrbytes);
			}
		});
		kryo.register(Address.class);
		if (compress) {
			kryo.register(Message.class, new DeflateCompressor(
					new FieldSerializer(kryo, Message.class)));
		} else {
			kryo.register(Message.class);
		}
	}

}
