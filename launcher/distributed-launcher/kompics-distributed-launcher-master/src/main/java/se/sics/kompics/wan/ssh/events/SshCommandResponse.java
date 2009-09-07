package se.sics.kompics.wan.ssh.events;

import se.sics.kompics.Response;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class SshCommandResponse extends Response {

    private final String commandResponse;
    private final int sessionId;
    private final boolean sshAuthenticationSuccess;

    public SshCommandResponse(SshCommandRequest request, int sessionId,
            String commandResponse, boolean sshAuthenticationSuccess) {
        super(request);
        this.commandResponse = commandResponse;
        this.sessionId = sessionId;
        this.sshAuthenticationSuccess = sshAuthenticationSuccess;
    }

    /**
     * @return the command
     */
    public String getCommandResponse() {
        return commandResponse;
    }

    /**
     * @return the sessionId
     */
    public int getSessionId() {
        return sessionId;
    }

    public boolean isSshAuthenticationSuccess() {
        return sshAuthenticationSuccess;
    }
}
