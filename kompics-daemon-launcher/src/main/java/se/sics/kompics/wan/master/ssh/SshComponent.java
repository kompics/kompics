package se.sics.kompics.wan.master.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.ExperimentHost;
import se.sics.kompics.wan.master.plab.rpc.RpcFunctions;
import se.sics.kompics.wan.master.scp.download.DownloadManager;
import se.sics.kompics.wan.master.scp.upload.UploadManager;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.HTTPProxyData;
import ch.ethz.ssh2.Session;

public class SshComponent extends ComponentDefinition {

	public static final int LOG_ERROR = 1;

	public static final int LOG_FULL = 3;

	public static final int LOG_DEVEL = 2;

	public static final int LOG_LEVEL = 3;

	public static final int SSH_CONNECT_TIMEOUT = 15000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 30000;

	public static final String EXIT_CODE_IDENTIFIER = "=:=:=EXIT STATUS==";

	private Negative<SshPort> sshPort;

	// (session, status)
	private Map<Session, SshConn> activeSshConnections = new HashMap<Session, SshConn>();

	// private String status = "disconnected";

	private Map<Session, CommandSpec> commandStatus = new HashMap<Session, CommandSpec>();

	private AtomicBoolean quit = new AtomicBoolean(false);

	public class SshConn implements ConnectionMonitor, Comparable<SshConn> {
		private String status;

		private final ExperimentHost hostname;

		private boolean isConnected = false;

		private boolean wasConnected = false;

		private final Credentials credentials;

		private Connection connection;

		public SshConn(ExperimentHost host, Credentials credentials,
				Connection connection) {
			super();
			this.status = "created";
			this.hostname = host;
			this.credentials = credentials;
			this.connection = connection;

			if (connection.isAuthenticationComplete() == true) {
				isConnected = true;
			}
		}

		public void connectionLost(Throwable reason) {
			// statusChange("connection lost, (HANDLE THIS?)", LOG_ERROR);

			isConnected = false;
		}

		/**
		 * @return the credentials
		 */
		public Credentials getCredentials() {
			return credentials;
		}

		/**
		 * @return the plHost
		 */
		public ExperimentHost getHostname() {
			return hostname;
		}

		/**
		 * @return the status
		 */
		public String getStatus() {
			return status;
		}

		/**
		 * @return the isConnected
		 */
		public boolean isConnected() {
			return isConnected;
		}

		/**
		 * @return the wasConnected
		 */
		public boolean isWasConnected() {
			return wasConnected;
		}

		/**
		 * @return the connection
		 */
		public Connection getConnection() {
			return connection;
		}

		/**
		 * @param isConnected
		 *            the isConnected to set
		 */
		public void setConnected(boolean isConnected) {
			this.isConnected = isConnected;
		}

		/**
		 * @param wasConnected
		 *            the wasConnected to set
		 */
		public void setWasConnected(boolean wasConnected) {
			this.wasConnected = wasConnected;
		}

		/**
		 * @param status
		 *            the status to set
		 */
		public void setStatus(String status) {
			this.status = status;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(SshConn that) {
			// we can have several connections to the same host with different
			// usernames
			if ((!this.credentials.equals(that.credentials))
					|| (this.hostname.compareTo(that.hostname) != 0)) {
				return -1;
			}

			if (new ConnectionComparator().compare(this.connection,
					that.connection) != 0) {
				return -1;
			}

			return 0;

		}
	}

	private class LineReader {
		InputStream inputStream;

		StringBuffer buf;

		public LineReader(InputStream inputStream) {
			this.inputStream = inputStream;
			this.buf = new StringBuffer();
		}

		public String readLine() throws IOException {

			// System.out.println(b)
			int available = inputStream.available();
			if (available > 0) {
				byte[] byteBuffer = new byte[1];
				while (inputStream.read(byteBuffer, 0, 1) > 0) {
					String str = new String(byteBuffer);
					if (str.equals("\n") || str.equals("\r")) {
						if (buf.length() > 0) {
							String ret = buf.toString();
							buf = new StringBuffer();
							return ret;
						} else {
							continue;
						}
					}
					buf.append(str);
				}
			}
			return null;

		}

		public String readRest() throws IOException {
			int available = inputStream.available();
			if (available > 0) {
				byte[] byteBuffer = new byte[available];
				inputStream.read(byteBuffer, 0, available);
				buf.append(new String(byteBuffer));
				return buf.toString();
			}
			return "";
		}

	}

	public SshComponent() {

		subscribe(handleSshCommand, sshPort);
		subscribe(handleSshConnectRequest, sshPort);
	}

	public Handler<InitSsh> handleInitSsh = new Handler<InitSsh>() {
		public void handle(InitSsh event) {

		}
	};

