package se.sics.kompics.wan.ssh;

import java.net.InetAddress;


public interface Host extends Comparable<Host>{

	public abstract int compareTo(Host host);

	public abstract String getHostname();

	public abstract InetAddress getIp();

	public abstract String getConnectFailedPolicy();

	public abstract void setConnectFailedPolicy(String connectFailedPolicy);

	public abstract void setHostname(String hostname);

	public abstract void setIp(InetAddress ip);

	public abstract int getSessionId();

	public abstract void setSessionId(int sessionId);

}