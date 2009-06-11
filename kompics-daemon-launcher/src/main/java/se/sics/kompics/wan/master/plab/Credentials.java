package se.sics.kompics.wan.master.plab;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
public class Credentials {

	private final String password;

	private String passwordMD5;

	private final String username;

	private String usernameMD5;

	private final String slice;

	private final String keyPath;

	private final String keyFilePassword;

	private final String role;

	private boolean ignoreCerificateErrors = false;

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

	public Credentials(Map<String, String> credentials) {

		this.username = (String) credentials.get("Username");
		this.password = (String) credentials.get("AuthString");
		this.slice = (String) credentials.get("Slice");
		this.role = (String) credentials.get("Role");
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

	 private void calcMD5() {
		 this.usernameMD5 = calcSunMD5(this.username);
		 this.passwordMD5 = calcSunMD5(this.username + this.password);
	// MD5 md5 = new MD5();
	// // use the default encoding
	// md5.Init();
	// md5.Update(username.getBytes());
	// this.usernameMD5 = md5.asHex();
	//
	// md5.Init();
	// md5.Update((username + password).getBytes());
	// this.passwordMD5 = md5.asHex();
	 }

	 private String calcSunMD5(String str) {
	 	return DigestUtils.md5Hex(str);
	 }


	private String calcSunMD5_old(String str) {
		byte[] defaultBytes = str.getBytes();
		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte[] messageDigest = algorithm.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String hex = Integer.toHexString(0xFF & messageDigest[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			// *** Testausgabe
			System.out.println("string: " + str + "   md5 version is "
					+ hexString.toString());
			return hexString.toString();
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println("Problem: " + nsae.getMessage());
			return null;
		}
	}

	public Credentials(String username, String password, String slice,
			String role, String keyPath, String keyFilePassword) {

		this.password = password;
		this.username = username;
		this.slice = slice;
		this.role = role;
		this.keyPath = keyPath;
		this.keyFilePassword = keyFilePassword;

		this.calcMD5();
	}

	/**
	 * Checks username and password
	 * 
	 * @param auth
	 * @return true if username and password matched
	 */
	public boolean authenticate(Credentials comp) {
		if (this.authenticateMD5(comp)) {
			return true;
		}

		if (this.authenticateClearTest(comp)) {
			return true;
		}

		return false;
	}

	public boolean authenticateClearTest(Credentials comp) {

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

	public boolean authenticateMD5(Credentials comp) {
		if (!usernameMD5.equals(comp.getUsername())) {
			return false;
		}

		if (!passwordMD5.equals(comp.getPassword())) {
			return false;
		}

		// System.out.println("Authenticated with md5");
		return true;
	}

	public boolean equals(Credentials comp) {
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

	public boolean equals(Map authMap) {
		Credentials c = new Credentials(authMap);
		return this.equals(c);

	}

	public boolean equals(Object obj) {
		if (obj instanceof Credentials) {
			Credentials comp = (Credentials) obj;
			return this.equals(comp);
		}
		return false;
	}

	public Map getAuthMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put("Username", usernameMD5);
		authMap.put("AuthString", passwordMD5);
		// authMap.put("Slice", slice);
		// authMap.put("Role", role);
		// authMap.put("PrivateKeyFile", keyPath);
		// authMap.put("PrivateKeyPassword", keyFilePassword);
		return authMap;
	}

	public String getKeyFilePassword() {
		return keyFilePassword;
	}

	/**
	 * returns the full path to the private key used from ssh authentication
	 * 
	 * @return
	 */
	public String getKeyPath() {
		return keyPath;
	}

	public String getPassword() {
		return password;
	}

	public Map getPLCMap() {
		HashMap<String, String> authMap = new HashMap<String, String>();
		authMap.put("Username", username);
		authMap.put("AuthMethod", "password");
		authMap.put("AuthString", password);
		authMap.put("Slice", slice);
		authMap.put("Role", role);
		return authMap;
	}

	public boolean isIgnoreCerificateErrors() {
		return ignoreCerificateErrors;
	}

	public String getRole() {
		return role;
	}

	public String getSlice() {
		return slice;
	}

	public String getUsername() {
		return username;
	}

	public int hashCode() {
		int code = 0;
		code += username.hashCode();
		code += password.hashCode();
		// code += slice.hashCode();
		// code += role.hashCode();

		return code;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("u:'" + this.getUsername() + "',");
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
