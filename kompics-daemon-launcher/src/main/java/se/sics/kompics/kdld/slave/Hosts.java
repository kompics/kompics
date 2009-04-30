package se.sics.kompics.kdld.slave;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class Hosts implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4320958213919299696L;

	
	public static Hosts load(String hostsFile) {
		Hosts hosts = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					hostsFile));
			hosts = (Hosts) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return hosts;
	}
}
