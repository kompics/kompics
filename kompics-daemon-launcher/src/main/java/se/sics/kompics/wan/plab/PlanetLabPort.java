package se.sics.kompics.wan.plab;

import se.sics.kompics.PortType;



/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PlanetLabPort extends PortType {

	{
		negative(GetRunningPlanetLabHosts.class);
	}
}
