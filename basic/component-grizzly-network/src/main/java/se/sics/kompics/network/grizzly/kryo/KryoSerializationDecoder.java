package se.sics.kompics.network.grizzly.kryo;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.utils.BufferInputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.ObjectBuffer;

import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;

public class KryoSerializationDecoder extends
		AbstractTransformer<Buffer, Object> {

	protected final Attribute<Integer> lengthAttribute = attributeBuilder
			.createAttribute("KryoSerializationDecoder.SerializedSize");;

	private Kryo kryo;
	private int initialBufferCapacity, maxBufferCapacity;

	public KryoSerializationDecoder(boolean compress,
			int initialBufferCapacity, int maxBufferCapacity) {
		this.initialBufferCapacity = initialBufferCapacity;
		this.maxBufferCapacity = maxBufferCapacity;
		kryo = new KryoReflectionFactorySupport();
		KryoMessage.registerMessages(kryo, compress);
	}

	@Override
	public String getName() {
		return "KryoSerializationDecoder";
	}

	@Override
	public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
		return input != null && input.hasRemaining();
	}

	@Override
	protected TransformationResult<Buffer, Object> transformImpl(
			AttributeStorage storage, Buffer input)
			throws TransformationException {

		Integer serialLength = lengthAttribute.get(storage);

		if (serialLength == null) {
			if (input.remaining() < 4) {
				return TransformationResult.createIncompletedResult(input);
			}

			serialLength = (int) input.getInt();
			lengthAttribute.set(storage, serialLength);
		}

		if (input.remaining() < serialLength) {
			return TransformationResult.createIncompletedResult(input);
		}

		try {
			BufferInputStream bis = new BufferInputStream(input);
			ObjectBuffer ob = new ObjectBuffer(kryo, initialBufferCapacity,
					maxBufferCapacity);
			Object output = ob.readClassAndObject(bis, serialLength);

			// System.out.println("DESERIALIZED " + output.getClass()
			// + " object: " + output.toString() + " from " + serialLength
			// + " bytes.");

			return TransformationResult.createCompletedResult(output, input);
		} catch (Exception e) {
			throw new TransformationException("Deserialization exception", e);
		}
	}

	@Override
	public void release(AttributeStorage storage) {
		lengthAttribute.remove(storage);
		super.release(storage);
	}
}
