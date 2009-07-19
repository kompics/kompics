package se.sics.kompics.wan.ssh;

import se.sics.kompics.PortType;
import se.sics.kompics.wan.ssh.events.CommandRequest;
import se.sics.kompics.wan.ssh.events.CommandResponse;
import se.sics.kompics.wan.ssh.events.DownloadFileRequest;
import se.sics.kompics.wan.ssh.events.DownloadFileResponse;
import se.sics.kompics.wan.ssh.events.HaltRequest;
import se.sics.kompics.wan.ssh.events.HaltResponse;
import se.sics.kompics.wan.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.ssh.events.SshConnectResponse;
import se.sics.kompics.wan.ssh.events.SshHeartbeatRequest;
import se.sics.kompics.wan.ssh.events.SshHeartbeatResponse;
import se.sics.kompics.wan.ssh.events.UploadFileRequest;
import se.sics.kompics.wan.ssh.events.UploadFileResponse;



/**
 * The <code>SshPort</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshPort extends PortType {

	{
		negative(SshConnectRequest.class);
		negative(CommandRequest.class);
		negative(HaltRequest.class);		
		negative(DownloadFileRequest.class);
		negative(UploadFileRequest.class);
		negative(SshHeartbeatRequest.class);
		
		
		positive(SshConnectResponse.class);
		positive(CommandResponse.class);
		positive(HaltResponse.class);
		positive(DownloadFileResponse.class);
		positive(UploadFileResponse.class);
		positive(SshHeartbeatResponse.class);
	}
}
