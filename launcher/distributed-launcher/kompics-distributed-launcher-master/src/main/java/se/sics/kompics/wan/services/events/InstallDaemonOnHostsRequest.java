package se.sics.kompics.wan.services.events;


import java.util.Set;

import se.sics.kompics.wan.services.ExperimentServicesComponent;
import se.sics.kompics.wan.ssh.Credentials;

public class InstallDaemonOnHostsRequest extends ActionRequest {

	private final boolean force;
	
	public InstallDaemonOnHostsRequest(Credentials cred, Set<String> hosts, boolean force) {
		super(cred, hosts,  ExperimentServicesComponent.Action.INSTALL_DAEMON);
		this.force = force;
	}
	
	public boolean isForce() {
		return force;
	}
}
