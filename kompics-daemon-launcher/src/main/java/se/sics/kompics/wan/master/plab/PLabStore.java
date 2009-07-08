package se.sics.kompics.wan.master.plab;

import java.util.Date;
import java.util.List;

public class PLabStore {
	
	private Date creationTime;

	private String slice;

	private String username;


	private List<PLabHost> hosts = null;

	private List<PLabSite> sites = null;

	private List<Integer> sliceNodes = null;

	public PLabStore() {
		// for hibernate serialization
	}

	public PLabStore(String slice, String username) {
		this.creationTime = new Date();
		this.slice = slice;
		this.username = username;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public List<PLabHost> getHosts() {
		return hosts;
	}

	public List<PLabSite> getSites() {
		return sites;
	}

	public List<Integer> getSliceNodes() {
		return sliceNodes;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public void setHosts(List<PLabHost> hosts) {
		this.hosts = hosts;
	}

	public void setSites(List<PLabSite> sites) {
		this.sites = sites;
	}

	public void setSliceNodes(List<Integer> sliceNodes) {
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
