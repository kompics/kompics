package se.sics.kompics.wan.plab;

import java.util.HashMap;
import java.util.Map;

import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.ssh.Credentials;

/**
 * Class holding credentials used both for communication with PLC and for
 * authentication with planetlab hosts.
 * 
 * @author isdal
 * 
 */
public class PlanetLabCredentials implements Credentials {

	public static final String USERNAME = "Username";
	public static final String AUTHSTRING = "AuthString";
	public static final String AUTHMETHOD = "AuthMethod";
	public static final String SLICE = "Slice";
	public static final String ROLE = "Role";
	public static final String PRIVATE_KEY_FILE = "PrivateKeyFile";
	public static final String PRIVATE_KEY_PASSWORD = "PrivateKeyPassword";
	public static final String IGNORE_CERT_ERRORS = "IgnoreCertificateErrors";

	protected final String password;

	protected final String username;

	protected final String keyPath;

	protected final String keyFilePassword;

	protected boolean ignoreCerificateErrors = true;

	private final String slice;

	private final String role;

	private final String authMethod = "password";

	public PlanetLabCredentials(Map<String, String> credentials) {
		this.username = (String) credentials.get(PlanetLabCredentials.USERNAME);
		this.password = (String) credentials.get(PlanetLabCredentials.AUTHSTRING);
		this.keyPath = (String) credentials.get(PlanetLabCredentials.PRIVATE_KEY_FILE);
		this.keyFilePassword = (String) credentials.get(PlanetLabCredentials.PRIVATE_KEY_PASSWORD);
		this.ignoreCerificateErrors = this.parseBooleanString((String) credentials
				.get(PlanetLabCredentials.IGNORE_CERT_ERRORS));
		this.slice = (String) credentials.get(PlanetLabCredentials.SLICE);
		this.role = (String) credentials.get(PlanetLabCredentials.ROLE);
	}

	public PlanetLabCredentials(String username, String password, String slice, String role,
			String keyPath, String keyFilePassword) {
		this.username = username;
		this.password = password;
		this.keyPath = keyPath;
		this.keyFilePassword = keyFilePassword;
		this.slice = slice;
		this.role = role;
	}

	public PlanetLabCredentials(String username, String password, String slice, String keyPath,
			String keyFilePassword) {
		this.username = username;
		this.password = password;
		this.keyPath = keyPath;
		this.keyFilePassword = keyFilePassword;
		this.slice = slice;
		this.role = PlanetLabConfiguration.DEFAULT_PL_ROLE;
	}


	public boolean equals(PlanetLabCredentials comp) {
		if (!username.equals(comp.getUsername())) {
			return false;
		}
		if (!slice.equals(comp.getSlice())) {
			return false;
		}

		if (!password.equals(comp.getSshPassword())) {
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

	@Override
	public Map<String, String> getAuthMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put(PlanetLabCredentials.USERNAME, username);
		authMap.put(PlanetLabCredentials.AUTHSTRING, password);
		authMap.put(PlanetLabCredentials.SLICE, slice);
		authMap.put(PlanetLabCredentials.ROLE, role);
		authMap.put(PlanetLabCredentials.PRIVATE_KEY_FILE, keyPath);
		authMap.put(PlanetLabCredentials.PRIVATE_KEY_PASSWORD, keyFilePassword);
		authMap.put(PlanetLabCredentials.AUTHMETHOD, authMethod);
		return authMap;
	}

	@Override
	public Map<String, String> getPlanetLabAuthMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put(PlanetLabCredentials.USERNAME, username);
		authMap.put(PlanetLabCredentials.AUTHMETHOD, "password");
		authMap.put(PlanetLabCredentials.AUTHSTRING, password);
		authMap.put(PlanetLabCredentials.SLICE, slice);
		authMap.put(PlanetLabCredentials.ROLE, role);
		return authMap;
	}

	public String getRole() {
		return this.role;
	}

	public String getSlice() {
		return this.slice;
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
		buf.append("u:'" + this.getSshLoginName() + "',");
		buf.append(" kf:'" + this.getKeyPath() + "',");
		return buf.toString();
	}

	private boolean parseBooleanString(String str) {
		if (str != null) {
			str = str.toLowerCase();
			if (str.equals("1")) {
				return true;
			} else if (str.equals("true")) {
				return true;
			} else if (str.equals("yes")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getKeyFilePassword() {
		return this.keyFilePassword;
	}

	@Override
	public String getKeyPath() {
		return this.keyPath;
	}

	@Override
	public String getSshLoginName() {
		return this.slice;
	}

	@Override
	public String getSshPassword() {
		return "";
	}

	@Override
	public boolean isIgnoreCerificateErrors() {
		return this.ignoreCerificateErrors;
	}

	@Override
	public int compareTo(Credentials comp) {

		if (this.authenticateClearText(comp)) {
			return 0;
		}

		return -1;
	}

	public boolean authenticateClearText(Credentials comp) {

		if (!slice.equals(comp.getSshLoginName())) {
			return false;
		}
		if (!password.equals(comp.getSshPassword())) {
			return false;
		}
		System.err.println("Authenticated with Clear Text: " + comp.getSshLoginName());
		return true;
	}

	public String getPassword() {
		return password;
	}

	public String getUsername() {
		return username;
	}

	public String getAuthMethod() {
		return authMethod;
	}

	public void setIgnoreCerificateErrors(boolean ignoreCerificateErrors) {
		this.ignoreCerificateErrors = ignoreCerificateErrors;
	}
}
