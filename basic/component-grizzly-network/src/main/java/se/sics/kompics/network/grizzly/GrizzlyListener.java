package se.sics.kompics.network.grizzly;

import se.sics.kompics.network.Message;

public abstract class GrizzlyListener {

	public static GrizzlyListener singleton;
	
	public static void delivered(Message m) {
		if (singleton != null) {
			singleton.messageDelivered(m);
		}
	}
	
	public abstract void messageDelivered(Message m);
}
