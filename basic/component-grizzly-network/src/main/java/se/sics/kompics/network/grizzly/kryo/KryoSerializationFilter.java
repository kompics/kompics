package se.sics.kompics.network.grizzly.kryo;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

public final class KryoSerializationFilter extends
		AbstractCodecFilter<Buffer, Object> {

	public KryoSerializationFilter() {
		super(new KryoSerializationDecoder(), new KryoSerializationEncoder());
	}
}