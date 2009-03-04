package se.sics.kompics.kdld;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.main.event.MavenCommandRequest;
import se.sics.kompics.kdld.main.event.MavenCommandResponse;


/**
 * The <code>Maven</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class Maven extends PortType {

	{
		negative(MavenCommandRequest.class);
		positive(MavenCommandResponse.class);
	}
}
