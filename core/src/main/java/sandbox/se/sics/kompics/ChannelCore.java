package sandbox.se.sics.kompics;

public class ChannelCore<P extends PortType> implements Channel<P> {

	public void hold() {
		// TODO Auto-generated method stub

	}

	public void plug() {
		// TODO Auto-generated method stub

	}

	public void resume() {
		// TODO Auto-generated method stub

	}

	public void unplug() {
		// TODO Auto-generated method stub

	}

	public P getPortType() {
		return portType;
	}

	/* === PRIVATE === */

	private PortCore<P> positivePort, negativePort;

	private P portType;

	ChannelCore(PortCore<P> positivePort, PortCore<P> negativePort, P portType) {
		this.positivePort = positivePort;
		this.negativePort = negativePort;
		this.portType = portType;
	}

	PortCore<P> getPositivePort() {
		return positivePort;
	}

	PortCore<P> getNegativePort() {
		return negativePort;
	}

	void forwardToPositive(Event event, int wid) {
		event.forwardedBy(this);
		positivePort.doTrigger(event, wid);
	}

	void forwardToNegative(Event event, int wid) {
		event.forwardedBy(this);
		negativePort.doTrigger(event, wid);
	}
}
