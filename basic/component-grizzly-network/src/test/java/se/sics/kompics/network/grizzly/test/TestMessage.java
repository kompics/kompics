package se.sics.kompics.network.grizzly.test;

import org.junit.Ignore;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

@Ignore
public class TestMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7243255539664143158L;
	
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
