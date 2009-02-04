package se.sics.kompics.manual.twopc.event;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public final class ParticipantInit extends Init {

    private final Address self;
	
	public ParticipantInit(Address self) {
		super();
		this.self = self;
	}

	public Address getSelf() {
		return self;
	}

}