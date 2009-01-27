package se.sics.kompics;

public interface Component {

	public <P extends PortType> Positive<P> getPositive(Class<P> portType);

	public <P extends PortType> Negative<P> getNegative(Class<P> portType);

	public Positive<ControlPort> getControl();
}
