package se.sics.kompics.manual.twopc.event;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public final class ParticipantInit extends Init {

	private final int id;
    private final Address self;
	private final Address coordinatorAddress;
	
	
	public ParticipantInit(int id, Address self, Address coordinatorAddress) {
		super();
		this.id = id;
		this.self = self;
		this.coordinatorAddress = coordinatorAddress;
	}

	public int getId() {
		return id;
	}
	
	public Address getSelf() {
		return self;
	}

	public Address getCoordinatorAddress() {
		return coordinatorAddress;
	}
}