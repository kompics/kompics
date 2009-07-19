package se.sics.kompics.wan.plab;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.plab.events.DnsResolverRequest;
import se.sics.kompics.wan.plab.events.DnsResolverResponse;
import se.sics.kompics.wan.plab.events.GetProgressRequest;
import se.sics.kompics.wan.plab.events.GetProgressResponse;



/**
 * The <code>DnsResolverPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DnsResolverPort extends PortType {

	{
		negative(GetProgressRequest.class);
		negative(DnsResolverRequest.class);
	
		positive(GetProgressResponse.class);
		positive(DnsResolverResponse.class);
	}
}
