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
		negative(SshCommandRequest.class);
		negative(HaltRequest.class);		
		negative(DownloadFileRequest.class);
		negative(UploadFileRequest.class);
		
		positive(SshConnectResponse.class);
		positive(SshCommandResponse.class);
		positive(HaltResponse.class);
		positive(DownloadFileResponse.class);
		positive(UploadFileResponse.class);

	}
}
