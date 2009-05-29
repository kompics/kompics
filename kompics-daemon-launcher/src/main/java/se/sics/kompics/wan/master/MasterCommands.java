package se.sics.kompics.wan.master;

import se.sics.kompics.PortType;


/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class MasterCommands extends PortType {

	{
		positive(PrintConnectedDameons.class);
		positive(PrintDaemonsWithLoadedJob.class);
		positive(PrintLoadedJobs.class);
		positive(InstallJobOnHosts.class);
		positive(StartJobOnHosts.class);
	}
}
