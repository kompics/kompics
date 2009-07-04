package se.sics.kompics.wan.master.plab.plc.events;

import se.sics.kompics.Response;

public class GetNodesForSliceResponse extends Response {

	private final int[] nodeIds;

	public GetNodesForSliceResponse(GetNodesForSliceRequest request,
			int[] nodeIds) {
		super(request);
		this.nodeIds =nodeIds;
	}


	/**
	 * @return the nodeIds
	 */
	public int[] getNodeIds() {
		return nodeIds;
	}
}
