package se.sics.kompics.wan.master.ssh;

import java.util.Map;

import se.sics.kompics.wan.master.plab.PLabSite;

public class ExperimentHost implements Comparable<ExperimentHost> {

	public static final String NODE_ID 			= "node_id";
	public static final String HOSTNAME 		= "hostname";
	public static final String BOOT_STATE 		= "boot_state";
	public static final String IP 				= "ip";
	public static final String BWLIMIT	 		= "bwlimit";
	public static final String CPU_LOAD 		= "cpu_load";
	public static final String MODEL			= "model";
	public static final String VERSION  		= "version";
	public static final String SSH_RSA_KEY		= "ssh_rsa_key";
	//session needs administration privileges
	public static final String SESSION			= "session"; 
	public static final String NODENETWORK_ID 	= "nodenetwork_id";
	public static final String METHOD			= "method";
	public static final String TYPE				= "type";
	/**
	 * @return the type
	 */
	public static String getType() {
		return TYPE;
	}

	/**
	 * @param bwlimit the bwlimit to set
	 */
	public void setBwlimit(String bwlimit) {
		this.bwlimit = bwlimit;
	}

	/**
	 * @param cpuLoad the cpuLoad to set
	 */
	public void setCpuLoad(int cpuLoad) {
		this.cpuLoad = cpuLoad;
	}

	public static final String MAC 				= "mac";
	public static final String GATEWAY			= "gateway";
	public static final String NETWORK			= "network";
	public static final String BROADCAST		= "broadcast";
	public static final String NETMASK			= "netmask";
	public static final String DNS1				= "dns1";
	public static final String DNS2				= "dns2";
	
	protected String boot_state;

	protected String bwlimit;

	protected int cpuLoad;

	protected String connectFailedPolicy = "";

	protected int connectionId = -1;

	protected String hostname;

	protected String ip;

	protected int node_id;

	protected int site;

	protected int sessionId;

	
	public ExperimentHost() {

	}

	@SuppressWarnings("unchecked")
	public ExperimentHost(Map nodeInfo) {
		node_id = (Integer) nodeInfo.get(ExperimentHost.NODE_ID);
		hostname = (String) nodeInfo.get(ExperimentHost.HOSTNAME);
		boot_state = (String) nodeInfo.get(ExperimentHost.BOOT_STATE);
		ip = (String) nodeInfo.get(ExperimentHost.IP);
		site = (Integer) nodeInfo.get(PLabSite.SITE_ID);
		bwlimit = (String) nodeInfo.get(ExperimentHost.BWLIMIT);
//		cpuLoad = (Integer) nodeInfo.get(ExperimentHost.CPU_LOAD);
	}

	public ExperimentHost(String hostname) {
		this.hostname = hostname;
	}
	
	public ExperimentHost(int sessionId, int nodeId, String hostname, String bootState,
			String ip, int site, String bwlimit, int cpuLoad) {
		this.sessionId = sessionId;
		this.node_id = nodeId; 
		this.hostname = hostname;
		this.boot_state = bootState;
		this.ip = ip;
		this.site = site;
		this.bwlimit = bwlimit;
		this.cpuLoad = cpuLoad;
	}
	
	public ExperimentHost(ExperimentHost host) {
		this.sessionId = host.getSessionId();
		this.node_id = host.getNode_id(); 
		this.hostname = host.getHostname();
		this.boot_state = host.getBoot_state();
		this.ip = host.getIp();
		this.site = host.getSite();
		this.bwlimit = host.getBwlimit();
		this.cpuLoad = host.getCpuLoad();
	}
	

	@Override
	public int compareTo(ExperimentHost host) {
		return this.getHostname().compareTo(host.getHostname());
	}

	public String getBoot_state() {
		return boot_state;
	}

	public String getBwlimit() {
		return bwlimit;
	}

	public int getCpuLoad() {
		return cpuLoad;
	}

	public String getConnectFailedPolicy() {
		return connectFailedPolicy;
	}

	public int getConnectionId() {
		return connectionId;
	}

	public String getHostname() {
		return hostname;
	}

	public String getIp() {
		return ip;
	}

	public int getNode_id() {
		return node_id;
	}

	public int getSite() {
		return site;
	}

	public int hashCode() {
		return hostname.hashCode();
	}

	public void setBoot_state(String boot_state) {
		this.boot_state = boot_state;
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

	public void setNode_id(int node_id) {
		this.node_id = node_id;
	}

	public void setSite(int site) {
		this.site = site;
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
