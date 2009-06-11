package se.sics.kompics.wan.master.plab.plc;

import java.util.Map;

import se.sics.kompics.wan.master.plab.ExperimentHost;
import se.sics.kompics.wan.master.plab.plc.comon.CoMonStat;


public class PlanetLabHost extends ExperimentHost {

	private CoMonStat coMonStat;


	public PlanetLabHost() {
		super();
	}

	public PlanetLabHost(Map<String, String> nodeInfo) {
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
