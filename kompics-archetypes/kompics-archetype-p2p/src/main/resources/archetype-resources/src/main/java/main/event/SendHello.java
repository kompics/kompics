package ${package}.main.event;

import se.sics.kompics.Event;

public class SendHello extends Event {

	private final int id;
	
	public SendHello(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}
