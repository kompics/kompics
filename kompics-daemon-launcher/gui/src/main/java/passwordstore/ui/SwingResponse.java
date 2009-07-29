package passwordstore.ui;


import java.util.UUID;

import se.sics.kompics.Response;

/**
 * The <code>Timeout</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: Timeout.java 737 2009-03-23 16:43:35Z Cosmin $
 */
public class SwingResponse extends Response implements Cloneable {

	private UUID reqId;


	protected SwingResponse(SwingRequest request) {
		super(request);
		reqId = UUID.randomUUID();
	}

	/**
	 * Gets the timeout id.
	 * 
	 * @return the timeout id
	 */
	public final UUID getTimeoutId() {
		return reqId;
	}
	
	/* (non-Javadoc)
	 * @see se.sics.kompics.Response#clone()
	 */
	@Override
	public final Object clone() {
		SwingResponse response = (SwingResponse) super.clone();
		response.reqId = reqId;
		return response;
	}
}
