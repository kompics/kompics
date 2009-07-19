package se.sics.kompics.wan.plab;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

@Entity
public class PLabStore {

	@Id()
	// @GeneratedValue(strategy = GenerationType.AUTO)
	private int id = 0;

	@Column(name = "slice")
	private String slice = "";

	@Column(name = "username")
	private String username = "";

	@Column(name = "creationTime")
	private Date creationTime;

	// @IndexColumn(name="nodeId")
	// @OrderBy("nodeId")
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
	
	public void addHosts(Set<PLabHost> hosts) {
		this.hosts.addAll(hosts);
	}

	@Transient
	public Set<PLabHost> getRunningHostsForThisSlice() {
		Set<PLabHost> setHosts = new HashSet<PLabHost>();
		for (PLabHost h : hosts) {
			if (h.isRegisteredForSlice() == true) {
				if (h.getBootState().compareTo("boot") == 0) {
					System.out.println("Adding host to ready hosts: " + h.getHostname());
					setHosts.add(h);
				}
			}
		}
		System.out.println("Ready hosts number: " + setHosts.size());
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

	@Override
	public int hashCode() {
		int hash = 31;
		return hash * id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || ! (obj instanceof PLabStore)) {
			return false;
		}
		
		PLabStore that = (PLabStore) obj;
		
		if (this.id != that.id) {
			return false;
		}
		if (this.slice.compareTo(that.slice)!= 0) {
			return false;
		}
		if (this.username.compareTo(that.username)!= 0) {
			return false;
		}
		
		return true;
	}

}
