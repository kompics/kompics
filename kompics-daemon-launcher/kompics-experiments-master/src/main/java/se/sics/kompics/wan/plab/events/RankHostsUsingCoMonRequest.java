package se.sics.kompics.wan.plab.events;


import se.sics.kompics.Request;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

public class RankHostsUsingCoMonRequest extends Request {
	
	private final PlanetLabCredentials cred;
	
	private final String[] coMonRankingCriteria;
	
	private final boolean forceDownload;
	
	public RankHostsUsingCoMonRequest(PlanetLabCredentials cred, boolean forceDownload,
			String... coMonRankingCriteria) {
		this.cred = cred;
		this.forceDownload = forceDownload;
		this.coMonRankingCriteria = coMonRankingCriteria;
	}
	
	/**
	 * @return the cred
	 */
	public PlanetLabCredentials getCred() {
		return cred;
	}
	
	public String[] getCoMonRankingCriteria() {
		return coMonRankingCriteria;
	}
	
	public boolean isForceDownload() {
		return forceDownload;
	}
}
