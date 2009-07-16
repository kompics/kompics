package se.sics.kompics.wan.plab;



public interface PLabHostDao {

	public void saveOrUpdate(PLabStore store);	
	public PLabStore load(String slice, String username);
}
