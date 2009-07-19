package se.sics.kompics.wan.ssh.scp;

public class ScpRetryExceeded extends Exception {

	private static final long serialVersionUID = -6220541167007539786L;

	private final String command;
	public ScpRetryExceeded(String command, String msg) {
		super(msg);
		this.command = command;
	}
	
	@Override
	public String toString() {
		return command + ": " + super.toString();
	}

	@Override
	public String getMessage() {
		return command + ": " + super.getMessage();
	}
}
