package se.sics.kompics.wan.ssh;


public interface Host extends Comparable<Host>{

	public abstract int compareTo(Host host);

	public abstract String getHostname();

	public abstract String getIp();

	public abstract String getConnectFailedPolicy();

	public abstract void setConnectFailedPolicy(String connectFailedPolicy);

	public abstract void setHostname(String hostname);

	public abstract void setIp(String ip);

	public abstract int getSessionId();

	public abstract void setSessionId(int sessionId);

}