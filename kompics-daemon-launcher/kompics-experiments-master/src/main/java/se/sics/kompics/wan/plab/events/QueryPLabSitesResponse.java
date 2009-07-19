package se.sics.kompics.wan.plab.events;

import java.util.Set;

import se.sics.kompics.Response;
import se.sics.kompics.wan.plab.PLabSite;

public class QueryPLabSitesResponse extends Response {

	private final Set<PLabSite> sites;

	public QueryPLabSitesResponse(QueryPLabSitesRequest request,
			Set<PLabSite> listSites) {
		super(request);
		this.sites = listSites;
	}

	/**
	 * @return the listSites
	 */
	public Set<PLabSite> getSites() {
		return sites;
	}
}
