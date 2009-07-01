package se.sics.kompics.wan.master.scp;

import se.sics.kompics.PortType;



/**
 * The <code>SshPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ScpPort extends PortType {

	{
		negative(ScpCopyFileRequest.class);
		negative(ScpCopyFileResponse.class);
		
		positive(ScpCopyFinished.class);
	}
}
