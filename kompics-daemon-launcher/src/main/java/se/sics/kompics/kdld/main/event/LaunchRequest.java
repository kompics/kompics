
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

	private static final long serialVersionUID = 1710717688555956452L;
	
	private final String groupId;
	private final String artifactId;
	private final String repoId;
	
	public LaunchRequest(String groupId, String artifactId, String repoId, 
			Address source, Address dest) {
		super(source, dest);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.repoId = repoId;		
	}
	
	public String getGroupId() {
		return groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	
	public String getRepoId() {
		return repoId;
	}
}