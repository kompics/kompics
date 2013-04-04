package se.sics.kompics.network.grizzly.test;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Transport;

public class TestMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -352575198220285795L;

	private final byte[] payload;
        private final byte[] padding;
        private final int seq;

        public TestMessage(Address s, Address d, byte[] payload) {
            this(s, d, payload, new byte[]{});
        }
        
        public TestMessage(Address s, Address d, byte[] payload, byte[] padding) {
            this(s, d, payload, padding, 0);
        }
        
	public TestMessage(Address s, Address d, byte[] payload, byte[] padding, int seq) {
		super(s, d, Transport.TCP);
		this.payload = payload;
                this.padding = padding;
                this.seq = seq;
	}

	public byte[] getPayload() {
            return payload;
	}
        
        public int getSeq() {
            return this.seq;
        }

	@Override
	public String toString() {
		return "TestMessage[" + new String(payload) + "]+" + padding.length + "B";
	}
        
}
