package se.sics.kompics.wan.master.plab;

import java.util.List;


public interface PLabHostDao {

	public void store(List<PLabHost> listHosts);
	public List<PLabHost> loadAllHosts( );

}
