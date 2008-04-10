/**
 * 
 */
package se.sics.kompics.api.capability;

public class CapabilityNotSupportedException extends Exception {

	private static final long serialVersionUID = -8339558110826831632L;

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
}
