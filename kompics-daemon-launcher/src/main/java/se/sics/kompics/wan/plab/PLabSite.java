package se.sics.kompics.wan.plab;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity 
public class PLabSite implements Comparable<PLabSite> {

	public static final String SITE_ID 				= "site_id";
	public static final String NAME 				= "name";
	public static final String ABBREVIATED_NAME 	= "abbreviated_name";
	public static final String LOGIN_BASE 			= "login_base";
	
	private String name;

	private Integer siteId;

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

	public String getAbbreviatedName() {
		return abbreviatedName;
	}

	public void setAbbreviatedName(String abbreviated_name) {
		this.abbreviatedName = abbreviated_name;
	}


	public String getLoginBase() {
		return loginBase;
	}

	public void setLoginBase(String login_base) {
		this.loginBase = login_base;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
//	@Column(name="site_id")
	public Integer getSiteId() {
		return siteId;
	}

	public void setSiteId(Integer site_id) {
		this.siteId = site_id;
	}


	public int hashCode(){
		return name.hashCode();
	}
	
	public int compareTo(PLabSite obj){
			PLabSite comp = (PLabSite) obj;
			return this.getName().compareTo(comp.getName());
	}
}
