package se.sics.kompics.wan.plab;



/**
 * The <code>PLabServer</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public interface PLabService {

	public PLabStore load(String slice, String username);
	
	public void save(PLabStore store);
	

}
