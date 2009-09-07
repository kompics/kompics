package se.sics.kompics.wan.ui;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.events.InstallJobOnHostsRequest;
import se.sics.kompics.wan.master.events.GetConnectedDameonsRequest;
import se.sics.kompics.wan.master.events.GetDaemonsWithLoadedJobRequest;
import se.sics.kompics.wan.master.events.GetLoadedJobsForDaemonRequest;
import se.sics.kompics.wan.master.events.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.events.StartJobOnHostsRequest;
import se.sics.kompics.wan.master.events.StopJobOnHostsRequest;



/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class TextUIPort extends PortType {

	{
		positive(GetConnectedDameonsRequest.class);
		positive(GetDaemonsWithLoadedJobRequest.class);
		positive(GetLoadedJobsForDaemonRequest.class);
		positive(InstallJobOnHostsRequest.class);
		positive(StartJobOnHostsRequest.class);
		positive(StopJobOnHostsRequest.class);
		positive(ShutdownDaemonRequest.class);
	}
}
