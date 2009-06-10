package se.sics.kompics.wan.plab;

import java.util.Map;

public interface RpcFunctions {

	/**
	 * "PlanetLab.connectToHost"
	 */
	public static final String PLANET_LAB_CONNECT_TO_HOST = "PlanetLab.connectToHost";

	public static final String PLANET_LAB_TOTAL_COMMAND_NUM = "PlanetLab.totalCommandNum";

	public static final String PLANET_LAB_GET_HOST_STATUS_OVERVIEW = "PlanetLab.getHostStatusOverview";

	public static final String PLANET_LAB_GET_COMMAND_STATUS_OVERVIEW = "PlanetLab.getCommandStatusOverview";

	public static final String PLANET_LAB_GET_COMMAND_OVERVIEW = "PlanetLab.getCommandOverview";

	public static final String PLANET_LAB_TOTAL_HOST_NUM = "PlanetLab.totalHostNum";

	public static final String PLANET_LAB_READ_CONSOLE = "PlanetLab.readConsole";

	public static final String PLANET_LAB_GET_HOSTNAME = "PlanetLab.getHostname";

	public static final String PLANET_LAB_SHUTDOWN = "PlanetLab.shutdown";

	public static final String PLANET_LAB_QUEUE_COMMAND = "PlanetLab.queueCommand";

	public static final String PLANET_LAB_GET_COMMAND = "PlanetLab.getCommand";

	public static final String PLANET_LAB_ADD_RANDOM_HOSTS_FROM_PLC = "PlanetLab.addRandomHostsFromPLC";

	public static final String PLANET_LAB_ADD_RANDOM_SITES_FROM_PLC = "PlanetLab.addRandomSitesFromPLC";

	public static final String PLANET_LAB_UPLOAD = "PlanetLab.upload";

	public static final String PLANET_LAB_DOWNLOAD = "PlanetLab.download";

	public static final String SPECIAL_COMMAND_UPLOAD_DIR = "#upload";

	public static final String SPECIAL_COMMAND_DOWNLOAD_DIR = "#download";

	public static final String CONNECT_FAILED_POLICY_SITE = "site";

	public static final String CONNECT_FAILED_POLICY_ANY = "any";

	public static final String CONNECT_FAILED_POLICY_CLOSEST = "closest";

	public static final String PLANET_LAB_GET_COMMAND_STATS = "PlanetLab.getCommandStats";

	public static final String PLANET_LAB_KILL_COMMAND = "PlanetLab.kill";

	public static final String PLANET_LAB_GET_HOSTS_NOT_IN_SLICE = "PlanetLab.getHostsNotInSlice";

	public static final String PLANET_LAB_ADD_HOSTS_TO_SLICE = "PlanetLab.addHostsToSlice";

	public static final String PLANET_LAB_FETCH_COMON_DATA = "PlanetLab.fetchCoMonData";

	public static final String PLANET_LAB_GET_COMON_PROGRESS = "PlanetLab.getCoMonProgress";

	public static final String PLANET_LAB_GET_HOST_STATS = "PlanetLab.getHostStats";

	public static final String PLANET_LAB_GET_ALL_HOSTS = "PlanetLab.getAllHosts";

	public static final String PLANET_LAB_GET_AVAILABLE_HOSTS = "PlanetLab.getAvailableHosts";
	
	/**
	 * Requests "numberOfHosts" random hosts from PLC and tries to connect to
	 * them. The connect failed policy used is "any"
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server. The values checked are Username and AuthString.
	 *            These can be provided in clear text or as md5 hashes. If md5
	 *            hashes are used, the Username field should contain the md5
	 *            hash of the username string, the AuthString field should
	 *            contain the md5 hash of the username string concatenated with
	 *            the password string. The use of md5 hashes makes it difficult
	 *            for an attacker to get the password by monitoring the network.
	 *            Keep in mind that the password can be extracted from the md5
	 *            hashes by an attacker by using a dictonary.
	 * @param numberOfHosts
	 *            number of hosts to request
	 * @return number of hosts actually added, this can be less that
	 *         "numberOfHosts" if, the total number of hosts available is less
	 *         than "numberOfHosts"
	 */
	public int addRandomHostsFromPLC(Map auth, int numberOfHosts);

