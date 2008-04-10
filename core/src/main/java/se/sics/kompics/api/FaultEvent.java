package se.sics.kompics.api;

public class FaultEvent implements Event {

	private Throwable throwable;

	public FaultEvent(Throwable throwable) {
		super();
		this.throwable = throwable;
	}

	public Throwable getThrowable() {
		return throwable;
	}
}
