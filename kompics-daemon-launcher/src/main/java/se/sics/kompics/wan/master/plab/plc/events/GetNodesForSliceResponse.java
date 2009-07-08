package se.sics.kompics.wan.master.plab.plc.events;

import java.util.List;

import se.sics.kompics.Response;

public class GetNodesForSliceResponse extends Response {

	private final List<Integer> nodeIds;

	public GetNodesForSliceResponse(GetNodesForSliceRequest request,
			List<Integer> nodeIds) {
		super(request);
		this.nodeIds =nodeIds;
	}


	/**
	 * @return the nodeIds
	 */
	public List<Integer> getNodeIds() {
		return nodeIds;
	}
}
