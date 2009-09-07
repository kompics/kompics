package se.sics.kompics.wan.master.events;

import java.util.Set;
import se.sics.kompics.Response;
import se.sics.kompics.wan.masterdaemon.events.DaemonAddress;


public class GetConnectedDaemonsResponse extends Response {

    private final Set<DaemonAddress> daemons;

    public GetConnectedDaemonsResponse(GetConnectedDameonsRequest request, 
            Set<DaemonAddress> daemons) {
        super(request);
        this.daemons = daemons;
    }

    public Set<DaemonAddress> getDaemons() {
        return daemons;
    }
}
