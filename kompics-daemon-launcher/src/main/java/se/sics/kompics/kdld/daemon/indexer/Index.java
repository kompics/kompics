package se.sics.kompics.kdld.daemon.indexer;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequestMsg;


/**
 * The <code>Index</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class Index extends PortType {

	{
		negative(IndexStart.class);
		negative(IndexShutdown.class);
		negative(IndexerInit.class);
		negative(ListJobsLoadedRequest.class);

		positive(JobsFound.class);
		positive(ListJobsLoadedResponse.class);
	}
}
