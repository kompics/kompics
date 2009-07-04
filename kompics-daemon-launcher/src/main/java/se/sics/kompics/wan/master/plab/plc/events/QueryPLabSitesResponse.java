package se.sics.kompics.wan.master.plab.plc.events;

import java.util.List;

import se.sics.kompics.Response;
import se.sics.kompics.wan.master.plab.PLabSite;

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
