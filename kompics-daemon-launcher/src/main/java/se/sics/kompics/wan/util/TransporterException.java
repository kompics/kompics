package se.sics.kompics.wan.util;

public class TransporterException extends Exception {

	private static final long serialVersionUID = 3470800466330216041L;

	public TransporterException(String msg) {
		super(msg);
	}

	public TransporterException(String msg, Throwable t) {
		super(msg, t);
	}

	public TransporterException(Throwable t) {
		super(t);
	}

}
