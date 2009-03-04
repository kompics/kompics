package se.sics.kompics.kdld.main.event;

import java.util.List;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public final class DaemonInit extends Init {

	private final int id;
	
	private final List<Address> neighbours;
	
    private final Address self;
	
	public DaemonInit(int id, Address self, List<Address> neighbours) {
		super();
		this.id = id;
		this.self = self;
		if (neighbours != null)
		{
			this.neighbours = neighbours;
		}
		else
		{
			throw new IllegalArgumentException("List of neighbours was null");
		}
	}

	public int getId() {
		return id;
	}

	public List<Address> getNeighbours() {
		return neighbours;
	}
	
	public Address getSelf() {
		return self;
	}
}