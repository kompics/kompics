package se.sics.kompics.network.grizzly.kryo;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.BufferOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;

import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;

public class KryoSerializationEncoder extends
		AbstractTransformer<Object, Buffer> {

	private Kryo kryo;
	private int initialBufferCapacity, maxBufferCapacity;

	public KryoSerializationEncoder(boolean compress,
			int initialBufferCapacity, int maxBufferCapacity) {
		this.initialBufferCapacity = initialBufferCapacity;
		this.maxBufferCapacity = maxBufferCapacity;
		setMemoryManager(MemoryManager.DEFAULT_MEMORY_MANAGER);
		kryo = new KryoReflectionFactorySupport();
		KryoMessage.registerMessages(kryo, compress);
	}

	@Override
	public String getName() {
		return "KryoSerializationEncoder";
	}

	@Override
	public boolean hasInputRemaining(AttributeStorage storage, Object input) {
		return input != null;
	}

	@Override
	protected TransformationResult<Object, Buffer> transformImpl(
			AttributeStorage storage, Object input)
			throws TransformationException {

		if (input == null) {
			throw new TransformationException("Input could not be null");
		}

		try {
			Buffer header = getMemoryManager().allocate(8192);
			header.putInt(0);

			// kryo.writeObject(header.toByteBuffer(), input);

			BufferOutputStream bos = new BufferOutputStream(getMemoryManager(),
					header);
			ObjectBuffer ob = new ObjectBuffer(kryo, initialBufferCapacity,
					maxBufferCapacity);
			ob.writeClassAndObject(bos, input);

			final Buffer output = bos.getBuffer();
			int length = output.position() - 4;

			header.putInt(0, length);

			// System.out.println("SERIALIZED " + input.getClass() + " object: "
			// + input.toString() + " into " + length + " bytes.");

			output.flip();
			output.allowBufferDispose(true);
			header.allowBufferDispose(true);

			return TransformationResult.createCompletedResult(output, null);
		} catch (Exception e) {
			throw new TransformationException("Serialization exception", e);
		}
	}
}
