
package ${package}.main.event;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * The <code>Hello</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class Hello extends Message {

	public Hello(Address source, Address dest) {
		super(source, dest);
	}
	
}