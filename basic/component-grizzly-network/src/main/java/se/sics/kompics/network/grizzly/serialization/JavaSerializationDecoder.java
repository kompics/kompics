package se.sics.kompics.network.grizzly.serialization;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.utils.BufferInputStream;

public class JavaSerializationDecoder extends
		AbstractTransformer<Buffer, Object> {

	protected final Attribute<Integer> lengthAttribute = attributeBuilder
			.createAttribute("JavaSerializationDecoder.SerializedSize");;

	@Override
	public String getName() {
		return "JavaSerializationDecoder";
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
//			int tmpLimit = input.limit();
//			input.limit(input.position() + serialLength);

			BufferInputStream bis = new BufferInputStream(input);
			ObjectInputStream ois = new ObjectInputStream(bis);
			Object output = ois.readObject();

//			input.position(input.limit());
//			input.limit(tmpLimit);

			return TransformationResult.createCompletedResult(output, input);
		} catch (IOException e) {
			throw new TransformationException("Deserialization exception", e);
		} catch (ClassNotFoundException e) {
			throw new TransformationException("Deserialization exception", e);
		}
	}

	@Override
	public void release(AttributeStorage storage) {
		lengthAttribute.remove(storage);
		super.release(storage);
	}
}
