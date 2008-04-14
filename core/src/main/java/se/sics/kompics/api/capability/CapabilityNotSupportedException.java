/**
 * 
 */
package se.sics.kompics.api.capability;

public class CapabilityNotSupportedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -625610361107775942L;

	public CapabilityNotSupportedException() {
		super();
	}

	public CapabilityNotSupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public CapabilityNotSupportedException(String message) {
		super(message);
	}

	public CapabilityNotSupportedException(Throwable cause) {
		super(cause);
	}

	public CapabilityNotSupportedException(ComponentCapabilityFlags flag) {
		super(flag.toString());
	}

	public CapabilityNotSupportedException(ChannelCapabilityFlags flag) {
		super(flag.toString());
	}
}
