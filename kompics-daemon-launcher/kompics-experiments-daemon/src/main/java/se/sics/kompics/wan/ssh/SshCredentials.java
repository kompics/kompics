package se.sics.kompics.wan.ssh;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;


/**
 * Class holding credentials used both for communication with PLC and for
 * authentication with planetlab hosts.
 * 
 * @author isdal
 * 
 */
public class SshCredentials implements Credentials {

	protected final String password;

	protected String passwordMD5;

	protected final String loginName;

	protected String usernameMD5;

	protected final String keyPath;

	protected final String keyFilePassword;

	protected boolean ignoreCerificateErrors = true;

	/**
	 * Creates a new Config class from a Map containing the following fields:
	 * 
	 * @param Map
	 *            with credentials used for authentication
	 * 
	 *            credentials{"Username"} The username
	 * 
	 *            credentials{"AuthMethod"} This should always be "password",
	 *            --to keep it PLC compadable...
	 * 
	 *            credentials{"AuthString"} The accutual password
	 * 
	 *            credentials{"Slice"} the name of the slice (this is only
	 *            needed to be able to know what login-name to use for ssh
	 *            conenctions
	 * 
	 *            credentials{"Role"} this is needed for PLC communication
	 */

	public SshCredentials(Map<String, String> credentials) {

		this.loginName = (String) credentials.get("Username");
		this.password = (String) credentials.get("AuthString");
		this.keyPath = (String) credentials.get("PrivateKeyFile");
		this.keyFilePassword = (String) credentials.get("PrivateKeyPassword");
		this.ignoreCerificateErrors = this
				.parseBooleanString((String) credentials
						.get("IgnoreCertificateErrors"));

		this.calcMD5();

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

	public SshCredentials(String username, String password,
			String keyPath, String keyFilePassword) {

		this.password = password;
		this.loginName = username;
		this.keyPath = keyPath;
		this.keyFilePassword = keyFilePassword;
		
		this.calcMD5();
	}
	

	 private void calcMD5() {
		 this.usernameMD5 = calcSunMD5(this.loginName);
		 this.passwordMD5 = calcSunMD5(this.loginName + this.password);
	 }

	 private String calcSunMD5(String str) {
	 	return DigestUtils.md5Hex(str);
	 }



	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#authenticate(se.sics.kompics.wan.ssh.Cred)
	 */
	 public int compareTo(Credentials comp) {
		if (this.authenticateMD5(comp)) {
			return 0;
		}

		if (this.authenticateClearText(comp)) {
			return 0;
		}

		return -1;
	}

	public boolean authenticateClearText(Credentials comp) {

		if (!loginName.equals(comp.getSshLoginName())) {
			return false;
		}
		if (!password.equals(comp.getSshPassword())) {
			return false;
		}
		System.err.println("Authenticated with Clear Text: "
				+ comp.getSshLoginName());
		return true;
	}

	public boolean authenticateMD5(Credentials comp) {
		if (!usernameMD5.equals(comp.getSshLoginName())) {
			return false;
		}

		if (!passwordMD5.equals(comp.getSshPassword())) {
			return false;
		}

		 System.out.println("Authenticated with md5");
		return true;
	}

	public boolean equals(Credentials comp) {
		if (!loginName.equals(comp.getSshLoginName())) {
			return false;
		}
		if (!password.equals(comp.getSshPassword())) {
			return false;
		}
		return true;
	}

	public boolean equals(Map<String, String> authMap) {
		Credentials c = new SshCredentials(authMap);
		return this.equals(c);

	}

	public boolean equals(Object obj) {
		if (obj instanceof Credentials) {
			Credentials comp = (Credentials) obj;
			return this.equals(comp);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#getAuthMap()
	 */
	public Map<String, String> getAuthMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put("Username", loginName); 
		authMap.put("AuthString", password); 
		 authMap.put("PrivateKeyFile", keyPath);
		 authMap.put("PrivateKeyPassword", keyFilePassword);
		return authMap;
	}
	
	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#getPlanetLabAuthMap()
	 */
	public Map<String, String> getPlanetLabAuthMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put("Username", loginName);
		authMap.put("AuthMethod", "password");
		authMap.put("AuthString", password);
		return authMap;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#getKeyFilePassword()
	 */
	public String getKeyFilePassword() {
		return keyFilePassword;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#getKeyPath()
	 */
	public String getKeyPath() {
		return keyPath;
	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#getPassword()
	 */
	public String getSshPassword() {
		return password;
	}



	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#isIgnoreCerificateErrors()
	 */
	public boolean isIgnoreCerificateErrors() {
		return ignoreCerificateErrors;
	}


	/* (non-Javadoc)
	 * @see se.sics.kompics.wan.ssh.Cred#getUsername()
	 */
	public String getSshLoginName() {
		return loginName;
	}

	public int hashCode() {
		int code = 0;
		code += loginName.hashCode();
		code += password.hashCode();

		return code;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("u:'" + this.getSshLoginName() + "',");
		buf.append(" kf:'" + this.getKeyPath() + "',");
		return buf.toString();
	}

	public String getPasswordMD5() {
		return passwordMD5;
	}

	public String getUsernameMD5() {
		return usernameMD5;
	}
	
}
