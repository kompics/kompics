package se.sics.kompics.wan.master.ssh;

import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.ExperimentHost;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;

public class SshConnData implements ConnectionMonitor, Comparable<SshConnData> {



	private String status;
	private final ExperimentHost hostname;
	private boolean isConnected;
	private boolean wasConnected;
	private final Credentials credentials;
	private Connection connection;
	
	
	public SshConnData(ExperimentHost hostname, Credentials credentials,
			Connection connection) {
		super();
		this.hostname = hostname;
		this.credentials = credentials;
		this.connection = connection;
		
		this.status = "created";

		if (this.connection.isAuthenticationComplete() == true) {
			isConnected = true;
		}
		wasConnected = false;
	}


	@Override
	public void connectionLost(Throwable reason) {
		// statusChange("connection lost, (HANDLE THIS?)", LOG_ERROR);
		
		if (isConnected == true) {
			wasConnected = true;
		}

		isConnected = false;
	}

	


	@Override
	public int compareTo(SshConnData that) {
		// we can have several connections to the same host with different
		// usernames
		if ((!credentials.equals(that.credentials))
				|| (hostname.compareTo(that.hostname) != 0)) {
			return -1;
		}

		if (new ConnectionComparator().compare(connection,
				that.connection) != 0) {
			return -1;
		}

		return 0;

	}

	
	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}


	public boolean isConnected() {
		return isConnected;
	}


	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}


	public boolean isWasConnected() {
		return wasConnected;
	}


	public void setWasConnected(boolean wasConnected) {
		this.wasConnected = wasConnected;
	}


	public ExperimentHost getHostname() {
		return hostname;
	}


	public Credentials getCredentials() {
		return credentials;
	}


	public Connection getConnection() {
		return connection;
	}



	
}