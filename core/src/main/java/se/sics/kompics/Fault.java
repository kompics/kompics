package se.sics.kompics;

public final class Fault extends Event {

	private final Throwable fault;

	public Fault(Throwable throwable) {
		this.fault = throwable;
	}
	
	public final Throwable getFault() {
		return fault;
	}
}
