package test;

import org.junit.Ignore;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

@Ignore
public class TestMulticastMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3076380548231930776L;

	private byte[] payload;

	public TestMulticastMessage(Address s, Address d, byte[] payload) {
		super(s, d, Transport.MULTICAST_UDP);
		this.payload = payload;
	}

	public byte[] getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "TestMulticastMessage[" + new String(payload) + "]";
	}
}
