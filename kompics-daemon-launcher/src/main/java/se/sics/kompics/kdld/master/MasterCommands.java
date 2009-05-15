package se.sics.kompics.kdld.master;

import se.sics.kompics.PortType;


/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class MasterCommands extends PortType {

	{
		negative(PrintConnectedDameons.class);
		negative(PrintDaemonsWithLoadedJob.class);
		negative(PrintLoadedJobs.class);

	}
}
