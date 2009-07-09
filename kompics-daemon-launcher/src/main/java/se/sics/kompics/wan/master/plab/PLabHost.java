package se.sics.kompics.wan.master.plab;

import java.util.Map;

import se.sics.kompics.wan.master.plab.plc.comon.CoMonStat;
import se.sics.kompics.wan.master.ssh.ExperimentHost;


public class PLabHost extends ExperimentHost {

	private CoMonStat coMonStat;

	private int id;
	

	
	public PLabHost() {
		super();
	}

	@SuppressWarnings("unchecked")
	public PLabHost(Map nodeInfo) {
		super(nodeInfo);
		
	}

	public PLabHost(ExperimentHost host) {
		super(host);		
	}

	public PLabHost(String hostname) {
		super(hostname);
	}


	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	public CoMonStat getComMonStat() {
		return coMonStat;
	}

	public void setCoMonStat(CoMonStat coMonStat) {
		this.coMonStat = coMonStat;
	}
	
}
