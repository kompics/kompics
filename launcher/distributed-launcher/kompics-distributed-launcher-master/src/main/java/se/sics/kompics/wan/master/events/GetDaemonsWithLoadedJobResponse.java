package se.sics.kompics.wan.master.events;

import java.util.Set;
import se.sics.kompics.Response;
import se.sics.kompics.wan.masterdaemon.events.DaemonAddress;


public class GetDaemonsWithLoadedJobResponse extends Response {

    private final int jobId;
    private final Set<DaemonAddress> daemons;

    public GetDaemonsWithLoadedJobResponse(GetDaemonsWithLoadedJobRequest request, int jobId,
            Set<DaemonAddress> daemons) {
        super(request);
        this.jobId = jobId;
        this.daemons = daemons;
    }

    public int getJobId() {
        return jobId;
    }

    public Set<DaemonAddress> getDaemons() {
        return daemons;
    }
}
