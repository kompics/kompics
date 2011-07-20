package se.sics.kompics.network.grizzly.protostuff;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.BufferOutputStream;

import se.sics.kompics.network.Message;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

public class ProtostuffSerializationEncoder extends
		AbstractTransformer<Message, Buffer> {

	private static final ThreadLocal<LinkedBuffer> threadLocalLinkedBuffer = new ThreadLocal<LinkedBuffer>() {
		@Override
		protected LinkedBuffer initialValue() {
			return LinkedBuffer.allocate(8192);
		}
	};

	public ProtostuffSerializationEncoder() {
		setMemoryManager(MemoryManager.DEFAULT_MEMORY_MANAGER);
	}

	@Override
	public String getName() {
		return "ProtostuffSerializationEncoder";
	}

	@Override
	public boolean hasInputRemaining(AttributeStorage storage, Message input) {
		return input != null;
	}

	@Override
	protected TransformationResult<Message, Buffer> transformImpl(
			AttributeStorage storage, Message input)
			throws TransformationException {

		if (input == null) {
			throw new TransformationException("Input could not be null");
		}

		try {
			LinkedBuffer buffer = threadLocalLinkedBuffer.get();
			Schema<Message> schema = RuntimeSchema.getSchema(input.getClass()
					.getName(), true);
			Buffer header = getMemoryManager().allocate(8192);
			byte[] name = input.getClass().getName().getBytes();
			header.putInt(0);
			header.putInt(name.length);
			header.put(name);
			BufferOutputStream bos = new BufferOutputStream(getMemoryManager(),
					header);
			int length = 0;
			try {
				length = ProtostuffIOUtil.writeDelimitedTo(bos, input, schema,
						buffer);
				// int length = ProtobufIOUtil.writeTo(buffer, input, schema);

//				System.out.println("SERIALIZED " + input.getClass()
//						+ " object: " + input.toString() + " into "
//						+ (length + 4 + name.length) + " bytes: 4 + "
//						+ name.length + " + " + length);
				
//				System.out.println("SourceID: " + input.getSource().getId());
//				System.out.println("SourcePORT: " + input.getSource().getPort());
//				System.out.println("SourceNAME: " + new String(input.getSource().getIp().getHostName()));
//				System.out.println("SourceIP: " + new String(input.getSource().getIp().getHostAddress()));

			} finally {
				buffer.clear();
			}

			final Buffer output = bos.getBuffer();

			header.putInt(0, length + 4 + name.length);

			output.flip();
			output.allowBufferDispose(true);
			header.allowBufferDispose(true);

			return TransformationResult.createCompletedResult(output, null);
		} catch (Exception e) {
			throw new TransformationException("Serialization exception", e);
		}
	}
}
