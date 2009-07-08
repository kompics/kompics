package se.sics.kompics.wan.master.plab.plc;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.plab.plc.events.GetAllHostsRequest;
import se.sics.kompics.wan.master.plab.plc.events.GetAllHostsResponse;
import se.sics.kompics.wan.master.plab.plc.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.master.plab.plc.events.InstallDaemonOnHostsResponse;
import se.sics.kompics.wan.master.plab.plc.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.master.plab.plc.events.QueryPLabSitesResponse;



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
