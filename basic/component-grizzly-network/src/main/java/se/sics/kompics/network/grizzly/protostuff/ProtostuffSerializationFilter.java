package se.sics.kompics.network.grizzly.protostuff;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

import se.sics.kompics.network.Message;

public final class ProtostuffSerializationFilter extends
		AbstractCodecFilter<Buffer, Message> {

	static {
//		System.setProperty(
//				"protostuff.runtime.collection_schema_on_repeated_fields",
//				"true");
		System.setProperty(
				"protostuff.runtime.morph_non_final_pojos",
				"true");
	}

	public ProtostuffSerializationFilter() {
		super(new ProtostuffSerializationDecoder(),
				new ProtostuffSerializationEncoder());
	}
}