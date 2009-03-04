
package se.sics.kompics.kdld.main.event;

import se.sics.kompics.Event;

/**
 * The <code>Deploy</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: Hello.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class Deploy extends Event {

	private static final long serialVersionUID = 1710717688555956452L;
	
	private final String repoUri;
	private final String groupId;
	private final String artifactId;
	private final String versionId;
	
	public Deploy(String repoUri, String groupId, String artifactId, String versionId) {
		this.repoUri = repoUri;
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