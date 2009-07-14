package se.sics.kompics.wan.plab.events;

import java.util.Set;

import se.sics.kompics.Response;

public class GetNodesForSliceResponse extends Response {

	private final Set<Integer> nodeIds;

	public GetNodesForSliceResponse(GetNodesForSliceRequest request,
			Set<Integer> nodeIds) {
		super(request);
		this.nodeIds =nodeIds;
	}


	/**
	 * @return the nodeIds
	 */
	public Set<Integer> getNodeIds() {
		return nodeIds;
	}
}
