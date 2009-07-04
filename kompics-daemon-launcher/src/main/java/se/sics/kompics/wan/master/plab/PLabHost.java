package se.sics.kompics.wan.master.plab;

import java.util.Map;

import se.sics.kompics.wan.master.plab.plc.comon.CoMonStat;
import se.sics.kompics.wan.master.ssh.ExperimentHost;


public class PLabHost extends ExperimentHost {

	private CoMonStat coMonStat;


	public PLabHost() {
		super();
	}

	public PLabHost(Map<String, String> nodeInfo) {
		super(nodeInfo);
		
	}

	public PLabHost(String hostname) {
		super(hostname);
	}

	public CoMonStat getComMonStat() {
		return coMonStat;
	}

	public void setCoMonStat(CoMonStat coMonStat) {
		this.coMonStat = coMonStat;
	}
}
