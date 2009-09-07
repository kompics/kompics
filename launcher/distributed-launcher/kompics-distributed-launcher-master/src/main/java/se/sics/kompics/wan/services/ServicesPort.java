package se.sics.kompics.wan.services;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.services.events.ActionProgressResponse;
import se.sics.kompics.wan.services.events.ActionSshTimeout;
import se.sics.kompics.wan.services.events.GetDaemonLogsRequest;
import se.sics.kompics.wan.services.events.GetDaemonLogsResponse;
import se.sics.kompics.wan.services.events.GetStatusRequest;
import se.sics.kompics.wan.services.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.services.events.InstallDaemonOnHostsResponse;
import se.sics.kompics.wan.services.events.InstallJavaOnHostsRequest;
import se.sics.kompics.wan.services.events.InstallJavaOnHostsResponse;
import se.sics.kompics.wan.services.events.StartDaemonOnHostsRequest;
import se.sics.kompics.wan.services.events.StartDaemonOnHostsResponse;
import se.sics.kompics.wan.services.events.StopDaemonOnHostsRequest;
import se.sics.kompics.wan.services.events.StopDaemonOnHostsResponse;




/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ServicesPort extends PortType {

	{
		negative(InstallDaemonOnHostsRequest.class);
		negative(InstallJavaOnHostsRequest.class);		
		negative(StartDaemonOnHostsRequest.class);
                negative(StopDaemonOnHostsRequest.class);
		negative(GetStatusRequest.class);
                negative(GetDaemonLogsRequest.class);
		
		positive(InstallDaemonOnHostsResponse.class);
                positive(InstallJavaOnHostsResponse.class);
		positive(ActionProgressResponse.class);
		positive(StartDaemonOnHostsResponse.class);
                positive(StopDaemonOnHostsResponse.class);
                positive(GetDaemonLogsResponse.class);
                    
                positive(ActionSshTimeout.class);
	}
}
