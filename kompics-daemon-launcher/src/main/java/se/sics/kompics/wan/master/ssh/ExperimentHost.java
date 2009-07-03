package se.sics.kompics.wan.master.ssh;

import java.util.Map;

public class ExperimentHost implements Comparable<ExperimentHost> {

	protected String boot_state;

	protected String bwlimit;

	protected int cpuLoad;

	protected String connectFailedPolicy = "";

	protected int connectionId = -1;

	protected String hostname;

	protected String ip;

	protected int node_id;

	protected int site;

	public ExperimentHost() {

	}

	public ExperimentHost(Map<String, String> nodeInfo) {
		node_id = Integer.parseInt(nodeInfo.get("node_id"));
		hostname = (String) nodeInfo.get("hostname");
		boot_state = (String) nodeInfo.get("boot_state");
		ip = nodeInfo.get("ip");
		site = Integer.parseInt(nodeInfo.get("site_id"));
		bwlimit = nodeInfo.get("bwlimit");
		cpuLoad = Integer.parseInt(nodeInfo.get("cpu_load"));
	}

	public ExperimentHost(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public int compareTo(ExperimentHost host) {
		return this.getHostname().compareTo(host.getHostname());
	}

	public String getBoot_state() {
		return boot_state;
	}

	/**
	 * only here for testing and fallback, don't use...
	 * 
	 * @param data.hostname
	 */

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

}
