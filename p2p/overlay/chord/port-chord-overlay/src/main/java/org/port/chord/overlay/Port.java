package org.port.chord.overlay;

import se.sics.kompics.PortType;

public final class Port extends PortType {
	{
		positive(EventOut.class);
		negative(EventIn.class);
	}
}
