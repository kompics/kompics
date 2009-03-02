package se.sics.kompics.kdld.main;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.main.event.Deploy;
import se.sics.kompics.kdld.main.event.DeployRequest;
import se.sics.kompics.kdld.main.event.DeployResponse;


/**
 * The <code>HelloPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: HelloPort.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class Kdl extends PortType {

	{
		negative(Deploy.class);
		negative(DeployRequest.class);
		negative(DeployResponse.class);
		positive(DeployRequest.class);
	}
}
