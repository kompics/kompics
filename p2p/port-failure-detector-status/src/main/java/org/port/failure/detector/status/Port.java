package org.port.failure.detector.status;

import se.sics.kompics.PortType;

public final class Port extends PortType {
	{
		positive(EventOut.class);
		negative(EventIn.class);
	}
}
