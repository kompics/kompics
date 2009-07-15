package se.sics.kompics.wan.ssh;

import java.util.Map;

public interface Credentials extends Comparable<Credentials> {

	/**
	 * Checks username and password
	 * 
	 * @param auth
	 * @return true if username and password matched
	 */
	public abstract int compareTo(Credentials comp);

	public abstract Map<String, String> getAuthMap();

	public abstract String getKeyFilePassword();

	/**
	 * returns the full path to the private key used from ssh authentication
	 * 
	 * @return
	 */
	public abstract String getKeyPath();

	public abstract String getSshPassword();

	public abstract Map<String, String> getPlanetLabAuthMap();

	public abstract boolean isIgnoreCerificateErrors();

	public abstract String getSshLoginName();

}