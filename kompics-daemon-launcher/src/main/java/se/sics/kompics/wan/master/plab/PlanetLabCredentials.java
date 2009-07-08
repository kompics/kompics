package se.sics.kompics.wan.master.plab;

import java.util.HashMap;
import java.util.Map;

import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.ssh.Credentials;


/**
 * Class holding credentials used both for communication with PLC and for
 * authentication with planetlab hosts.
 * 
 * @author isdal
 * 
 */
public class PlanetLabCredentials extends Credentials {

	public static final String USERNAME = "Username";
	public static final String AUTHSTRING = "AuthString";
	public static final String AUTHMETHOD = "AuthMethod";
	 public static final String SLICE = "Slice";
	 public static final String ROLE = "Role";
	 public static final String PRIVATE_KEY_FILE = "PrivateKeyFile";
	 public static final String PRIVATE_KEY_PASSWORD = "PrivateKeyPassword";

	
	private final String slice;

	private final String role;


	public PlanetLabCredentials(Map<String, String> credentials) {
		super(credentials);
		this.slice = (String) credentials.get(PlanetLabCredentials.SLICE);
		this.role = (String) credentials.get(PlanetLabCredentials.ROLE);
	}

	public PlanetLabCredentials(String username, String password, String slice,
			String role, String keyPath, String keyFilePassword) {
		super(username, password, keyPath, keyFilePassword);
		this.slice = slice;
		this.role = role;
	}
	
	public PlanetLabCredentials(String username, String password, String slice,
			String keyPath, String keyFilePassword) {
		super(username, password, keyPath, keyFilePassword);
		this.slice = slice;
		this.role = PlanetLabConfiguration.DEFAULT_PL_ROLE;
	}

	/**
	 * Checks username and password
	 * 
	 * @param auth
	 * @return true if username and password matched
	 */
	public boolean authenticate(PlanetLabCredentials comp) {
		if (this.authenticateMD5(comp)) {
			return true;
		}

		if (this.authenticateClearTest(comp)) {
			return true;
		}

		return false;
	}

	public boolean authenticateClearTest(PlanetLabCredentials comp) {

		if (!username.equals(comp.getUsername())) {
			return false;
		}
		if (!password.equals(comp.getPassword())) {
			return false;
		}
		System.err.println("Authenticated with Clear Text: "
				+ comp.getUsername());
		return true;
	}

	public boolean authenticateMD5(PlanetLabCredentials comp) {
		if (!usernameMD5.equals(comp.getUsername())) {
			return false;
		}

		if (!passwordMD5.equals(comp.getPassword())) {
			return false;
		}

		// System.out.println("Authenticated with md5");
		return true;
	}

	public boolean equals(PlanetLabCredentials comp) {
		if (!username.equals(comp.getUsername())) {
			return false;
		}
		if (!slice.equals(comp.getSlice())) {
			return false;
		}

		if (!password.equals(comp.getPassword())) {
			return false;
		}
		if (!role.equals(comp.getRole())) {
			return false;
		}
		return true;
	}

	public boolean equals(Map<String, String> authMap) {
		PlanetLabCredentials c = new PlanetLabCredentials(authMap);
		return this.equals(c);

	}

	public boolean equals(Object obj) {
		if (obj instanceof PlanetLabCredentials) {
			PlanetLabCredentials comp = (PlanetLabCredentials) obj;
			return this.equals(comp);
		}
		return false;
	}

	public Map<String, String> getAuthMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put(PlanetLabCredentials.USERNAME, usernameMD5);
		authMap.put(PlanetLabCredentials.AUTHSTRING, passwordMD5);
		 authMap.put(PlanetLabCredentials.SLICE, slice);
		 authMap.put(PlanetLabCredentials.ROLE, role);
		 authMap.put(PlanetLabCredentials.PRIVATE_KEY_FILE, keyPath);
		 authMap.put(PlanetLabCredentials.PRIVATE_KEY_PASSWORD, keyFilePassword);
		return authMap;
	}


	@Override
	public Map<String,String> getPLCMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put(PlanetLabCredentials.USERNAME, username);
		authMap.put(PlanetLabCredentials.AUTHMETHOD, "password");
		authMap.put(PlanetLabCredentials.AUTHSTRING, password);
		authMap.put(PlanetLabCredentials.SLICE, slice);
		authMap.put(PlanetLabCredentials.ROLE, role);
		return authMap;
	}

	public String getRole() {
		return role;
	}

	public String getSlice() {
		return slice;
	}

	public int hashCode() {
		int code = 0;
		code += slice.hashCode();
		code += username.hashCode();
		code += password.hashCode();
		code += role.hashCode();

		return code;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("u:'" + this.getUsername() + "',");
		buf.append(" kf:'" + this.getKeyPath() + "',");
		return buf.toString();
	}

}
