package test;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

public class TestMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3076380548231930776L;

	private byte[] payload; 
	
	public TestMessage(Address s, Address d, byte[] payload) {
		super(s, d, Transport.UDP);
		this.payload = payload;
	}
	
	public byte[] getPayload() {
		return payload;
	}
}
