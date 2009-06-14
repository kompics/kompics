package se.sics.kompics.wan.master.scp;

import java.io.File;

import se.sics.kompics.Event;
import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.ExperimentHost;

/**
 * The <code>ConnectSsh</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class ScpCopyFileTo extends Event {

	private Credentials credentials;
	private File baseDir;

	public ScpCopyFileTo(Credentials credentials, File baseDir) {
		this.credentials = credentials;
		this.baseDir = baseDir;
	}

	/**
	 * @return the credentials
	 */
	public Credentials getCredentials() {
		return credentials;
	}

	/**
	 * @return the baseDir
	 */
	public File getBaseDir() {
		return baseDir;
	}
}
