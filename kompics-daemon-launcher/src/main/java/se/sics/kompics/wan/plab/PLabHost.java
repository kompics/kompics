package se.sics.kompics.wan.plab;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import se.sics.kompics.wan.ssh.ExperimentHost;

@Entity
public class PLabHost extends ExperimentHost {

	/**
	 * These are attributes that can be queried using the Planetlab API
	 */
	public static final String NODE_ID = "node_id";
	public static final String HOSTNAME = "hostname";
	public static final String BOOT_STATE = "boot_state";
	public static final String SITE_ID = PLabSite.SITE_ID;

	private CoMonStats coMonStat;

	protected String bootState;

	protected int siteId;

	protected boolean registeredForSlice = false;

	private transient int heartbeatTimeout = 0;

	public PLabHost() {
		super();
	}

	@SuppressWarnings("unchecked")
	public PLabHost(Map nodeInfo) {
		super(nodeInfo);

		bootState = (String) nodeInfo.get(PLabHost.BOOT_STATE);
		siteId = (Integer) nodeInfo.get(PLabHost.SITE_ID);

	}

	public PLabHost(ExperimentHost host) {
		super(host);
	}

	public PLabHost(PLabHost host) {
		super(host);
		this.coMonStat = new CoMonStats(host.getComMonStat());
	}

	
	public PLabHost(String hostname, int siteId) {
		super(hostname);
		this.siteId = siteId;
	}

	public PLabHost(String hostname) {
		super(hostname);
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
}
