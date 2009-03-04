
package se.sics.kompics.kdld.main.event;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * The <code>Hello</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Hello.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class LaunchRequest extends Message {

	private static final long serialVersionUID = 1711231235956452L;
	
	private final String repoUri;
	private final String groupId;
	private final String artifactId;
	private final String versionId;
	
	public LaunchRequest(String repoId, String groupId, String artifactId, String versionId,
			Address source, Address dest) {
		super(source, dest);
		this.repoUri = repoId;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.versionId = versionId;
	}

	public String getRepoUri() {
		return repoUri;
	}

	public String getGroupId() {
		return groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public String getVersionId() {
		return versionId;
	}

}