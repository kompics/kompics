package se.sics.kompics.wan.plab.events;

import java.util.List;

import se.sics.kompics.Response;
import se.sics.kompics.wan.plab.PLabSite;

public class QueryPLabSitesResponse extends Response {

	private final List<PLabSite> listSites;

	public QueryPLabSitesResponse(QueryPLabSitesRequest request,
			List<PLabSite> listSites) {
		super(request);
		this.listSites = listSites;
	}

	/**
	 * @return the listSites
	 */
	public List<PLabSite> getListSites() {
		return listSites;
	}
}
