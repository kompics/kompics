package passwordstore.ui;


import se.sics.kompics.Request;

/**
 * The <code>ScheduleTimeout</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: ScheduleTimeout.java 481 2009-01-28 01:10:41Z cosmin $
 */
public final class SwingRequest extends Request {


	private SwingResponse response;

	/**
	 * Instantiates a new schedule timeout.
	 * 
	 * @param delay
	 *            the delay
	 */
	public SwingRequest(SwingResponse response) {
		this.response = response;
	}

	/**
	 * Sets the timeout event.
	 * 
	 * @param timeout
	 *            the new timeout event
	 */
	public final void setResponse(SwingResponse timeout) {
		this.response = timeout;
	}

	/**
	 * Gets the timeout event.
	 * 
	 * @return the timeout event
	 */
	public final SwingResponse getResponseEvent() {
		return response;
	}
}
