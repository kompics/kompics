package se.sics.kompics.wan.plab;

import java.util.List;


/**
 * The <code>PLabServer</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public interface PLabService {

	public PLabStore load(String slice);
	
	public void save(PLabStore store);
}
