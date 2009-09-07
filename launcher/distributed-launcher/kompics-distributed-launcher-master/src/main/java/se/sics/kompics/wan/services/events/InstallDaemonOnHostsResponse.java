package se.sics.kompics.wan.services.events;

import java.util.List;

import se.sics.kompics.Response;
import se.sics.kompics.wan.services.ExperimentServicesComponent;

public class InstallDaemonOnHostsResponse extends Response {

    private final boolean sshAuthenticationSuccess;
    private final List<String> hosts;
    private final List<ExperimentServicesComponent.ServicesStatus> status;

    public InstallDaemonOnHostsResponse(InstallDaemonOnHostsRequest request, List<String> hosts,
            List<ExperimentServicesComponent.ServicesStatus> status, boolean sshAuthenticationSuccess) {
        super(request);
        this.hosts = hosts;
        this.status = status;
        this.sshAuthenticationSuccess = sshAuthenticationSuccess;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public List<ExperimentServicesComponent.ServicesStatus> getStatus() {
        return status;
    }

    public boolean isSshAuthenticationSuccess() {
        return sshAuthenticationSuccess;
    }
}
