package sandbox.se.sics.kompics;

public class ControlPort extends PortType {
	{
		positive(Fault.class);
		negative(Stop.class);
		negative(Start.class);
		negative(Init.class);
	}
}
