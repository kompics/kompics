package se.sics.kompics.wan.services.events;


import java.util.Set;
import se.sics.kompics.Response;

public class StartDaemonOnHostsResponse extends Response {

	
    private final Set<String> hosts;

    private final boolean sshAuthenticationSuccess;

	public StartDaemonOnHostsResponse(StartDaemonOnHostsRequest request, Set<String> hosts, boolean sshAuthenticationSuccess) {
		super(request);
                this.hosts = hosts;
                this.sshAuthenticationSuccess = sshAuthenticationSuccess;
	}

    public Set<String> getHosts() {
        return hosts;
    }

    public boolean isSshAuthenticationSuccess() {
        return sshAuthenticationSuccess;
    }
    
}
