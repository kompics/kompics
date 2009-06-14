package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.PortType;



/**
 * The <code>SshPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshPort extends PortType {

	{
		negative(SshConnectRequest.class);
		negative(SshCommand.class);
		
		positive(SshConnectResponse.class);
	}
}
