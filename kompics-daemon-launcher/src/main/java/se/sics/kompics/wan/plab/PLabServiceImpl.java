package se.sics.kompics.wan.plab;


/**
 * The <code>PLabServiceImp</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PLabServiceImpl implements PLabService {

	private PLabHostDao pLabHostDao; // attribute name in "applicationContext-.xml"


	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.wan.master.plab.plc.PLabService#getHostsFromDB()
	 */
	/**
	 * @return the pLabHostDao
	 */
	public PLabHostDao getPLabHostDao() {
		return pLabHostDao;
	}

	/**
	 * @param pLabHostDao the pLabHostDao to set
	 */
	public void setPLabHostDao(PLabHostDao pLabHostDao) {
		this.pLabHostDao = pLabHostDao;
	}

//	@Override
//	public List<PLabHost> getHostsFromDB() {
//
//		return getPLabHostDao().loadAllHosts();
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * se.sics.kompics.wan.master.plab.plc.PLabService#storeHostsToDB(java.util
	 * .List)
	 */
//	@Override
//	public void storeHostsToDB(List<PLabHost> hosts) {
//
//		getPLabHostDao().store(hosts);
//
//	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.master.plab.PLabService#getPlanetLabStore()
	 */
	@Override
	public PLabStore load(String slice) {
		
		return getPLabHostDao().load(slice);
	}

	@Override
	public void save(PLabStore store) {
		getPLabHostDao().save(store);
		
	}

}
