package se.sics.kompics.network.grizzly.protostuff;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.utils.BufferInputStream;

import se.sics.kompics.network.Message;

import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

public class ProtostuffSerializationDecoder extends
		AbstractTransformer<Buffer, Message> {

	protected final Attribute<Integer> lengthAttribute = attributeBuilder
			.createAttribute("ProtostuffSerializationDecoder.SerializedSize");

	public ProtostuffSerializationDecoder() {
	}

	@Override
	public String getName() {
		return "ProtostuffSerializationDecoder";
	}

	@Override
	public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
		return input != null && input.hasRemaining();
	}

	@Override
	protected TransformationResult<Buffer, Message> transformImpl(
			AttributeStorage storage, Buffer input)
			throws TransformationException {

		Integer serialLength = lengthAttribute.get(storage);

		if (serialLength == null) {
			if (input.remaining() < 4) {
				return TransformationResult.createIncompletedResult(input);
			}

			serialLength = input.getInt();
			lengthAttribute.set(storage, serialLength);
		}

		if (input.remaining() < serialLength) {
			return TransformationResult.createIncompletedResult(input);
		}
		try {
			int nameLength = input.getInt();
			byte[] name = new byte[nameLength];
			input.get(name);
			String typeName = new String(name);
			Schema<Message> schema = RuntimeSchema.getSchema(typeName, true);
			Message output = schema.newMessage();

//			input.mark();

			BufferInputStream bis = new BufferInputStream(input);
			ProtostuffIOUtil.mergeDelimitedFrom(bis, output, schema);
			// ProtobufIOUtil.mergeDelimitedFrom(bis, output, schema);

//			System.out.println("DESERIALIZED " + output.getClass()
//					+ " object: " + output.toString() + " from " + serialLength
//					+ " bytes.");

//			byte[] all = new byte[serialLength - 4 - nameLength];
//			input.reset();
//			input.get(all);
//			System.out.println("ALL=" + all.length);
//			System.out.println(new String(all));
//			System.out.println("SourceID: " + output.getSource().getId());
//			System.out.println("SourcePORT: " + output.getSource().getPort());
//			System.out.println("SourceNAME: " + new String(output.getSource().getIp().getHostName()));
//			System.out.println("SourceIP: " + new String(output.getSource().getIp().getHostAddress()));

			return TransformationResult.createCompletedResult((Message) output,
					input);
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
