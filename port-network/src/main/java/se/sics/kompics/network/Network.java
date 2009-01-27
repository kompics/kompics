package se.sics.kompics.network;

import se.sics.kompics.PortType;

public final class Network extends PortType {
	{
		positive(Message.class);
		negative(Message.class);
	}
}
