package se.sics.kompics.wan.master.plab;

import java.util.List;


public interface PLabHostDao {


	public abstract void store(List<PLabHost> listHosts);
	public abstract List<PLabHost> loadAllHosts( );

}
