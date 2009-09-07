package se.sics.kompics.wan.ssh.events;

import java.util.UUID;

import se.sics.kompics.Response;
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.Host;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshConnectResponse extends Response {

    private final int sessionId;
    private final Host host;
    private final UUID requestId;
    private final Credentials cred;

    public SshConnectResponse(SshConnectRequest request, int sessionId, UUID requestID,
            Host hostname, Credentials cred) {
        super(request);
        this.sessionId = sessionId;
        this.requestId = requestID;
        this.host = hostname;
        this.cred = cred;
    }

    /**
     * @return the sessionId
     */
    public int getSessionId() {
        return sessionId;
    }

    public Host getHost() {
        return host;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public Credentials getCred() {
        return cred;
    }
}
