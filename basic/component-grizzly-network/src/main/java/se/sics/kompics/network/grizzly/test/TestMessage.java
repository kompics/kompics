package se.sics.kompics.network.grizzly.test;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

public class TestMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -352575198220285795L;

	private byte[] payload;

	public TestMessage(Address s, Address d, byte[] payload) {
		super(s, d, Transport.TCP);
		this.payload = payload;
	}

	public byte[] getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "TestMessage[" + new String(payload) + "]";
	}
}
