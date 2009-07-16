package se.sics.kompics.wan.plab;

import java.util.Date;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

@Entity
	public class PLabStore {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id = 0;

	@Column(name="slice")
	private String slice="";

	private String username="";

	private Date creationTime;

//	@IndexColumn(name="nodeId")
//	@OrderBy("nodeId")
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<PLabHost> hosts = null;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<PLabSite> sites = null;


	public PLabStore() {
		// for hibernate serialization
	}

	public PLabStore(String slice, String username) {
		this.creationTime = new Date();
		this.slice = slice;
		this.username = username;
		this.id = slice.hashCode() + username.hashCode();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getCreationTime() {
		return creationTime;
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

	public Set<PLabHost> getHosts() {
		return hosts;
	}
	
	@Transient
	public SortedSet<PLabHost> getRunningHostsForThisSlice()
	{
		SortedSet<PLabHost> setHosts = new TreeSet<PLabHost>();
		for (PLabHost h : hosts) {
			if (h.isRegisteredForSlice() == true) {
					if (h.getBootState().compareTo("boot") == 0) {
						setHosts.add(h);
					}
			}
		}
		System.out.println("Running hosts number: " + setHosts.size());
		return setHosts;
	}
	

	public Set<PLabSite> getSites() {
		return sites;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public void setHosts(Set<PLabHost> hosts) {
		this.hosts = hosts;
	}

	public void setSites(Set<PLabSite> sites) {
		this.sites = sites;
	}

}
