package se.sics.kompics.wan.master.plab.plc;

import java.util.Map;

import se.sics.kompics.wan.plab.CoMonStats;
import se.sics.kompics.wan.ssh.ExperimentHost;


public class PlanetLabHost extends ExperimentHost {

	private CoMonStats coMonStat;

private int siteId;
private String bootState;
	
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

	public PlanetLabHost() {
		super();
	}

	@SuppressWarnings("unchecked")
	public PlanetLabHost(Map nodeInfo) {
		super(nodeInfo);
		
	}

	public PlanetLabHost(String hostname) {
		super(hostname);
	}

	public CoMonStats getComMonStat() {
		return coMonStat;
	}

	public void setCoMonStat(CoMonStats coMonStat) {
		this.coMonStat = coMonStat;
	}
}
