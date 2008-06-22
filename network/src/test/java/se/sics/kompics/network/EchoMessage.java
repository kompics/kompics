package se.sics.kompics.network;

import se.sics.kompics.network.events.NetworkDeliverEvent;

public class EchoMessage extends NetworkDeliverEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4882538292165396356L;

	private final int sequenceNo;

	public EchoMessage(Address source, Address destination, int sequenceNo) {
		super(source, destination);
		this.sequenceNo = sequenceNo;
	}

	public int getSequenceNo() {
		return sequenceNo;
	}

	public String toString() {
		return "Echo" + sequenceNo;
	}
}
