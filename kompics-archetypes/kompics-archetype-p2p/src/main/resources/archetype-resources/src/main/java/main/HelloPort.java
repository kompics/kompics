package ${package}.main;

import se.sics.kompics.PortType;
import ${package}.main.event.Hello;

/**
 * The <code>HelloPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: HelloPort.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class HelloPort extends PortType {

	{
		negative(Hello.class);
	}
}
