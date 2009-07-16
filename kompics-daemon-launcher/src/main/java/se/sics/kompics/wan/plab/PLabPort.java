package se.sics.kompics.wan.plab;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.plab.events.AddHostsToSliceRequest;
import se.sics.kompics.wan.plab.events.AddHostsToSliceResponse;
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
		negative(QueryPLabSitesRequest.class);
		negative(InstallDaemonOnHostsRequest.class);
		negative(UpdateCoMonStats.class);
		negative(GetNodesForSliceRequest.class);
		negative(UpdateHostsAndSites.class);
		negative(GetProgressRequest.class);
		negative(AddHostsToSliceRequest.class);
		
		positive(QueryPLabSitesResponse.class);
		positive(InstallDaemonOnHostsResponse.class);
		positive(GetNodesForSliceResponse.class);
		positive(GetProgressResponse.class);
		positive(AddHostsToSliceResponse.class);
	}
}
