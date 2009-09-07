package se.sics.kompics.wan.hosts;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.hosts.events.AddNodesRequest;
import se.sics.kompics.wan.hosts.events.AddNodesResponse;
import se.sics.kompics.wan.hosts.events.GetNodesRequest;
import se.sics.kompics.wan.hosts.events.GetNodesResponse;
import se.sics.kompics.wan.hosts.events.RemoveNodesRequest;
import se.sics.kompics.wan.hosts.events.RemoveNodesResponse;




/**
 * The <code>MasterCommands</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class HostsPort extends PortType {

	{
		negative(GetNodesRequest.class);
		negative(AddNodesRequest.class);
		negative(RemoveNodesRequest.class);
		
		positive(GetNodesResponse.class);
		positive(AddNodesResponse.class);
		positive(RemoveNodesResponse.class);
	}
}
