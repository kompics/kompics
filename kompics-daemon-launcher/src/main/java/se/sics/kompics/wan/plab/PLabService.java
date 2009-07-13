package se.sics.kompics.wan.plab;

import java.util.List;


/**
 * The <code>PLabServer</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public interface PLabService {

//	public List<PLabHost> getHostsFromDB();	
//	public void storeHostsToDB(List<PLabHost> listHosts);
	
	public PLabStore load();
	
	public void save(PLabStore store);
}
