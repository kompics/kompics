package se.sics.kompics.wan.services.events;

import se.sics.kompics.Event;
import se.sics.kompics.wan.ssh.Host;

public class ActionSshTimeout extends Event {

	private final ActionRequest action;
        private final Host host;

    public ActionSshTimeout(ActionRequest action, Host host) {
        this.action = action;
        this.host = host;
    }

    public ActionRequest getAction() {
        return action;
    }

    public Host getHost() {
        return host;
    }


}
