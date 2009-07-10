package se.sics.kompics.wan.plab;

import java.util.List;


public interface PLabHostDao {

	public void store(List<PLabHost> listHosts);
	public List<PLabHost> loadAllHosts( );

	public void store(PLabStore store);	
	public PLabStore loadPLabStore();
}
