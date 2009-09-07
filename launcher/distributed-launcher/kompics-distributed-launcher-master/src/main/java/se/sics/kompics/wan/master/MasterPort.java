package se.sics.kompics.wan.master;

import se.sics.kompics.wan.master.events.ShutdownDaemonRequest;
import se.sics.kompics.wan.master.events.GetDaemonsWithLoadedJobRequest;
import se.sics.kompics.wan.master.events.GetConnectedDaemonsResponse;
import se.sics.kompics.wan.master.events.GetDaemonsWithLoadedJobResponse;
import se.sics.kompics.wan.master.events.GetConnectedDameonsRequest;
import se.sics.kompics.wan.master.events.StartJobOnHostsRequest;
import se.sics.kompics.wan.master.events.GetLoadedJobsForDaemonRequest;
import se.sics.kompics.PortType;
import se.sics.kompics.wan.job.JobExecRequest;
import se.sics.kompics.wan.job.JobExecResponse;
import se.sics.kompics.wan.job.JobStopRequest;
import se.sics.kompics.wan.job.JobStopResponse;
import se.sics.kompics.wan.master.events.ConnectedDaemonNotification;
import se.sics.kompics.wan.master.events.GetLoadedJobsForDaemonResponse;
import se.sics.kompics.wan.master.events.GetLoadedJobsRequest;
import se.sics.kompics.wan.master.events.GetLoadedJobsResponse;
import se.sics.kompics.wan.master.events.InstallJobOnHostsRequest;
import se.sics.kompics.wan.master.events.InstallJobOnHostsResponse;
import se.sics.kompics.wan.master.events.JobsFound;

/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class MasterPort extends PortType {

    {
        negative(GetConnectedDameonsRequest.class);
        negative(GetDaemonsWithLoadedJobRequest.class);
        negative(GetLoadedJobsForDaemonRequest.class);
        negative(GetLoadedJobsRequest.class);
        negative(InstallJobOnHostsRequest.class);
        negative(StartJobOnHostsRequest.class);
        negative(JobStopRequest.class);
        negative(ShutdownDaemonRequest.class);
        negative(JobExecRequest.class);

        positive(GetDaemonsWithLoadedJobResponse.class);
        positive(GetConnectedDaemonsResponse.class);
        positive(GetLoadedJobsForDaemonResponse.class);
        positive(GetLoadedJobsResponse.class);
        positive(JobsFound.class);
        positive(InstallJobOnHostsResponse.class);
        positive(JobExecResponse.class);
        positive(JobStopResponse.class);
        positive(ConnectedDaemonNotification.class);
    }
}
