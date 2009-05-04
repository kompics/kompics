package se.sics.kompics.kdld.job;

import se.sics.kompics.PortType;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequest;
import se.sics.kompics.kdld.daemon.ListJobsLoadedResponse;


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
		negative(ListJobsLoadedRequest.class);

		positive(JobFoundLocally.class);
		positive(ListJobsLoadedResponse.class);
	}
}
