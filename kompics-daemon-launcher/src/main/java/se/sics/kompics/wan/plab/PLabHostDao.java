package se.sics.kompics.wan.plab;

import java.util.List;


public interface PLabHostDao {

//	public void store(List<PLabHost> listHosts);
//	public List<PLabHost> loadAllHosts( );

	public void save(PLabStore store);	
	public PLabStore load();
}
