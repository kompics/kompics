package se.sics.kompics.p2p.network.topology;

import se.sics.kompics.network.Address;

public class LinkDescriptor {

	private Address source;

	private Address destination;

	private long latency;

	private double lossRate;

	public LinkDescriptor(Address source, Address destination, long latency,
			double lossRate) {
		this.source = source;
		this.destination = destination;
		this.latency = latency;
		this.lossRate = lossRate;
	}

	public long getLatency() {
		return latency;
	}

	public double getLossRate() {
		return lossRate;
	}

	public Address getSource() {
		return source;
	}

	public Address getDestination() {
		return destination;
	}

	@Override
	public String toString() {
		return "Link: SRC=" + source + ", DST= " + destination + " , LATENCY="
				+ latency + ", LOSS_RATE=" + lossRate;
	}
}
