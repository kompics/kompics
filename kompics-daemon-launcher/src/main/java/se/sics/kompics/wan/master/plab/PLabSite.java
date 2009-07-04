package se.sics.kompics.wan.master.plab;

import java.util.Map;

public class PLabSite implements Comparable<PLabSite> {

	public static final String SITE_ID 				= "site_id";
	public static final String NAME 				= "name";
	public static final String ABBREVIATED_NAME 	= "abbreviated_name";
	public static final String LATITUDE 			= "latitude";
	public static final String LONGITUDE 			= "longitude";
	public static final String LOGIN_BASE 			= "login_base";
	public static final String IS_PUBLIC			= "is_public";
	public static final String NODEGROUP_ID 		= "nodegroup_id";
	public static final String ORGANIZATION_ID 		= "organization_id";
	public static final String URL 					= "url";
	public static final String EXT_CONSORTIUM_ID 	= "ext_consortium_id";
	public static final String MAX_SLICES 			= "max_slices";
	
	
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

	public PLabSite() {

	}

	@SuppressWarnings("unchecked")
	public PLabSite(Map map) {
		site_id = (Integer) map.get(PLabSite.SITE_ID);
		name = (String) map.get(PLabSite.NAME);
		abbreviated_name = (String) map.get(PLabSite.ABBREVIATED_NAME);
		login_base = (String) map.get(PLabSite.LOGIN_BASE);
		is_public = (Boolean) map.get(PLabSite.IS_PUBLIC);
		try {
			latitude = (Double) map.get(PLabSite.LATITUDE);
			longitude = (Double) map.get(PLabSite.LONGITUDE);
		} catch (ClassCastException e) {
			latitude = null;
			longitude = null;
			//System.err.println("error with latitude or longitude of " + name);
		}
		url = (String) map.get(PLabSite.URL);
		nodegroup_id = (Integer) map.get(PLabSite.NODEGROUP_ID);

		try {
			organization_id = (String)map.get(ORGANIZATION_ID);
		} catch (ClassCastException e) {
			//System.err.println("error with organization_id of: " + name);
		}
		
		try{
			ext_consortium_id = (Integer) map.get(PLabSite.EXT_CONSORTIUM_ID);
		} catch (ClassCastException e) {
			//System.err.println("error with ext_consortium_id of: " + name);
		}
		
		max_slices = (Integer) map.get(PLabSite.MAX_SLICES);
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
	
	public int compareTo(PLabSite obj){
			PLabSite comp = (PLabSite) obj;
			return this.getName().compareTo(comp.getName());
	}
}
