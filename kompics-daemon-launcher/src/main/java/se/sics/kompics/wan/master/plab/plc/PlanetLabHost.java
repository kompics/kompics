package se.sics.kompics.wan.master.plab.plc;

import java.util.Map;

import se.sics.kompics.wan.master.plab.plc.comon.CoMonStat;
import se.sics.kompics.wan.master.ssh.ExperimentHost;


public class PlanetLabHost extends ExperimentHost {

	private CoMonStat coMonStat;


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

	public CoMonStat getComMonStat() {
		return coMonStat;
	}

	public void setCoMonStat(CoMonStat coMonStat) {
		this.coMonStat = coMonStat;
	}
}
