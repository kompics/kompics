package se.sics.kompics;

public interface Channel<P extends PortType> {

	public void hold();
	
	public void resume();
	
	public void unplug();

	public void plug();
	
	public P getPortType();
}
