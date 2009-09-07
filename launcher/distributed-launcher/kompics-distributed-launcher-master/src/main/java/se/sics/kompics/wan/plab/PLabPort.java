package se.sics.kompics.wan.plab;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.plab.events.AddHostsToSliceRequest;
import se.sics.kompics.wan.plab.events.AddHostsToSliceResponse;
import se.sics.kompics.wan.plab.events.GetHostsInSliceRequest;
import se.sics.kompics.wan.plab.events.GetHostsInSliceResponse;
import se.sics.kompics.wan.plab.events.GetHostsNotInSliceRequest;
import se.sics.kompics.wan.plab.events.GetHostsNotInSliceResponse;
import se.sics.kompics.wan.plab.events.GetNodesWithCoMonStatsRequest;
import se.sics.kompics.wan.plab.events.GetNodesWithCoMonStatsResponse;
import se.sics.kompics.wan.plab.events.GetPLabNodesRequest;
import se.sics.kompics.wan.plab.events.GetPLabNodesResponse;
import se.sics.kompics.wan.plab.events.GetProgressRequest;
import se.sics.kompics.wan.plab.events.GetProgressResponse;
import se.sics.kompics.wan.plab.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.plab.events.QueryPLabSitesResponse;
import se.sics.kompics.wan.plab.events.RankHostsUsingCoMonRequest;
import se.sics.kompics.wan.plab.events.RankHostsUsingCoMonResponse;
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
		negative(UpdateCoMonStats.class);
		negative(GetHostsInSliceRequest.class);
		negative(GetHostsNotInSliceRequest.class);
		negative(UpdateHostsAndSites.class);
		negative(GetProgressRequest.class);
		negative(AddHostsToSliceRequest.class);
		negative(RankHostsUsingCoMonRequest.class);
		negative(GetNodesWithCoMonStatsRequest.class);
		negative(GetPLabNodesRequest.class);
		
		positive(QueryPLabSitesResponse.class);
		positive(GetHostsInSliceResponse.class);
		positive(GetHostsNotInSliceResponse.class);
		positive(GetProgressResponse.class);
		positive(AddHostsToSliceResponse.class);
		positive(RankHostsUsingCoMonResponse.class);
		positive(GetNodesWithCoMonStatsResponse.class);
		positive(GetPLabNodesResponse.class);
	}
}
