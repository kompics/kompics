package se.sics.kompics.wan.services.events;

import java.util.LinkedHashSet;
import java.util.Set;
import se.sics.kompics.Response;

public class GetDaemonLogsResponse extends Response {

    private final LinkedHashSet<String> hosts;
    private final boolean sshAuthenticationSuccess;
    private final LinkedHashSet<String> logs;

    public GetDaemonLogsResponse(GetDaemonLogsRequest request, Set<String> hosts,
            Set<String> logs,
            boolean sshAuthenticationSuccess) {
        super(request);
        this.hosts = new LinkedHashSet<String>(hosts);
        this.logs = new LinkedHashSet<String>(logs);
        this.sshAuthenticationSuccess = sshAuthenticationSuccess;
    }

    public LinkedHashSet<String> getHosts() {
        return hosts;
    }

    public boolean isSshAuthenticationSuccess() {
        return sshAuthenticationSuccess;
    }

    public LinkedHashSet<String> getLogs() {
        return logs;
    }
}