	/**
	 * Requests "numberOfSites" random sites from PLC and tries to connect to
	 * one random host in each site . The connect failed policy is "site"
	 * meaning that if the connect failed, the controller will try another host
	 * in the same site
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param numberOfSites
	 *            number of sites to request
	 * @return number of sites actually added, this can be less that
	 *         "numberOfSites" if, the total number of sites available is less
	 *         than "numberOfSites"
	 * 
	 */
	public int addRandomSitesFromPLC(Map auth, int numberOfSites);

	/**
	 * Returns what percentage of hosts that has completed command "commandId"
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param commandId
	 *            the id of the command to query
	 * @return double between 0 and 1, 1 meaning that all hosts have completed
	 *         the command
	 */
	public double commandCompleted(Map auth, int commandId);

	/**
	 * Request the controller to connect to host hostname.
	 * 
	 * If the ip accociated with the hostname is recognized by PLC, information
	 * from PLC, like bandwidth cap, will be associated with the connection
	 * 
	 * If any commands have bee queued on other hosts previously, those command
	 * will be queued on this hosts as well
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param hostname
	 *            hostname or ip address to connect to
	 * @return the connection id of the host (used to check host progress), or
	 *         -1 on authentication failure
	 */
	public int connectToHost(Map auth, String hostname);

	/**
	 * Queries the controller for the list of nodes currently connected to Lazy
	 * wrapper for PlanetLab.getSuccessfulHosts(auth,0)
	 * 
	 * @param auth
	 *            authentication
	 * @return a String array of hostnames
	 */
	public Object[] getConnectedHosts(Map auth);

	/**
	 * Queries the controller for a list of all nodes that succesfully have
	 * completed all commands. The returned hosts must have completed command
	 * commandId with 0 exit status
	 * 
	 * @param auth
	 *            authentication info
	 * @param commandId
	 *            the command that must have completed succesfully, or -1 for
	 *            all commands
	 * @return a String of hostnames
	 */
	public Object[] getSuccessfulHosts(Map auth, int commandId);

	/**
	 * return the command associated with a certain commmandId
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param commandId
	 * @return the command associated with the commandId, or -1 on
	 *         authentication failure
	 */
	public String getCommand(Map auth, int commandId);

	/**
	 * used by the gui to get a summary of the status of each command
	 * 
	 * @param auth
	 * @return
	 */
	public Object[] getCommandStatusOverview(Map auth);

	public Object[] getCommandOverview(Map auth, int commandId);

	/**
	 * returns a map of exitcode=>count
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param commandId
	 *            the id of the command to query
	 * @return counts of how many hosts that have each exitcode
	 */
	public Map getExitStats(Map auth, int commandId);

	/**
	 * returns the hostname given a connectionId
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param connectionId
	 *            the connectionId of the host to get the hostname of
	 * @return The hostname
	 */
	public String getHostname(Map auth, int connectionId);

	/**
	 * used by the gui to get a summary of the status of each host
	 * 
	 * @param auth
	 * @return
	 */
	public Object[] getHostStatusOverview(Map auth);

	/**
	 * lazy wrapper for queueCommand(Map auth, String command, 0, false),
	 * 
	 * Running "command" with no timeout and continuing with subsequent commands
	 * even if the command returns a non non-zero exit code
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param command
	 *            the command to queue
	 * @return the index of the command, used to get for example exit-status
	 *         when the command has completed, or -1 on authentication failure
	 */
	public int queueCommand(Map auth, String command);

	/**
	 * lazy wrapper for queueCommand(Map auth, String command, double timeout,
	 * false),
	 * 
	 * Running the command until it completes, or gets killed because the
	 * timeout expired. Subsequent commands will be run even if the command
	 * returns a non non-zero exit code
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param command
	 *            the command to queue
	 * @param timeout
	 *            the maximum number of seconds (or fractions of a second) the
	 *            command can run, after this the command will get killed
	 * @return the index of the command, used to get for example exit-status
	 *         when the command has completed, or -1 on authentication failure
	 * 
	 */
	public int queueCommand(Map auth, String command, double timeout);

	/**
	 * Add the command "command" to the queue of command to execute
	 * 
	 * Running the command until it completes, or gets killed because the
	 * timeout expired. Subsequent commands will not run if stop on error is
	 * true
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param command
	 *            the command to queue
	 * @param timeout
	 *            the maximum number of seconds (or fractions of a second) the
	 *            command can run, after this the command will get killed
	 * @param stopOnError
	 *            controller if execution of commands should stop if the
	 *            commands exit with a non-zero exit code. If stop on error is
	 *            enabled execution is halted until the user override.
	 * @return the index of the command, used to get for example exit-status
	 *         when the command has completed, or -1 on authentication failure
	 * 
	 */
	public int queueCommand(Map auth, String command, double timeout,
			boolean stopOnError);

