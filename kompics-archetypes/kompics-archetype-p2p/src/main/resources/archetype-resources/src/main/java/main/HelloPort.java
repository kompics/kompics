package ${package}.main;

import se.sics.kompics.PortType;
import ${package}.main.event.Hello;
import ${package}.main.event.Hello;
import ${package}.main.event.SendHello;


/**
 * The <code>HelloPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class HelloPort extends PortType {

	{
		negative(Hello.class);
		negative(SendHello.class);
		positive(Hello.class);
	}
}
