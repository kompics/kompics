package se.sics.kompics.wan.plab;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.plab.events.GetAllHostsRequest;
import se.sics.kompics.wan.plab.events.GetAllHostsResponse;
import se.sics.kompics.wan.plab.events.GetNodesForSliceRequest;
import se.sics.kompics.wan.plab.events.GetNodesForSliceResponse;
import se.sics.kompics.wan.plab.events.GetProgressRequest;
import se.sics.kompics.wan.plab.events.GetProgressResponse;
import se.sics.kompics.wan.plab.events.InstallDaemonOnHostsRequest;
import se.sics.kompics.wan.plab.events.InstallDaemonOnHostsResponse;
import se.sics.kompics.wan.plab.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.plab.events.QueryPLabSitesResponse;
import se.sics.kompics.wan.plab.events.UpdateCoMonStats;
import se.sics.kompics.wan.plab.events.UpdateHostsAndSites;



/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PLabPort extends PortType {

	{
		negative(GetAllHostsRequest.class);
		negative(QueryPLabSitesRequest.class);
		negative(InstallDaemonOnHostsRequest.class);
		negative(UpdateCoMonStats.class);
		negative(GetNodesForSliceRequest.class);
		negative(UpdateHostsAndSites.class);
		negative(GetProgressRequest.class);
		
		positive(GetAllHostsResponse.class);
		positive(QueryPLabSitesResponse.class);
		positive(InstallDaemonOnHostsResponse.class);
		positive(GetNodesForSliceResponse.class);
		negative(GetProgressResponse.class);
	}
}
