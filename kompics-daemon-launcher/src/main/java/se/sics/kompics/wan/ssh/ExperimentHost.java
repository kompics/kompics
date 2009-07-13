package se.sics.kompics.wan.ssh;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import se.sics.kompics.wan.plab.PLabHost;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public class ExperimentHost implements Comparable<ExperimentHost> {


	protected String hostname;

	protected String ip=null;

	protected int nodeId;

	@Transient 
	protected int sessionId;

	@Transient 
	protected String connectFailedPolicy = "";

	@Transient 
	protected int connectionId = -1;

	
	public ExperimentHost() {

	}

	/**
	 * Called using results from Planetlab's XML-RPC API
	 * 
	 * @param nodeInfo
	 */
	@SuppressWarnings("unchecked")
	public ExperimentHost(Map nodeInfo) {
		nodeId = (Integer) nodeInfo.get(PLabHost.NODE_ID);
		hostname = (String) nodeInfo.get(PLabHost.HOSTNAME);
	}

	public ExperimentHost(String hostname) {
		this.hostname = hostname;
	}
	
	public ExperimentHost(int sessionId, int nodeId, String hostname, String bootState,
			String ip, int site, String bwlimit, int cpuLoad) {
		this.sessionId = sessionId;
		this.nodeId = nodeId; 
		this.hostname = hostname;
		this.ip = ip;
	}
	
	public ExperimentHost(ExperimentHost host) {
		this.sessionId = host.getSessionId();
		this.nodeId = host.getNodeId(); 
		this.hostname = host.getHostname();
		this.ip = host.getIp();
	}
	

	@Override
	public int compareTo(ExperimentHost host) {
		return this.getHostname().compareTo(host.getHostname());
	}

	public String getConnectFailedPolicy() {
		return connectFailedPolicy;
	}

	public int getConnectionId() {
		return connectionId;
	}

	@Column(name="hostname", length=150, nullable=true)
	public String getHostname() {
		return hostname;
	}

	@Column(length=15)
	public String getIp() {
		return ip;
	}

	@Id
	@Column(name="node_id")
	public int getNodeId() {
		return nodeId;
	}


	public int hashCode() {
		return hostname.hashCode();
	}

	public void setConnectFailedPolicy(String connectFailedPolicy) {
		this.connectFailedPolicy = connectFailedPolicy;
	}

	public void setConnectionId(int hostId) {
		this.connectionId = hostId;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setNodeId(int node_id) {
		this.nodeId = node_id;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(hostname);
		return buf.toString();
	}

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

}
