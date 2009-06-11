package se.sics.kompics.wan.master.plab.plc;

import java.util.Map;

public class PlanetLabSite implements Comparable{
	private Integer site_id;

	private String name;

	private String abbreviated_name;

	private String login_base;

	private Boolean is_public;

	private Double latitude;

	private Double longitude;

	private String url;

	private Integer nodegroup_id;

	private String organization_id;

	private Integer ext_consortium_id;

	private Integer max_slices;

	public PlanetLabSite() {

	}

	public PlanetLabSite(Map map) {
		site_id = (Integer) map.get("site_id");
		name = (String) map.get("name");
		abbreviated_name = (String) map.get("abbreviated_name");
		login_base = (String) map.get("login_base");
		is_public = (Boolean) map.get("is_public");
		try {
			latitude = (Double) map.get("latitude");
			longitude = (Double) map.get("longitude");
		} catch (ClassCastException e) {
			latitude = null;
			longitude = null;
			//System.err.println("error with latitude or longitude of " + name);
		}
		url = (String) map.get("url");
		nodegroup_id = (Integer) map.get("nodegroup_id");

		try {
			organization_id = (String)map.get("organization_id");
		} catch (ClassCastException e) {
			//System.err.println("error with organization_id of: " + name);
		}
		
		try{
			ext_consortium_id = (Integer) map.get("ext_consortium_id");
		} catch (ClassCastException e) {
			//System.err.println("error with ext_consortium_id of: " + name);
		}
		
		max_slices = (Integer) map.get("max_slices");
	}

	public String getAbbreviated_name() {
		return abbreviated_name;
	}

	public void setAbbreviated_name(String abbreviated_name) {
		this.abbreviated_name = abbreviated_name;
	}

	public Integer getExt_consortium_id() {
		return ext_consortium_id;
	}

	public void setExt_consortium_id(Integer ext_consortium_id) {
		this.ext_consortium_id = ext_consortium_id;
	}

	public Boolean getIs_public() {
		return is_public;
	}

	public void setIs_public(Boolean is_public) {
		this.is_public = is_public;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public String getLogin_base() {
		return login_base;
	}

	public void setLogin_base(String login_base) {
		this.login_base = login_base;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Integer getMax_slices() {
		return max_slices;
	}

	public void setMax_slices(Integer max_slices) {
		this.max_slices = max_slices;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNodegroup_id() {
		return nodegroup_id;
	}

	public void setNodegroup_id(Integer nodegroup_id) {
		this.nodegroup_id = nodegroup_id;
	}

	public String getOrganization_id() {
		return organization_id;
	}

	public void setOrganization_id(String organization_id) {
		this.organization_id = organization_id;
	}

	public Integer getSite_id() {
		return site_id;
	}

	public void setSite_id(Integer site_id) {
		this.site_id = site_id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	
	public int hashCode(){
		return name.hashCode();
	}
	
	public int compareTo(Object obj){
		if(obj instanceof PlanetLabSite){
			PlanetLabSite comp = (PlanetLabSite) obj;
			return this.getName().compareTo(comp.getName());
		}
		return -1;
	}
}
