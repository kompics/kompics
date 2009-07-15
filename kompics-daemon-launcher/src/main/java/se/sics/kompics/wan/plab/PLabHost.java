package se.sics.kompics.wan.plab;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import se.sics.kompics.wan.ssh.Host;

@Entity
public class PLabHost implements Host {

	/**
	 * These are attributes that can be queried using the Planetlab API
	 */
	public static final String NODE_ID = "node_id";
	public static final String HOSTNAME = "hostname";
	public static final String BOOT_STATE = "boot_state";
	public static final String SITE_ID = PLabSite.SITE_ID;

	@Id
	protected int nodeId=0;

	@Transient
	private CoMonStats coMonStat;
	
	protected String hostname=null;

	protected String ip=null;

	protected int sessionId;

	protected String bootState;

	protected int siteId;

	protected String connectFailedPolicy = "";
	
	protected boolean registeredForSlice = false;

	private transient int heartbeatTimeout = 0;

	public PLabHost() {
		super();
	}

	@SuppressWarnings("unchecked")
	public PLabHost(Map nodeInfo) {
		nodeId = (Integer) nodeInfo.get(PLabHost.NODE_ID);
		hostname = (String) nodeInfo.get(PLabHost.HOSTNAME);
		bootState = (String) nodeInfo.get(PLabHost.BOOT_STATE);
		siteId = (Integer) nodeInfo.get(PLabHost.SITE_ID);
	}

	public PLabHost(Host host) {
		this.sessionId = host.getSessionId();
		this.ip = host.getIp();
		this.hostname = host.getHostname();
		this.connectFailedPolicy = host.getConnectFailedPolicy();
	}

	public PLabHost(PLabHost host) {
		this((Host) host);
		this.coMonStat = new CoMonStats(host.getComMonStat());
		this.bootState = host.bootState;
		this.siteId = host.siteId;
		this.registeredForSlice = host.registeredForSlice;
	}

	
	public PLabHost(String hostname, int siteId) {
		this(hostname);
		this.siteId = siteId;
	}

	public PLabHost(String hostname) {
		this.hostname = hostname;
	}

	@Transient
	public CoMonStats getComMonStat() {
		return coMonStat;
	}

	public void setCoMonStat(CoMonStats coMonStat) {
		this.coMonStat = coMonStat;
	}

	@Transient
	public int getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	public void incHearbeatTimeout() {
		heartbeatTimeout++;
	}

	public void zeroHearbeatTimeout() {
		heartbeatTimeout = 0;
	}

	@Transient
	public String getBootState() {
		return bootState;
	}

	public void setBootState(String bootState) {
		this.bootState = bootState;
	}

	@Column
	public int getSiteId() {
		return siteId;
	}

	public void setSiteId(int siteId) {
		this.siteId = siteId;
	}

	@Column(nullable = false, length = 1)
	@Type(type = "yes_no")
	public boolean isRegisteredForSlice() {
		return registeredForSlice;
	}

	public void setRegisteredForSlice(boolean registeredForSlice) {
		this.registeredForSlice = registeredForSlice;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(hostname);
		buf.append(": " + siteId + ", " + bootState + ", " + this.ip + ", " + this.sessionId
				+ "- stats=");
		if (coMonStat != null) {
			buf.append(coMonStat.toString());
		} else {
			buf.append("NO COMON STATS AVAILABLE FOR THIS HOST");
		}
		buf.append(";");
		return buf.toString();
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + super.hashCode();
		return hash;
	}

	@Column(name="hostname", length=150, nullable=true)
	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Column(length=15)
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	@Transient
	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	public String getConnectFailedPolicy() {
		return connectFailedPolicy;
	}

	public void setConnectFailedPolicy(String connectFailedPolicy) {
		this.connectFailedPolicy = connectFailedPolicy;
	}

	@Override
	public int compareTo(Host host) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
}
