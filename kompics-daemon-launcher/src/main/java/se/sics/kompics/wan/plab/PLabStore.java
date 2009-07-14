package se.sics.kompics.wan.plab;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@Entity
public class PLabStore {

	private String slice;

	private String username;

	private Date creationTime;

	private List<PLabHost> hosts = null;

	private List<PLabSite> sites = null;

//	private List<Integer> sliceNodes = null;

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

	@Id
	@Column(name="slice")
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

	@OneToMany
	@OrderBy("hostname")
	public List<PLabHost> getHosts() {
		return hosts;
	}

	@OneToMany // (mappedBy = "PlabStore", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@OrderBy("siteId")
	public List<PLabSite> getSites() {
		return sites;
	}


//	@Transient
//	public List<Integer> getSliceNodes() {
//		return sliceNodes;
//	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public void setHosts(List<PLabHost> hosts) {
		this.hosts = hosts;
	}

	public void setSites(List<PLabSite> sites) {
		this.sites = sites;
	}

//	public void setSliceNodes(List<Integer> sliceNodes) {
//		this.sliceNodes = sliceNodes;
//	}

}
