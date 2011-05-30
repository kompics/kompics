package se.sics.kompics.network.grizzly.serialization;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

public final class JavaSerializationFilter extends
		AbstractCodecFilter<Buffer, Object> {

	public JavaSerializationFilter() {
		super(new JavaSerializationDecoder(), new JavaSerializationEncoder());
	}
}