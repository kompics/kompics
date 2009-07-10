package se.sics.kompics.wan.master.plab.plc;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.plab.events.GetAllHostsRequest;
import se.sics.kompics.wan.plab.events.GetAllHostsResponse;
import se.sics.kompics.wan.plab.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.plab.events.InstallDaemonOnHostsResponse;
import se.sics.kompics.wan.plab.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.plab.events.QueryPLabSitesResponse;



/**
 * The <code>SshPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PLControllerPort extends PortType {

	{
		negative(GetAllHostsRequest.class);
		negative(QueryPLabSitesRequest.class);
		negative(InstallDaemonOnHostsRequest.class);
		
		positive(GetAllHostsResponse.class);
		positive(QueryPLabSitesResponse.class);
		positive(InstallDaemonOnHostsResponse.class);

	}
}
