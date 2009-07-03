package se.sics.kompics.wan.master.scp;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.master.scp.events.ScpGetFileRequest;
import se.sics.kompics.wan.master.scp.events.ScpGetFileResponse;
import se.sics.kompics.wan.master.scp.events.ScpGetFinished;
import se.sics.kompics.wan.master.scp.events.ScpPutFileRequest;
import se.sics.kompics.wan.master.scp.events.ScpPutFileResponse;
import se.sics.kompics.wan.master.scp.events.ScpPutFinished;



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
