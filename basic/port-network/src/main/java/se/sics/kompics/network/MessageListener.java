package se.sics.kompics.network;


public abstract class MessageListener {

	public static MessageListener singleton;
	
	public static void delivered(Message m) {
		if (singleton != null) {
			singleton.messageDelivered(m);
		}
	}
	
	public abstract void messageDelivered(Message m);
}
