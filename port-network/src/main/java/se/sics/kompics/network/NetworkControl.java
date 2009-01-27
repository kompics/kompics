package se.sics.kompics.network;

import se.sics.kompics.PortType;

public final class NetworkControl extends PortType {
	{
		positive(NetworkSessionOpened.class);
		positive(NetworkSessionClosed.class);
		positive(NetworkException.class);
		positive(NetworkConnectionRefused.class);
	}
}
