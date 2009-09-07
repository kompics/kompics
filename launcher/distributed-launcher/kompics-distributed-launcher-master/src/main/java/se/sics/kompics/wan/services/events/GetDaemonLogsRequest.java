package se.sics.kompics.wan.services.events;


import java.util.Set;

import se.sics.kompics.wan.services.ExperimentServicesComponent;
import se.sics.kompics.wan.ssh.Credentials;

public class GetDaemonLogsRequest extends ActionRequest {

	public GetDaemonLogsRequest(Credentials cred, Set<String> hosts) {
		super(cred, hosts, ExperimentServicesComponent.Action.START_DAEMON);
	}
	
}