	public int queueCommand(Map auth, String command, double timeout,
			int stopOnError);

	/**
	 * lazy wrapper for queueCommand(Map auth, String command, double timeout,
	 * false),
	 * 
	 * Running the command until it completes, or gets killed because the
	 * timeout expired. Subsequent commands will be run even if the command
	 * returns a non non-zero exit code
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param command
	 *            the command to queue
	 * @param timeout
	 *            the maximum number of seconds the command can run, after this
	 *            the command will get killed
	 * @return the index of the command, used to get for example exit-status
	 *         when the command has completed, or -1 on authentication failure
	 */
	public int queueCommand(Map auth, String command, int timeout);

	/**
	 * Reads the consle from ssh connection,
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param connectionId
	 *            The connection ID of the host to read the console of
	 * @param fromRow
	 *            Row to start reading from, useful when only new information is
	 *            interesting, like for example in a terminal like window
	 * 
	 * @return Object[], each object in the array an instance of Map<String,Object>,
	 *         where each Map contrains information about one row.
	 * 
	 * "line"=>String, the output line
	 * 
	 * "command"=> String, the command the resulted in the line
	 * 
	 * "stderr"=> Boolean, true if the line was read from stderr, false if read
	 * from stdout
	 * 
	 * "time"=>Double, the number of seconds since application start the line
	 * was received
	 * 
	 * "execution_time"=>Double, number of seconds the command had been running
	 * when the line was received
	 */
	public Object[] readConsole(Map auth, int connectionId, int fromRow);

	/**
	 * stops the RPC Server and closes all current ssh connections, including
	 * any running scp transfers
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @return 0 on successful shutdown and -1 on authentication failure
	 */
	public int shutdown(Map auth);

	public int kill(Map auth, int commandId);

	/**
	 * returns the total number of commands that have been queued on the
	 * controller
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @return number of commands executed + in queue
	 */
	public int totalCommandNum(Map auth);

	/**
	 * returns the total number of hosts assisated with the system
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @return total number of hosts
	 */
	public int totalHostNum(Map auth);

	/**
	 * Uploads a local file or directory to all hosts
	 * 
	 * The upload is implemented as follows:
	 * 
	 * First a md5 checksum is calculated of the file or on all files in the
	 * specified directory and subdirectories
	 * 
	 * Secondly md5s are calculated on the destination to check if the file(s)
	 * already exists. If the md5 check fails, or the file doesn't exist, the
	 * file is uploaded.
	 * 
	 * Thirdly a second md5 check is done on the uploaded file, if this fails
	 * the file is uploaded-and-checked two more times. If the md5 check fails
	 * the third time the command fail and execution of subsequent commands on
	 * the host will stop.
	 * 
	 * All files are copied to ~/dir_name on the remote hosts, if for example
	 * 
	 * /homes/isdal/bandwidth_experiment/bw_planetlab_bin/
	 * 
	 * is uplaoded, the directory ~/bw_planetlab_bin will be created on the
	 * remote hosts and all files will copied there
	 * 
	 * @param auth
	 *            authentication information that is matching the information on
	 *            the server
	 * @param path
	 *            the local directory to upload
	 * @param timeout
	 *            maximum time allowed for the upload
	 * @param stopOnError
	 *            stop exection on command if the upload was unsuccessful
	 * @return 0 if the local directory exists, 1 if the local directory doesn't
	 *         exist or is unreachable, -1 on authentication failure
	 */
	public int upload(Map auth, String path, double timeout, boolean stopOnError);

	public int upload(Map auth, String path, double timeout, int stopOnError);

	public Object[] getCommandStats(Map auth, int connectionId);

	public int download(Map auth, String remotePath, String localPath,
			String fileFilter, String localNamingType);

	public int addHostsToSlice(Map auth, Object[] hosts);

	public Object[] getHostsNotInSlice(Map auth);

	public Object[] getAllHosts(Map auth);

	public Object[] getAvailableHosts(Map auth);
	
	public int fetchCoMonData(Map auth);

	public double getCoMonProgress(Map auth);

	public Object[] getHostStats(Map auth, Object[] hosts);
}
