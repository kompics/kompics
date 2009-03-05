
package se.sics.kompics.kdld.main.event;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * MAVEN_OPTS=-Xmx1024m
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Hello.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class SetupEnvRequest extends Message {

	private static final long serialVersionUID = 1986455956452L;
	
	private final String variable;
	private final String value;
	
	public SetupEnvRequest(String variable, String value, Address src, Address dest) {
		super(src, dest);
		this.variable = variable;
		this.value = value;
	}

	public String getVariable() {
		return variable;
	}
	
	public String getValue() {
		return value;
	}
}