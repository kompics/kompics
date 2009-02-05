package se.sics.kompics.manual.twopc.event;

import java.util.HashMap;
import java.util.Map;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public final class CoordinatorInit extends Init {

	private final int id;
	
	private final Map<Integer,Address> mapParticipants = new HashMap<Integer,Address>();
	
    private final Address self;
	
	public CoordinatorInit(int id, Address self, Map<Integer,Address> mapParticipants) {
		super();
		this.id = id;
		this.self = self;
		if (mapParticipants != null)
		{
			this.mapParticipants.putAll(mapParticipants);
		}
	}

	public int getId() {
		return id;
	}

	public Map<Integer, Address> getMapParticipants() {
		return mapParticipants;
	}
	
	public Address getSelf() {
		return self;
	}
}