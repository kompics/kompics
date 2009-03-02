
package se.sics.kompics.kdld.main.event;

import se.sics.kompics.Event;

/**
 * The <code>Hello</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Hello.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class Deploy extends Event {

	private static final long serialVersionUID = 1710717688555956452L;
	
	private final String pomUri;
	
	public Deploy(String pomUri) {
		this.pomUri = pomUri;
	}
	
	public String getPomUri() {
		return pomUri;
	}
}