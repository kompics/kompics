package se.sics.kompics.wan.ssh;

import java.net.InetAddress;
import java.util.Map;

import se.sics.kompics.wan.plab.PLabHost;

public class ExperimentHost implements Host {


	protected String hostname=null;

	protected InetAddress ip=null;

	protected int sessionId=0;

	protected String connectFailedPolicy = "";

	
	public ExperimentHost() {
		
	}

	/**
	 * Called using results from Planetlab's XML-RPC API
	 * 
	 * @param nodeInfo
	 */
	@SuppressWarnings("unchecked")
	public ExperimentHost(Map nodeInfo) {
		hostname = (String) nodeInfo.get(PLabHost.HOSTNAME);
	}

	public ExperimentHost(String hostname) {
		this.hostname = hostname;
	}
	
	public ExperimentHost(int sessionId, String hostname, InetAddress ip) {
		this.sessionId = sessionId;
		this.hostname = hostname;
		this.ip = ip;
	}
	
	public ExperimentHost(Host host) {
		this.sessionId = host.getSessionId();
		this.hostname = host.getHostname();
		this.ip = host.getIp();
	}
	

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#compareTo(se.sics.kompics.wan.ssh.ExperimentHost)
	 */
	@Override
	public int compareTo(Host host) {
		return this.getHostname().compareTo(host.getHostname());
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#getConnectFailedPolicy()
	 */
	public String getConnectFailedPolicy() {
		return connectFailedPolicy;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#getHostname()
	 */
	
	public String getHostname() {
		return hostname;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#getIp()
	 */
	
	public InetAddress getIp() {
		return ip;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#getNodeId()
	 */

	public int hashCode() {
			int hash = 7;
			hash = 31 * hash + sessionId;
			hash = hash * ((hostname == null) ? 1 : hostname.hashCode()); 
			return hash;		
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#setConnectFailedPolicy(java.lang.String)
	 */
	public void setConnectFailedPolicy(String connectFailedPolicy) {
		this.connectFailedPolicy = connectFailedPolicy;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#setHostname(java.lang.String)
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#setIp(java.lang.String)
	 */
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(hostname);
		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#getSessionId()
	 */
	public int getSessionId() {
		return sessionId;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Host#setSessionId(int)
	 */
	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}


}
