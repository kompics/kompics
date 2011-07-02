package se.sics.kompics.network.grizzly.kryo;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

public final class KryoSerializationFilter extends
		AbstractCodecFilter<Buffer, Object> {

	public KryoSerializationFilter(boolean compress, int initialBufferCapacity,
			int maxBufferCapacity) {
		super(new KryoSerializationDecoder(compress, initialBufferCapacity,
				maxBufferCapacity), new KryoSerializationEncoder(compress,
				initialBufferCapacity, maxBufferCapacity));
	}
}