	public Handler<SshCommand> handleSshCommand = new Handler<SshCommand>() {
		public void handle(SshCommand event) {
			CommandSpec commandSpec = new CommandSpec(event.getCommand(), event
					.getTimeout(), commandStatus.size(), event.isStopOnError());

			commandStatus.put(event.getSession(), commandSpec);

			String command = commandSpec.getCommand();
			boolean commandFailed = false;

			if (command.startsWith("#")) {
				runSpecialCommand(commandSpec);
			}// run the command in the current session
			else {
				try {
					commandFailed = runCommand(commandSpec, event.getSession(),
							activeSshConnections.get(event.getSession())) < 0;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// send commandFailed

		}
	};

	public Handler<SshConnectRequest> handleSshConnectRequest = new Handler<SshConnectRequest>() {
		public void handle(SshConnectRequest event) {

			Session session = connect(event.getCredentials(), event.getHostname(), 
					new CommandSpec("#connect", SSH_CONNECT_TIMEOUT,
							commandStatus.size(), true));

			// if connection request succeeds
			commandStatus.put(session, new CommandSpec("#connect",
					SSH_CONNECT_TIMEOUT, commandStatus.size(), true));

			// trigger SshConnectResponse
		}
	};

	public int runCommand(CommandSpec commandSpec, Session session,
			SshConn sshConn) throws IOException, InterruptedException {

		LineReader stdout = new LineReader(session.getStdout());
		LineReader stderr = new LineReader(session.getStderr());
		OutputStream stdin = session.getStdin();

		this.statusChange(sshConn, "executing: '" + commandSpec.getCommand()
				+ "'", LOG_FULL);
		stdin.write(commandSpec.getCommand().getBytes());
		stdin.write(("\necho \"" + EXIT_CODE_IDENTIFIER + "$?\"\n").getBytes());
		commandSpec.started();

		String line;
		String errLine;

		do {

			// session.waitForCondition(ChannelCondition.STDOUT_DATA
			// | ChannelCondition.STDERR_DATA, Math
			// .round(1000.0 / DATA_POLLING_FREQ));
			Thread.sleep(50);
			line = stdout.readLine();
			errLine = stderr.readLine();

			// check if we got any data on stderr
			while (errLine != null) {
				commandSpec.receivedErr(errLine);
				errLine = stderr.readLine();
			}
			// check for data on stdout
			while (line != null) {
				if (line.startsWith(EXIT_CODE_IDENTIFIER)) {
					String[] split = line.split("==");
					commandSpec.setExitCode(Integer.parseInt(split[1]));
					this.statusChange(sshConn, commandSpec.getCommand()
							+ " completed, code=" + commandSpec.getExitCode()
							+ " time=" + commandSpec.getExecutionTime()
							/ 1000.0, LOG_FULL);
					return commandSpec.getExitCode();
				}
				commandSpec.receivedData(line);

				// System.out.println(line);
				line = stdout.readLine();
			}

			if (commandSpec.isTimedOut()) {
				commandSpec.setExitCode(CommandSpec.RETURN_TIMEDOUT,
						"timed out");
				commandSpec
						.recievedControllErr("timeout after "
								+ (Math
										.round(commandSpec.getExecutionTime() * 10.0) / 10.0)
								+ " s");
				if (commandSpec.isStopOnError()) {
					commandSpec
							.recievedControllErr("command is stop on error, halting");
				}
				return commandSpec.getExitCode();
			}

			// handle the case when the command is killed
			if (commandSpec.isKilled()) {
				commandSpec.setExitCode(CommandSpec.RETURN_KILLED, "killed");
				commandSpec
						.recievedControllErr("killed after "
								+ (Math
										.round(commandSpec.getExecutionTime() * 10.0) / 10.0)
								+ " s");
				if (commandSpec.isStopOnError()) {
					commandSpec
							.recievedControllErr("command is stop on error, halting");
				}
				return commandSpec.getExitCode();
			}

			if (line == null) {
				line = "";
			}

		} while (!line.startsWith(EXIT_CODE_IDENTIFIER)
				&& (quit.get() == false));

		// we should never make it down here... unless quiting
		return Integer.MIN_VALUE;
	}

	private void runSpecialCommand(CommandSpec commandSpec) {
		// handle these in a special way...
		String[] command = this.parseParameters(commandSpec.getCommand());
		if (command.length > 0) {
			if (command[0].equals(RpcFunctions.SPECIAL_COMMAND_UPLOAD_DIR)) {
				if (command.length == 2) {
					File fileOrDir = new File(command[1]);
					if (fileOrDir.exists()) {
						this.upload(fileOrDir, commandSpec);
					}
				}
			} else if (command[0]
					.startsWith(RpcFunctions.SPECIAL_COMMAND_DOWNLOAD_DIR)) {
				if (command.length == 5) {
					String remotePath = command[1];
					File localFileOrDir = new File(command[2]);
					String fileFilter = command[3];
					String localNameType = command[4];
					if (localFileOrDir.exists()) {
						this.download(remotePath, localFileOrDir, fileFilter,
								localNameType, commandSpec);
					}
				} else {
					System.err.println("parse error '"
							+ commandSpec.getCommand() + "'" + "length="
							+ command.length);
				}
			} else {
				System.err.println("unknown command '" + command[0] + "'");
			}
		} else {
			System.out.println("parameter parsing problem: '"
					+ commandSpec.getCommand() + "'");
		}
	}

	public boolean upload(File fileOrDir, CommandSpec commandSpec) {
		return UploadManager.getInstance().uploadDir(this, fileOrDir,
				commandSpec);
	}

	public boolean download(String remotePath, File localBaseDir,
			String fileFilter, String localNamingType, CommandSpec commandSpec) {
		// sanity checks
		if (fileFilter == null || fileFilter.length() == 0) {
			// match everything
			fileFilter = ".";
		}
		DownloadManager dManager = DownloadManager.getInstance();
		dManager.setLocalFilenameType(localNamingType);

		return dManager.downloadDir(this, remotePath, localBaseDir, fileFilter,
				commandSpec);
	}

	private String[] parseParameters(String parameters) {
		String[] split = new String[0];
		if (parameters.contains("\"") && parameters.contains("'")) {
			System.err
					.println("sorry... arguments can only contain either \" or ', not both");
			return split;
		}

		if (parameters.contains("\"") || parameters.contains("'")) {
			// handle specially

			ArrayList<String> params = new ArrayList<String>();
			boolean withinQuotes = false;
			StringBuffer tmpBuffer = new StringBuffer();
			for (int i = 0; i < parameters.length(); i++) {

				char c = parameters.charAt(i);
				// System.out.println("processing: " + c);
				if (c == '"' || c == '\'') {
					withinQuotes = !withinQuotes;
					// System.out.println("w=" + withinQuotes);
					// continue to the next character
				} else {
					if (c == ' ' && !withinQuotes) {
						// we reached a space, and we are not between quoutes,
						// add to list and flush buffer
						params.add(tmpBuffer.toString());

						// System.out.println("found: " + tmpBuffer.toString()
						// + "(" + params.size() + ")");
						tmpBuffer = new StringBuffer();
					} else {
						// if the char is not ' ' or '"' or '\'', append to
						// stringbuffer

						tmpBuffer.append(c);
						// System.out.println("adding: " +
						// tmpBuffer.toString());
					}
				}
			}
			if (tmpBuffer.length() > 0) {
				params.add(tmpBuffer.toString());
			}
			split = params.toArray(split);
		} else {
			split = parameters.split(" ");
		}

		return split;
	}

	private Session connect(Credentials credentials, ExperimentHost expHost,
			CommandSpec commandSpec) {

		Connection connection = new Connection(expHost.getHostname());

		SshConn sshConnection = new SshConn(expHost, credentials, connection);

		// SshConn sshConnection = activeSshConnections.get(session);

		List<SshConn> listActiveConns = new ArrayList<SshConn>(
				activeSshConnections.values());

		if (listActiveConns.contains(sshConnection) == true) {
			Set<Session> sessions = activeSshConnections.keySet();
			for (Session s : sessions) {
				if (activeSshConnections.get(s).compareTo(sshConnection) == 0) {
					return s;
				}
			}
			throw new IllegalStateException("Shouldn't get this far.");
		}

		commandSpec.started();

		sshConnection = new SshConn(expHost, credentials, connection);

		connection.addConnectionMonitor(sshConnection);

		// if (Main.getConfig(Constants.HTTP_PROXY_HOST) != null
		// && Main.getConfig(Constants.HTTP_PROXY_PORT) != null) {

		if (PlanetLabConfiguration.getHttpProxyHost().compareTo(
				PlanetLabConfiguration.DEFAULT_HTTP_PROXY_HOST) != 0
				&& PlanetLabConfiguration.getHttpProxyPort() != PlanetLabConfiguration.DEFAULT_HTTP_PROXY_PORT) {
			int port = PlanetLabConfiguration.getHttpProxyPort();
			String hostname = PlanetLabConfiguration.getHttpProxyHost();
			String username = PlanetLabConfiguration.getHttpProxyUsername();
			String password = PlanetLabConfiguration.getHttpProxyPassword();
			// if username AND password is specified
			if (username != PlanetLabConfiguration.DEFAULT_HTTP_PROXY_USERNAME
					&& password != PlanetLabConfiguration.DEFAULT_HTTP_PROXY_PASSWORD) {
				connection.setProxyData(new HTTPProxyData(hostname, port,
						username, password));
				System.out
						.println("ssh connect with http proxy and auth, host="
								+ hostname + " port=" + port + "user="
								+ username);
			} else {
				// ok, only hostname and port
				connection.setProxyData(new HTTPProxyData(
						PlanetLabConfiguration.getHttpProxyHost(), port));
				System.out.println("ssh connect with http proxy, host="
						+ hostname + " port=" + port);
			}
		}
		// try to open the connection

		try {
			// try to connect
			connection.connect(null, SSH_CONNECT_TIMEOUT,
					SSH_KEY_EXCHANGE_TIMEOUT);

			// try to authenticate
			// if (sshConn.authenticateWithPublicKey(controller
			// .getCredentials().getSlice(), new File(controller
			// .getCredentials().getKeyPath()), controller
			// .getCredentials().getKeyFilePassword())) {

			if (connection.authenticateWithPublicKey(credentials.getUsername(),
					new File(credentials.getKeyPath()), credentials
							.getKeyFilePassword())) {

				// ok, authentiaction succesfull, return the connection
				commandSpec.recievedControllData("connect successful");
				// isConnected = true;
				sshConnection.setConnected(true);
				
				Session session = startShell(sshConnection);
				
				activeSshConnections.put(session, sshConnection);
				
				return session;

			} else {
				// well, authentication failed
				statusChange(sshConnection, "auth failed", LOG_DEVEL);
				commandSpec.setExitCode(1, "auth failed");
				commandSpec.recievedControllErr("auth failed");
			}

			// handle errors...
		} catch (SocketTimeoutException e) {
			this.statusChange(sshConnection, "connection timeout: "
					+ e.getMessage(), LOG_DEVEL);
			if (e.getMessage().contains("kex")) {
				commandSpec.setExitCode(4, "kex timeout");
			} else {
				commandSpec.setExitCode(3, "conn timeout");
			}
			commandSpec.recievedControllErr(e.getMessage());
		} catch (IOException e) {

			if (e.getCause() != null) {
				commandSpec.recievedControllErr(e.getCause().getMessage());
				if (e.getCause().getMessage().contains("Connection reset")) {
					statusChange(sshConnection, e.getCause().getMessage(),
							LOG_DEVEL);
					commandSpec.setExitCode(2, "conn reset");

				} else if (e.getCause().getMessage().contains(
						"Connection refused")) {
					statusChange(sshConnection, e.getCause().getMessage(),
							LOG_DEVEL);
					commandSpec.setExitCode(2, "conn refused");

				} else if (e.getCause().getMessage().contains(
						"Premature connection close")) {
					statusChange(sshConnection, e.getCause().getMessage(),
							LOG_DEVEL);
					commandSpec.setExitCode(2, "prem close");

				} else if (e.getCause() instanceof java.net.UnknownHostException) {
					statusChange(sshConnection, e.getCause().getMessage(),
							LOG_DEVEL);
					commandSpec.setExitCode(2, "dns unknown");

				} else if (e.getCause() instanceof NoRouteToHostException) {
					statusChange(sshConnection, e.getCause().getMessage(),
							LOG_DEVEL);
					commandSpec.setExitCode(2, "no route");
				} else if (e.getMessage().contains("Publickey")) {
					statusChange(sshConnection, e.getMessage(), LOG_DEVEL);
					commandSpec.setExitCode(2, "auth error");
				} else {
					System.err.println("NEW EXCEPTION TYPE, handle...");

					e.printStackTrace();
				}
			} else {
				commandSpec.receivedErr(e.getMessage());
				commandSpec.setExitCode(255, "other");

				statusChange(sshConnection, e.getMessage(), LOG_DEVEL);
			}

		}

		return null;

	}
	
	public Session startShell(SshConn conn) {
		Session session = null;
		if (conn.isConnected()) {
			try {
				session = conn.getConnection().openSession();
			} catch (IOException e) {
				statusChange(conn, "could not open session: " + e.getMessage(),
						LOG_ERROR);
				return null;
			}

			try {
				session.startShell();
			} catch (IOException e) {
				statusChange(conn, "could not start shell: " + e.getMessage(),
						LOG_ERROR);
				return null;
			}
		}
		return session;
	}

	private void statusChange(SshConn connection, String status, int level) {

		connection.setStatus(status);
		if (level <= LOG_LEVEL) {
			System.out.println(connection.getHostname() + ": " + status);
		}
	}
}
