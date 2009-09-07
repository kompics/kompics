package se.sics.kompics.wan.plab.events;

import java.util.List;

import se.sics.kompics.Response;
import se.sics.kompics.wan.plab.PLabHost;

public class RankHostsUsingCoMonResponse extends Response {

	private final List<PLabHost> hosts;
	private final String[] ranking;
	
	public RankHostsUsingCoMonResponse(RankHostsUsingCoMonRequest request,
			List<PLabHost> hosts, String[] ranking) {
		super(request);
		this.hosts = hosts;
		this.ranking = ranking;
	}
	
	/**
	 * @return the hosts
	 */
	public List<PLabHost> getHosts() {
		return hosts;
	}
	
	public String[] getRanking() {
		return ranking;
	}

}
