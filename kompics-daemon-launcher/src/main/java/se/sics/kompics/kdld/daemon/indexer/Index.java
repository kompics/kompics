package se.sics.kompics.kdld.daemon.indexer;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequestMsg;
import se.sics.kompics.kdld.daemon.ListJobsLoadedResponseMsg;


/**
 * The <code>Index</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class Index extends PortType {

	{
		negative(IndexStart.class);
		negative(IndexStop.class);
		negative(IndexerInit.class);
		negative(ListJobsLoadedRequestMsg.class);

		positive(JobFoundLocally.class);
		positive(ListJobsLoadedResponseMsg.class);
	}
}
