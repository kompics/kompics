package se.sics.kompics.wan.ssh.events;

import java.util.UUID;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.ssh.SshComponent;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class DownloadFileRequest extends SshCommandRequest {

    private final String localDir;
    private final String remotePath;
    private final String patternMatch;
    private final String fileType;
    private final int sessionId;

    public DownloadFileRequest(UUID requestId, int sessionId, String remotePath, String localDir,
            String patternMatch,
            String fileType,
            double timeout, boolean stopOnError) {
        super(requestId, sessionId, "#download " + remotePath + " " + localDir + " " + patternMatch + " " + fileType,
                timeout, stopOnError);
        this.remotePath = remotePath;
        this.localDir = localDir;
        this.patternMatch = patternMatch;
        this.fileType = fileType;
        this.sessionId = sessionId;
    }

    public DownloadFileRequest(UUID requestId, int sessionId, String remotePath, String localDir) {
        this(requestId, sessionId, remotePath, localDir, "", SshComponent.FLAT,
                PlanetLabConfiguration.DEFAULT_SSH_COMMAND_TIMEOUT, true);
    }

    /**
     * @return the command
     */
    public String getLocalDir() {
        return localDir;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public String getFileType() {
        return fileType;
    }

    public String getPatternMatch() {
        return patternMatch;
    }

    /**
     * @return the sessionId
     */
    public int getSessionId() {
        return sessionId;
    }

    /**
     * @return the timeout
     */
    public double getTimeout() {
        return timeout;
    }

    /**
     * @return the stopOnError
     */
    public boolean isStopOnError() {
        return stopOnError;
    }
//	public String getCommand() {
//		StringBuffer b =  new StringBuffer();
//		b.append(localDir);
//		b.append(Character.SPACE_SEPARATOR);
//		b.append(remotePath);
//		b.append(Character.SPACE_SEPARATOR);
//		b.append(patternMatch);
//		b.append(Character.SPACE_SEPARATOR);
//		b.append(fileType);
//		return b.toString();
//	}
}
