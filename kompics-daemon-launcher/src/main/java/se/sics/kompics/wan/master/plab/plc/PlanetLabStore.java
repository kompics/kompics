package se.sics.kompics.wan.master.plab.plc;

import java.util.Date;

public class PlanetLabStore {
	private Date creationTime;

	private String slice;

	private String username;


	private PlanetLabHost[] hosts;

	private PlanetLabSite[] sites;

	private int[] sliceNodes;

	// for java xml serialization
	 public PlanetLabStore() {

	}

	public PlanetLabStore(String slice, String username) {
		this.creationTime = new Date();
		this.slice = slice;
		this.username = username;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public PlanetLabHost[] getHosts() {
		return hosts;
	}

	public PlanetLabSite[] getSites() {
		return sites;
	}

	public int[] getSliceNodes() {
		return sliceNodes;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public void setHosts(PlanetLabHost[] hosts) {
		this.hosts = hosts;
	}

	public void setSites(PlanetLabSite[] sites) {
		this.sites = sites;
	}

	public void setSliceNodes(int[] sliceNodes) {
		this.sliceNodes = sliceNodes;
	}

	public String getSlice() {
		return slice;
	}

	public void setSlice(String slice) {
		this.slice = slice;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
