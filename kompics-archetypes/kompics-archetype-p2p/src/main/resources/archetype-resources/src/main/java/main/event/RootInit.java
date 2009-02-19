package ${package}.main.event;

import java.util.HashMap;
import java.util.Map;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public final class RootInit extends Init {

	private final int id;
	
	private final Map<Integer,Address> neighbours = new HashMap<Integer,Address>();
	
    private final Address self;
	
	public RootInit(int id, Address self, Map<Integer,Address> neighbours) {
		super();
		this.id = id;
		this.self = self;
		if (neighbours != null)
		{
			this.neighbours.putAll(neighbours);
		}
	}

	public int getId() {
		return id;
	}

	public Map<Integer, Address> getNeighbours() {
		return neighbours;
	}
	
	public Address getSelf() {
		return self;
	}
}