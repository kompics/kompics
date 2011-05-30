package se.sics.kompics.network.grizzly.serialization;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.BufferOutputStream;

public class JavaSerializationEncoder extends
		AbstractTransformer<Object, Buffer> {

	public JavaSerializationEncoder() {
		setMemoryManager(MemoryManager.DEFAULT_MEMORY_MANAGER);
	}

	@Override
	public String getName() {
		return "JavaSerializationEncoder";
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
			BufferOutputStream bos = new BufferOutputStream(getMemoryManager(), header);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(input);
			oos.flush();
			oos.close();

			final Buffer output = bos.getBuffer();
			int length = output.position() - 4;

			header.putInt(0, length);

			output.flip();
			output.allowBufferDispose(true);
			header.allowBufferDispose(true);

			return TransformationResult.createCompletedResult(output, null);
		} catch (IOException e) {
			throw new TransformationException("Serialization exception", e);
		}
	}
}
