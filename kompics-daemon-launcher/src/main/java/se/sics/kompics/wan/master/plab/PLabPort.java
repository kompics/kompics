package se.sics.kompics.wan.master.plab;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.plab.plc.events.GetAllHostsRequest;
import se.sics.kompics.wan.master.plab.plc.events.GetAllHostsResponse;
import se.sics.kompics.wan.master.plab.plc.events.QueryPLabSitesRequest;
import se.sics.kompics.wan.master.plab.plc.events.QueryPLabSitesResponse;



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
		
		positive(GetAllHostsResponse.class);
		positive(QueryPLabSitesResponse.class);
	}
}
