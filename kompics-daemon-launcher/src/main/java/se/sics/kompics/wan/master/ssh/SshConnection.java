package se.sics.kompics.wan.master.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.configuration.ConfigurationException;

import se.sics.kompics.wan.config.Configuration;
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


public class SshConnection extends Thread implements ConnectionMonitor {

	// private static final String SPECIAL_COMMAND_ADD_RANDOM = "#addRandom";

	public static final int LOG_ERROR = 1;

	public static final int LOG_FULL = 3;

	public static final int LOG_DEVEL = 2;

	public static final int LOG_LEVEL = 3;

	public static final int SSH_CONNECT_TIMEOUT = 15000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 30000;

	public static final String EXIT_CODE_IDENTIFIER = "=:=:=EXIT STATUS==";

	private ExperimentHost plHost;

	private LinkedBlockingQueue<CommandSpec> commandQueue = new LinkedBlockingQueue<CommandSpec>();

	private Vector<CommandSpec> commandStatus = new Vector<CommandSpec>();

	private String status = "disconnected";

	private volatile boolean isConnected = false;

	private volatile boolean wasConnected = false;

	private volatile boolean quit = false;

	private volatile int currentCommand = 0;

//	private ConnectionController controller;
	
	private Credentials credentials;

	private Connection sshConn;

	private volatile boolean forceRun = false;

	private volatile boolean halted = false;

	/**
	 * used for debugging...
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			Configuration.init(args, PlanetLabConfiguration.class);
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// TODO Auto-generated method stub
		Credentials cred = new Credentials("jdowling", "password", "/home/jdowling/.ssh/id_rsa", "none");
		SshConnection conn = new SshConnection(cred, new ExperimentHost("lqist.com"));
		Thread t = new Thread(conn);
		t.start();

		try {
			conn.queueCommand("ls -la");
			Thread.sleep(2000);

			// conn.queueCommand("sleep 10");

			conn.queueCommand("ls -larth");
			Thread.sleep(2000);
			// use 'session' object to identify session to halt
			conn.halt();
			t.join();
			// System.out.println(conn.getOutput());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * create a new instance of a SSHConnection Thread
	 * 
	 * @param hostname
	 *            hostname to connect to
	 */
	public SshConnection(Credentials cred, ExperimentHost hostname) {
		this.setName("sshconn: " + hostname.getHostname());
//		this.controller = controller;
		this.plHost = hostname;
		this.credentials = cred;
		
		commandStatus.add(0, new CommandSpec("#connect", SSH_CONNECT_TIMEOUT,
				commandStatus.size(), true));

	}

	public void connectionLost(Throwable reason) {
		// statusChange("connection lost, (HANDLE THIS?)", LOG_ERROR);

		isConnected = false;
	}

	public void disconnect() {
		if (sshConn != null) {
			sshConn.close();
		}
		isConnected = false;

		statusChange("disconnecting", LOG_DEVEL);
	}

	public String getCommand(int num) {
		return commandStatus.get(num).getCommand();
	}

	public int getCommandExitStatus(int commandIndex) {
		if (commandIndex == -1) {
			for (CommandSpec command : commandStatus) {
				if (command.getExitCode() != 0) {
					return 1;
				}
			}
		}
		return commandStatus.get(commandIndex).getExitCode();
	}

	public String getCommandExitStatusString(int commandIndex) {
		return commandStatus.get(commandIndex).getExitCodeString();
	}

	public CommandSpec getCommandSpec(int commandIndex) {
		if (commandIndex < 0 || commandIndex >= commandStatus.size()) {
			return null;
		}
		return commandStatus.get(commandIndex);
	}

