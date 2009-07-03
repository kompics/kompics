package se.sics.kompics.wan.master.plab;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.plab.events.GetRunningPlanetLabHostsRequest;
import se.sics.kompics.wan.master.plab.events.GetRunningPlanetLabHostsResponse;



/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PlanetLabPort extends PortType {

	{
		negative(GetRunningPlanetLabHostsRequest.class);
		
		
		positive(GetRunningPlanetLabHostsResponse.class);
	}
}
