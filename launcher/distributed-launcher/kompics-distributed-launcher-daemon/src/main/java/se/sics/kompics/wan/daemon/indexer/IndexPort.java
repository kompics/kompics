package se.sics.kompics.wan.daemon.indexer;

import se.sics.kompics.wan.job.JobFound;
import se.sics.kompics.PortType;



/**
 * The <code>Index</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class IndexPort extends PortType {

	{
		negative(IndexStart.class);
		negative(IndexShutdown.class);
		negative(IndexerInit.class);
		negative(ListJobsLoadedRequest.class);

		positive(JobFound.class);
		positive(ListJobsLoadedResponse.class);
	}
}
