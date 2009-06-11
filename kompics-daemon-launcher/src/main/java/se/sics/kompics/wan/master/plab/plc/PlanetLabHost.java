package se.sics.kompics.wan.master.plab.plc;

import java.util.Map;

import se.sics.kompics.wan.master.plab.plc.comon.CoMonStat;


public class PlanetLabHost implements Comparable {

	private String boot_state;

	//
	// private String mac;
	//
	// private String gateway;
	//
	// private String network;
	//
	// private String broadcast;
	//
	// private String netmask;
	//
	// private String dns1;
	//
	// private String dns2;
	//
	// private String bwlimit;
	//
	private CoMonStat coMonStat;

	private String connectFailedPolicy = "";

	private int connectionId = -1;

	private String hostname;

	// private String model;
	//
	// private String version;
	//
	// private String ssh_rsa_key;
	//
	// // private String session;
	//
	// private Object nodenetwork_id;
	//
	// private String method;
	//
	// private String type;
	//
	private String ip;

	private int node_id;

	private int site;

	// used for java bean xml serialization compatability
	public PlanetLabHost() {

	}

	public PlanetLabHost(Map nodeInfo) {
		node_id = (Integer) nodeInfo.get("node_id");
		hostname = (String) nodeInfo.get("hostname");
		boot_state = (String) nodeInfo.get("boot_state");
		// model = (String) nodeInfo.get("model");
		// version = (String) nodeInfo.get("version");
		// ssh_rsa_key = (String) nodeInfo.get("ssh_rsa_key");
		// // session = (String) nodeInfo.get("session");
		// nodenetwork_id = nodeInfo.get("nodenetwork_id");
		// method = (String) nodeInfo.get("method");
		// type = (String) nodeInfo.get("type");
		ip = (String) nodeInfo.get("ip");
		site = (Integer) nodeInfo.get("site_id");
		// mac = (String) nodeInfo.get("mac");
		// gateway = (String) nodeInfo.get("gateway");
		// network = (String) nodeInfo.get("network");
		// broadcast = (String) nodeInfo.get("broadcast");
		// netmask = (String) nodeInfo.get("netmask");
		// dns1 = (String) nodeInfo.get("dns1");
		// dns2 = (String) nodeInfo.get("dns2");
		// bwlimit = (String) nodeInfo.get("bwlimit");
	}

	public PlanetLabHost(String hostname) {
		this.hostname = hostname;
	}

	public int compareTo(Object obj) {
		if (obj instanceof PlanetLabHost) {
			PlanetLabHost comp = (PlanetLabHost) obj;
			return this.getHostname().compareTo(comp.getHostname());
		}
		return -1;
	}

	public String getBoot_state() {
		return boot_state;
	}

	/**
	 * only here for testing and fallback, don't use...
	 * 
	 * @param hostname
	 */

	public CoMonStat getComMonStat() {
		return coMonStat;
	}

	// public String getBroadcast() {
	// return broadcast;
	// }
	//
	// public String getBwlimit() {
	// return bwlimit;
	// }

	public String getConnectFailedPolicy() {
		return connectFailedPolicy;
	}

	public int getConnectionId() {
		return connectionId;
	}

	// public String getDns1() {
	// return dns1;
	// }
	//
	// public String getDns2() {
	// return dns2;
	// }
	//
	// public String getGateway() {
	// return gateway;
	// }

	public String getHostname() {
		return hostname;
	}

	public String getIp() {
		return ip;
	}

	// public String getMac() {
	// return mac;
	// }
	//
	// public String getMethod() {
	// return method;
	// }
	//
	// public String getModel() {
	// return model;
	// }
	//
	// public String getNetmask() {
	// return netmask;
	// }
	//
	// public String getNetwork() {
	// return network;
	// }

	public int getNode_id() {
		return node_id;
	}

	// public Object getNodenetwork_id() {
	// return nodenetwork_id;
	// }
	//
	// public String getSsh_rsa_key() {
	// return ssh_rsa_key;
	// }
	//
	// public String getType() {
	// return type;
	// }
	//
	// public String getVersion() {
	// return version;
	// }

	public int getSite() {
		return site;
	}

	public int hashCode() {
		return hostname.hashCode();
	}

	public void setBoot_state(String boot_state) {
		this.boot_state = boot_state;
	}

	public void setCoMonStat(CoMonStat coMonStat) {
		this.coMonStat = coMonStat;
	}

	public void setConnectFailedPolicy(String connectFailedPolicy) {
		this.connectFailedPolicy = connectFailedPolicy;
	}

	public void setConnectionId(int hostId) {
		this.connectionId = hostId;
	}

	// public void setBroadcast(String broadcast) {
	// this.broadcast = broadcast;
	// }
	//
	// public void setBwlimit(String bwlimit) {
	// this.bwlimit = bwlimit;
	// }
	//
	// public void setDns1(String dns1) {
	// this.dns1 = dns1;
	// }
	//
	// public void setDns2(String dns2) {
	// this.dns2 = dns2;
	// }
	//
	// public void setGateway(String gateway) {
	// this.gateway = gateway;
	// }

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	//
	// public void setMac(String mac) {
	// this.mac = mac;
	// }
	//
	// public void setMethod(String method) {
	// this.method = method;
	// }
	//
	// public void setModel(String model) {
	// this.model = model;
	// }
	//
	// public void setNetmask(String netmask) {
	// this.netmask = netmask;
	// }
	//
	// public void setNetwork(String network) {
	// this.network = network;
	// }
	//
	public void setNode_id(int node_id) {
		this.node_id = node_id;
	}

	//
	// public void setNodenetwork_id(Object nodenetwork_id) {
	// this.nodenetwork_id = nodenetwork_id;
	// }
	//
	// public void setSsh_rsa_key(String ssh_rsa_key) {
	// this.ssh_rsa_key = ssh_rsa_key;
	// }
	//
	// public void setType(String type) {
	// this.type = type;
	// }
	//
	// public void setVersion(String version) {
	// this.version = version;
	// }

	public void setSite(int site) {
		this.site = site;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(hostname);
		return buf.toString();
	}

}
