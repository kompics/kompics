package se.sics.kompics.wan.master.events;

import se.sics.kompics.Event;


public class ConnectedDaemonNotification extends Event {

    private final String daemonHost;

    public ConnectedDaemonNotification(String daemonHost) {
        this.daemonHost = daemonHost;
    }

    public String getDaemonHost() {
        return daemonHost;
    }
}
