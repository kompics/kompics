package se.sics.kompics.wan.master;

import se.sics.kompics.PortType;



/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class MasterPort extends PortType {

	{
		negative(PrintConnectedDameons.class);
		negative(PrintDaemonsWithLoadedJob.class);
		negative(PrintLoadedJobs.class);
		negative(InstallJobOnHosts.class);
		negative(StartJobOnHosts.class);
		negative(StopJobOnHosts.class);
		negative(ShutdownDaemonRequest.class);
	}
}
