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
		negative(ScpGetFileRequest.class);
		negative(ScpPutFileRequest.class);
		
		
		positive(ScpGetFileResponse.class);
		positive(ScpPutFileResponse.class);
		positive(ScpGetFinished.class);
		positive(ScpPutFinished.class);
	}
}
