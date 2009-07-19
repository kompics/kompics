package se.sics.kompics.wan.ui;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.InstallJobOnHosts;
import se.sics.kompics.wan.master.PrintConnectedDameons;
import se.sics.kompics.wan.master.PrintDaemonsWithLoadedJob;
import se.sics.kompics.wan.master.PrintLoadedJobs;
import se.sics.kompics.wan.master.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.StartJobOnHosts;
import se.sics.kompics.wan.master.StopJobOnHosts;



/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class TextUIPort extends PortType {

	{
		positive(PrintConnectedDameons.class);
		positive(PrintDaemonsWithLoadedJob.class);
		positive(PrintLoadedJobs.class);
		positive(InstallJobOnHosts.class);
		positive(StartJobOnHosts.class);
		positive(StopJobOnHosts.class);
		positive(ShutdownDaemonRequest.class);
	}
}
