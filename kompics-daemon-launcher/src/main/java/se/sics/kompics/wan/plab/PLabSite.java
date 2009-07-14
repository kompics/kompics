package se.sics.kompics.wan.plab;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity 
public class PLabSite implements Comparable<PLabSite> {

	public static final String SITE_ID 				= "site_id";
	public static final String NAME 				= "name";
	public static final String ABBREVIATED_NAME 	= "abbreviated_name";
	public static final String LOGIN_BASE 			= "login_base";
	
	private String name=null;

	private Integer siteId=0;

	private String abbreviatedName;

	private String loginBase;

	public PLabSite() {

	}

	@SuppressWarnings("unchecked")
	public PLabSite(Map map) {
		siteId = (Integer) map.get(PLabSite.SITE_ID);
		name = (String) map.get(PLabSite.NAME);
		abbreviatedName = (String) map.get(PLabSite.ABBREVIATED_NAME);
		loginBase = (String) map.get(PLabSite.LOGIN_BASE);
	}

	@Column(nullable=true)
	public String getAbbreviatedName() {
		return abbreviatedName;
	}

	public void setAbbreviatedName(String abbreviated_name) {
		this.abbreviatedName = abbreviated_name;
	}

	@Column(nullable=true)
	public String getLoginBase() {
		return loginBase;
	}

	public void setLoginBase(String login_base) {
		this.loginBase = login_base;
	}

	@Column(nullable=true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
	public Integer getSiteId() {
		return siteId;
	}

	public void setSiteId(Integer site_id) {
		this.siteId = site_id;
	}


	public int hashCode(){
		int hash = 7;
		hash = 31 * hash + siteId;
		hash *= (name == null ? 1 : name.hashCode());
		return hash;
	}
	
	public int compareTo(PLabSite obj){
			PLabSite comp = (PLabSite) obj;
			return this.getName().compareTo(comp.getName());
	}
}