	public Object[] getCommandStats() {
		Object[] ret = new Object[currentCommand + 1];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = commandStatus.get(i).getCommandStats();
			// System.out.println("exit: " +
			// commandStatus.get(i).getCommandStats().get("exit"));
		}
		return ret;
	}

	public Connection getConnection() {
		return sshConn;
	}

	public int getCurrentCommandNum() {
		return currentCommand;
	}

	public double getExecutionTime(int commandNum) {
		return commandStatus.get(commandNum).getExecutionTime();
	}

	public ExperimentHost getHost() {
		return this.plHost;
	}

	public String getHostname() {
		return plHost.getHostname();
	}

	public String getLastOutput() {
		for (int i = currentCommand; i >= 0; i--) {
			String lastLine = commandStatus.get(i).getLastLine();
			if (lastLine != null) {
				return lastLine;
			}
		}
		return "";
	}

	/**
	 * Get output from the ssh connection
	 * 
	 * @return a String with all data written to stdout and stderr by the remote
	 *         system
	 */
	public List<Map> getOutput(int fromRow) {
		List<Map> lines = new Vector<Map>();

		for (int i = 0; i < commandStatus.size(); i++) {
			CommandSpec command = commandStatus.get(i);
			if (command.isStarted()) {
				lines.addAll(command.getProcOutput(0));
			}
		}
		lines = lines.subList(fromRow, lines.size());
		return lines;
	}

	public int getOutputRowNum(int command) {
		return commandStatus.get(command).getLineNum();
	}

	public String getStatus() {
		return this.status;
	}

	/**
	 * Closes the connction and removes all commands from the execution queue
	 * 
	 */

	public void halt() {
		this.isConnected = false;
		commandQueue.clear();
		this.quit = true;
		this.interrupt();
		this.disconnect();
	}

	public boolean isCommandCompleted(int commandIndex) {
		if (commandIndex >= commandStatus.size()) {
			return false;
		}
		return commandStatus.get(commandIndex).isCompleted();
	}

	public boolean isConnected() {
		return isConnected;
	}

	public boolean isHalted() {
		return halted;
	}

	public boolean mirrorDir(File baseDir) {
		if (baseDir.exists()) {
			this.queueCommand("mirror " + baseDir.getAbsolutePath(), 0, true);
		}
		return false;
	}

	/**
	 * Lazy wrapper for queueCommand, - identical to calling
	 * <code>queueCommand(command, 0,false)</code>.
	 * 
	 * @param command
	 *            The command to execute
	 * @return The command number (used to check exit status)
	 */
	public int queueCommand(String command) {
		return queueCommand(command, 0, false);
	}

	/**
	 * Add command to the queue of commands to execute
	 * 
	 * @param command
	 *            The command to execute
	 * @param timeout
	 *            Maximum time allowed for the command to execute without any
	 *            data beeing written to stdout. 0 disable
	 * @param stopOnError
	 *            controller if subsequent commands should be executed if a
	 *            non-zero exit code is returned
	 * @return The command number (used to check exit status)
	 */
	public int queueCommand(String command, double timeout, boolean stopOnError) {

		CommandSpec commandSpec = new CommandSpec(command, timeout,
				commandStatus.size(), stopOnError);
		commandStatus.add(commandSpec);
		commandQueue.add(commandSpec);

		return commandStatus.size() - 1;
	}

	public int killCommand(int commandId) {
		CommandSpec c = this.getCommandSpec(commandId);
		if (c != null) {
			c.kill();
			return 0;
		}
		return 1;
	}

	public void run() {
		this.setPriority(Thread.NORM_PRIORITY + 2);
		// only run if we were able to connect
		if (this.connect(commandStatus.get(0)) == false) {
			// tell the controller, there might be a disconnect policy
			 System.err.println("connect failed");
//			controller.handleFailedConnect(plHost);
			return;
		}
		isConnected = true;
		wasConnected = true;

		// Start a shell
		Session session = this.startShell();
		if (session == null) {
			commandStatus.get(0).setExitCode(-1337);
			return;
		}

		commandStatus.get(0).setExitCode(0);

		this.statusChange("Shell started", LOG_DEVEL);
		try {
			while (!quit) {
				try {
					boolean commandFailed = false;
					CommandSpec commandSpec;

					commandSpec = getNextFromQueue();
					String command = commandSpec.getCommand();
					if (command.startsWith("#")) {
						runSpecialCommand(commandSpec);

					}
					// run the command in the current session
					else {
						commandFailed = this.runCommand(commandSpec, session) < 0;
					}

					// check if the command exit with non zero exit code, and
					// the command is stopOnError

					while (commandSpec.isStopOnError()
							&& commandSpec.getExitCode() != 0 && !forceRun) {
						halted = true;
						Thread.sleep(1000);
					}
					forceRun = false;
					halted = false;
					if (commandFailed) {
						// hmm, command timed out, close the session to kill it,
						// and
						// start a new session
						this.statusChange(command + " failed", LOG_DEVEL);
						session.close();
						if (this.isConnected()) {
							session = this.startShell();
						}
					}

				} catch (InterruptedException e) {
				}

			}
		} catch (IOException e) {
			if (e.getMessage().contains("SSH channel closed")) {
				this.statusChange(e.getMessage(), LOG_ERROR);
				this.commandStatus.get(0).setExitCode(5, "disc after conn");

			}
		}

		session.close();
	}

	/**
	 * Run a command in the given session
	 * 
	 * @param commandSpec
	 * @param session
	 * @return exit code of the command, or CommandSpec.RETURN_KILLED if killed
	 *         or CommandSpec.RETURN_TIMEDOUT if timedout
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public int runCommand(CommandSpec commandSpec, Session session)
			throws IOException, InterruptedException {

		LineReader stdout = new LineReader(session.getStdout());
		LineReader stderr = new LineReader(session.getStderr());
		OutputStream stdin = session.getStdin();

		this.statusChange("executing: '" + commandSpec.getCommand() + "'",
				LOG_FULL);
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
					this.statusChange(commandSpec.getCommand()
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
						.receivedControlErr("timeout after "
								+ (Math
										.round(commandSpec.getExecutionTime() * 10.0) / 10.0)
								+ " s");
				if (commandSpec.isStopOnError()) {
					commandSpec
							.receivedControlErr("command is stop on error, halting");
				}
				return commandSpec.getExitCode();
			}

			// handle the case when the command is killed
			if (commandSpec.isKilled()) {
				commandSpec.setExitCode(CommandSpec.RETURN_KILLED, "killed");
				commandSpec
						.receivedControlErr("killed after "
								+ (Math
										.round(commandSpec.getExecutionTime() * 10.0) / 10.0)
								+ " s");
				if (commandSpec.isStopOnError()) {
					commandSpec
							.receivedControlErr("command is stop on error, halting");
				}
				return commandSpec.getExitCode();
			}

			if (line == null) {
				line = "";
			}

		} while (!line.startsWith(EXIT_CODE_IDENTIFIER) && !quit);

		// we should never make it down here... unless quiting
		return Integer.MIN_VALUE;
	}

	/**
	 * starts a shell using the connection
	 * 
	 * @return a session with a shell, or null if not connected or an error
	 *         occurs
	 */
	public Session startShell() {
		Session session = null;
		if (this.isConnected()) {
			try {
				session = sshConn.openSession();
			} catch (IOException e) {
				statusChange("could not open session: " + e.getMessage(),
						LOG_ERROR);
				return null;
			}

			try {
				session.startShell();
			} catch (IOException e) {
				statusChange("could not start shell: " + e.getMessage(),
						LOG_ERROR);
				return null;
			}
		}
		return session;
	}

	/**
	 * checks if this connection is or was successfully connected
	 * 
	 * @return true if the connection sequence was initiated and successful,
	 *         false otherwise
	 */
	public boolean wasConnected() {
		return wasConnected;
	}

	private boolean connect(CommandSpec commandSpec) {

		if (isConnected) {
			
			return isConnected;
		} else {
			commandSpec.started();
			sshConn = new Connection(plHost.getHostname());
			sshConn.addConnectionMonitor(this);

//			if (Main.getConfig(Constants.HTTP_PROXY_HOST) != null
//					&& Main.getConfig(Constants.HTTP_PROXY_PORT) != null) {
			
			if (PlanetLabConfiguration.getHttpProxyHost().compareTo(
					PlanetLabConfiguration.DEFAULT_HTTP_PROXY_HOST) != 0 && 
					PlanetLabConfiguration.getHttpProxyPort() != 
						PlanetLabConfiguration.DEFAULT_HTTP_PROXY_PORT)
			{
				int port = PlanetLabConfiguration.getHttpProxyPort();
				String hostname = PlanetLabConfiguration.getHttpProxyHost();
				String username = PlanetLabConfiguration.getHttpProxyUsername();
				String password = PlanetLabConfiguration.getHttpProxyPassword();
				// if username AND password is specified
				if (username != PlanetLabConfiguration.DEFAULT_HTTP_PROXY_USERNAME 
						&& password != PlanetLabConfiguration.DEFAULT_HTTP_PROXY_PASSWORD) {
					sshConn.setProxyData(new HTTPProxyData(hostname, port,
							username, password));
					System.out
							.println("ssh connect with http proxy and auth, host="
									+ hostname
									+ " port="
									+ port
									+ "user="
									+ username);
				} else {
					// ok, only hostname and port
					sshConn.setProxyData(new HTTPProxyData(PlanetLabConfiguration.getHttpProxyHost(), 
							port));
					System.out.println("ssh connect with http proxy, host="
							+ hostname + " port=" + port);
				}
			}
			// try to open the connection

			try {
				// try to connect
				sshConn.connect(null, SSH_CONNECT_TIMEOUT,
						SSH_KEY_EXCHANGE_TIMEOUT);

				// try to authenticate
//				if (sshConn.authenticateWithPublicKey(controller
//						.getCredentials().getSlice(), new File(controller
//						.getCredentials().getKeyPath()), controller
//						.getCredentials().getKeyFilePassword())) {
				
				if (sshConn.authenticateWithPublicKey(credentials.getUsername()
						, new File(credentials.getKeyPath()), credentials.getKeyFilePassword())) 
				{				

					// ok, authentiaction succesfull, return the connection
					commandSpec.receivedControlData("connect successful");
					isConnected = true;
					return true;

				} else {
					// well, authentication failed
					statusChange("auth failed", LOG_DEVEL);
					commandSpec.setExitCode(1, "auth failed");
					commandSpec.receivedControlErr("auth failed");
				}

				// handle errors...
			} catch (SocketTimeoutException e) {
				this.statusChange("connection timeout: " + e.getMessage(),
						LOG_DEVEL);
				if (e.getMessage().contains("kex")) {
					commandSpec.setExitCode(4, "kex timeout");
				} else {
					commandSpec.setExitCode(3, "conn timeout");
				}
				commandSpec.receivedControlErr(e.getMessage());
			} catch (IOException e) {

				if (e.getCause() != null) {
					commandSpec.receivedControlErr(e.getCause().getMessage());
					if (e.getCause().getMessage().contains("Connection reset")) {
						statusChange(e.getCause().getMessage(), LOG_DEVEL);
						commandSpec.setExitCode(2, "conn reset");

					} else if (e.getCause().getMessage().contains(
							"Connection refused")) {
						statusChange(e.getCause().getMessage(), LOG_DEVEL);
						commandSpec.setExitCode(2, "conn refused");

					} else if (e.getCause().getMessage().contains(
							"Premature connection close")) {
						statusChange(e.getCause().getMessage(), LOG_DEVEL);
						commandSpec.setExitCode(2, "prem close");

					} else if (e.getCause() instanceof java.net.UnknownHostException) {
						statusChange(e.getCause().getMessage(), LOG_DEVEL);
						commandSpec.setExitCode(2, "dns unknown");

					} else if (e.getCause() instanceof NoRouteToHostException) {
						statusChange(e.getCause().getMessage(), LOG_DEVEL);
						commandSpec.setExitCode(2, "no route");
					} else if (e.getMessage().contains("Publickey")) {
						statusChange(e.getMessage(), LOG_DEVEL);
						commandSpec.setExitCode(2, "auth error");
					} else {
						System.err.println("NEW EXCEPTION TYPE, handle...");

						e.printStackTrace();
					}
				} else {
					commandSpec.receivedErr(e.getMessage());
					commandSpec.setExitCode(255, "other");

					statusChange(e.getMessage(), LOG_DEVEL);
				}

			}

			return false;
		}

	}

	private CommandSpec getNextFromQueue() throws InterruptedException {
		CommandSpec cs = commandQueue.take();
		currentCommand++;

		if (!getCommand(currentCommand).equals(cs.getCommand())) {
			System.err.println("eer");
			System.err.println(getCommand(currentCommand) + ","
					+ cs.getCommand());

		}
		return cs;
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
//		return UploadManager.getInstance().uploadDir(this, fileOrDir,
//				commandSpec);
		return false;
		
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

//		return dManager.downloadDir(this, remotePath, localBaseDir, fileFilter,
//				commandSpec);
		return false;
	}

	private void statusChange(String status, int level) {
		this.status = status;
		if (level <= LOG_LEVEL) {
			System.out.println(plHost + ": " + status);
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

	/**
	 * Very primitive arguments parser, only supports " and ' and only on at the
	 * same time
	 * 
	 * @param parameters
	 * @return String array of the space separated parameters
	 */

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

}
