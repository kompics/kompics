package se.sics.kompics.wan.services.events;

import se.sics.kompics.Event;
import se.sics.kompics.wan.services.ExperimentServicesComponent;
import se.sics.kompics.wan.ssh.Host;

public class ActionProgressResponse extends Event {

    private final Host host;
    private final ExperimentServicesComponent.ServicesStatus status;
    private final ExperimentServicesComponent.Action action;
    private final boolean sshAuthenticationSuccess;

    // XXX can we implement this as Request/Response?
    // Can you have multiple responses for a single request????
    public ActionProgressResponse(Host hosts,
            ExperimentServicesComponent.Action action,
            ExperimentServicesComponent.ServicesStatus status,
            boolean sshAuthenticationSuccess) {
        this.host = hosts;
        this.action = action;
        this.status = status;
        this.sshAuthenticationSuccess = sshAuthenticationSuccess;

    }

    public Host getHost() {
        return host;
    }

    public ExperimentServicesComponent.ServicesStatus getStatus() {
        return status;
    }

    public ExperimentServicesComponent.Action getAction() {
        return action;
    }

    public boolean isSshAuthenticationSuccess() {
        return sshAuthenticationSuccess;
    }
}
