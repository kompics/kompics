package se.sics.kompics.wan.daemon.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.daemon.Daemon;
import se.sics.kompics.wan.daemon.JobRemoveResponseMsg;
import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.job.JobExited;
import se.sics.kompics.wan.job.JobLoadRequest;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobReadFromExecutingRequest;
import se.sics.kompics.wan.job.JobReadFromExecutingResponse;
import se.sics.kompics.wan.job.JobRemoveRequest;
import se.sics.kompics.wan.job.JobRemoveResponse;
import se.sics.kompics.wan.job.JobStartRequest;
import se.sics.kompics.wan.job.JobStartResponse;
import se.sics.kompics.wan.job.JobStopRequest;
import se.sics.kompics.wan.job.JobStopResponse;
import se.sics.kompics.wan.job.JobWriteToExecutingRequest;
import se.sics.kompics.wan.util.PomUtils;

public class MavenLauncher extends ComponentDefinition {

	public static final String SCENARIO_FILENAME = "scenario";

	private static final Logger logger = LoggerFactory.getLogger(MavenLauncher.class);

	private Positive<Network> net = positive(Network.class);

	private Negative<Maven> maven = negative(Maven.class);

	private Map<Integer, Job> executingJobs = new HashMap<Integer, Job>();

	private Map<Integer, ProcessWrapper> executingProcesses = new ConcurrentHashMap<Integer, ProcessWrapper>();

	public class ProcessWrapper implements Runnable {
		private final int jobId;

		private Process process=null;

		private BufferedWriter writeToInput=null;

		private BufferedReader readFromOutput=null;

		private final ProcessBuilder processBuilder;

		private AtomicBoolean started = new AtomicBoolean(false);

		private int exitValue = -111;

		public ProcessWrapper(int jobId, ProcessBuilder processBuilder) {
			this.jobId = jobId;
			if (processBuilder == null) {
				throw new IllegalArgumentException("ProcessBuilder was null");
			}
			this.processBuilder = processBuilder;
		}

		public void run() {

			String exitMsg = jobId + ": Process exited successfully";
			try {
				process = processBuilder.start();
				writeToInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
				// XXX configure output to write to Socket for log4j at Master
				readFromOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

				started.set(true);

				exitValue = process.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
				exitMsg = e.getMessage();
			} catch (InterruptedException e) {
				e.printStackTrace();
				exitMsg = e.getMessage();
			} finally {
				executingJobs.remove(jobId);
				executingProcesses.remove(jobId);
			}

			// XXX notify MavenLauncher that process is exiting with event
			notifyProcessExiting(jobId, exitValue, exitMsg);
		}

		public boolean started() {
			return started.get();
		}

		public int getExitValue() {
			return exitValue;
		}

		/**
		 * @return true : stopped, false : already stopped
		 */
		public synchronized boolean destroy() {
			if (isAlive() == false) {
				return false;
			}
			process.destroy();
			return true;
		}

		public int getJobId() {
			return jobId;
		}

		public boolean isAlive() {
			try {
				// XXX hack here. Shouldn't use RuntimeException
				// to determine if process hasn't failed.
				int exitValue = process.exitValue();
			} catch (IllegalThreadStateException e) {
				return true;
			}
			return false;
		}

		/**
		 * Input.
		 * 
		 * @param string
		 *            the string
		 * 
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		public final void writeBufferedInput(String string) throws IOException {
			if (writeToInput == null)
			{
				return;
			}
			writeToInput.write(string);
			writeToInput.write("\n");
			writeToInput.flush();
		}

		public final String readLineFromOutput() throws IOException {
			if (readFromOutput == null)
			{
				return "Process not initialized yet";
			}
			return readFromOutput.readLine();
		}

		public final CharBuffer readBufferedOutput(CharBuffer cb) throws IOException {
			if (readFromOutput == null)
			{
				return cb.append("Process not initialized yet");
			}
			if (readFromOutput.ready() == false)
			{
				return cb.append("Process not ready yet");
			}
			readFromOutput.read(cb);
			return cb;
		}

	}

	public MavenLauncher() {

		subscribe(handleJobLoadRequest, maven);
		subscribe(handleJobStartRequest, maven);
		subscribe(handleJobStopRequest, maven);
		subscribe(handleJobRemoveRequest, maven);
		subscribe(handleJobWriteToExecuting, maven);
		subscribe(handleJobReadFromExecuting, maven);
	}

	public Handler<JobReadFromExecutingRequest> handleJobReadFromExecuting = 
		new Handler<JobReadFromExecutingRequest>() {
		public void handle(JobReadFromExecutingRequest event) {

			int jobId = event.getJobId();
			ProcessWrapper p = executingProcesses.get(jobId);
			if (p == null) {
				throw new IllegalStateException("Process p not found for jobId: " + jobId);
			}

			String msg;
			CharBuffer cb = CharBuffer.allocate(1000);
			try {
				cb = p.readBufferedOutput(cb);
				// msg = p.readLineFromOutput();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				cb = CharBuffer.allocate(e.getMessage().length());
				cb.put(e.getMessage());
			}
			cb.rewind();
			msg = cb.toString();
			JobReadFromExecutingResponse resp = new JobReadFromExecutingResponse(event, event
					.getJobId(), msg);
			trigger(resp, maven);
		}
	};
	
	
	public Handler<JobWriteToExecutingRequest> handleJobWriteToExecuting = 
		new Handler<JobWriteToExecutingRequest>() {
		public void handle(JobWriteToExecutingRequest event) {

			int jobId = event.getJobId();
			ProcessWrapper p = executingProcesses.get(jobId);
			if (p == null) {
				throw new IllegalStateException("Process p not found for jobId: " + jobId);
			}


			String msg;
			try {
				p.writeBufferedInput(event.getMsg());
				CharBuffer cb = CharBuffer.allocate(1000);
				try {
					cb = p.readBufferedOutput(cb);
				} catch (IOException e) {
					e.printStackTrace();
					cb = CharBuffer.allocate(e.getMessage().length());
					cb.put(e.getMessage());
				}
				cb.rewind();
				msg = cb.toString();
			} catch (IOException e1) {
				msg = e1.toString();
			}
			
			
			JobReadFromExecutingResponse resp = new JobReadFromExecutingResponse(event, event
					.getJobId(), msg);
			trigger(resp, maven);
		}
	};

	public Handler<JobRemoveRequest> handleJobRemoveRequest = new Handler<JobRemoveRequest>() {
		public void handle(JobRemoveRequest event) {
			String msg = "Success";
			JobRemoveResponseMsg.Status status = JobRemoveResponseMsg.Status.SUCCESS;
			Job job = event.getJob();
			File pomFile = job.getPomFile();

			try {
				removeFileRecurse(pomFile.getParentFile(), true);
			} catch (FileRemovalException e) {
				msg = e.getMessage();
				status = JobRemoveResponseMsg.Status.ERROR;
			}
			trigger(new JobRemoveResponse(event, status, event.getJob().getId(), msg), maven);
		}
	};

	/**
	 * @param pathToPomDir
	 *            directory containing pom.xml file.
	 * @param isInPomDir
	 * @return true if success, false is only used in recursion not in public
	 *         API.
	 * @throws FileRemovalException
	 *             if the file wasn't successfully deleted.
	 */
	private boolean removeFileRecurse(File pathToPomDir, boolean isInPomDir)
			throws FileRemovalException {
		if (isInPomDir == true) {
			if (pathToPomDir.exists()) {
				if (pathToPomDir.isDirectory()) {
					File[] files = pathToPomDir.listFiles();
					for (int i = 0; i < files.length; i++) {
						if (files[i].isDirectory()) {
							removeFileRecurse(files[i], true);
						} else {
							if (files[i].delete() == false) {
								throw new FileRemovalException("Problem when deleting file: "
										+ files[i].getAbsolutePath());
							}
						}

					}
				}
			}
		}
		File parent = pathToPomDir.getParentFile();

		if (pathToPomDir.delete() == false) {
			// if I couldn't delete the pom dir, throw exception
			// don't throw exception if couldn't delete a parent directory, as
			// this is
			// expected behaviour if the parent directory contains existing
			// files/dirs.
			if (isInPomDir == true) {
				return false;
			}

		}

		// recurse deleting dirs and stop when Daemon.KOMPICS_HOME is reached.
		if (parent.getAbsolutePath().compareTo(Daemon.KOMPICS_HOME) != 0) {
			removeFileRecurse(parent, false);
		}

		return true;
	}

	public Handler<JobLoadRequest> handleJobLoadRequest = new Handler<JobLoadRequest>() {
		public void handle(JobLoadRequest event) {

			int id = event.getId();
			JobLoadResponse.Status status;

			// If msg not a duplicate
			// if (assembledJobs.containsKey(id) == false &&
			// executingJobs.containsKey(id) == false) {
			try {
				event.createDummyPomFile();
				status = JobLoadResponse.Status.POM_CREATED;
				status = mvnAssemblyAssembly(event);
				// assembledJobs.put(id, event);
			} catch (DummyPomConstructionException e) {
				e.printStackTrace();
				status = JobLoadResponse.Status.FAIL;
			} catch (MavenExecException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				status = JobLoadResponse.Status.FAIL;
			}

			// } else {
			// status = JobAssemblyResponse.Status.DUPLICATE;
			// }

			trigger(new JobLoadResponse(event, id, status), maven);
		}
	};

	public Handler<JobStartRequest> handleJobStartRequest = new Handler<JobStartRequest>() {
		public void handle(JobStartRequest event) {
			int jobId = event.getId();

			JobStartResponse.Status status = JobStartResponse.Status.SUCCESS;
			if (event == null) {
				status = JobStartResponse.Status.FAIL;
			} else {
				// status = mvnExecExec(event, event.getScenario());
				forkDummyExec(event.getNumPeers(), event, event.getScenario());
			}

			// ProcessWrapper p = executingProcesses.get(id);
			JobStartResponse response = new JobStartResponse(event, jobId, status);
			trigger(response, maven);
		}
	};

	private JobLoadResponse.Status mvnAssemblyAssembly(JobLoadRequest job)
			throws MavenExecException {
		JobLoadResponse.Status status = JobLoadResponse.Status.ASSEMBLED;
		MavenWrapper mw = new MavenWrapper(job.getPomFile().getAbsolutePath());

		// XXX redirecting stdin and stderr for this command, due to much
		// crap being printed. <pluginmanagement> not recongnized in assembly
		// artifact
		// by maven embedder.
		PrintStream origOut = System.out;
		PrintStream origErr = System.err;

		if (job.isHideMavenOutput() == true)
		{
			System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
				public void write(int devNull) { // dump output (like writing to
													// /dev/null)
				}
			}));
			System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
				public void write(int devNull) { // dump output (like writing to
													// /dev/null)
				}
			}));
		}

		mw.execute("assembly:assembly");

		System.setOut(origOut);
		System.setErr(origErr);

		logger.info("maven assembly:assembly completed...");
		return status;
	}

	private JobStartResponse.Status forkDummyExec(int numPeers, JobStartRequest job,
			SimulationScenario scenario) {
		JobStartResponse.Status status;
		int res = forkProcess(numPeers, job, scenario);
		switch (res) {
		case 0:
			status = JobStartResponse.Status.SUCCESS;
			break;
		case -1:
			status = JobStartResponse.Status.FAIL;
			break;
		default:
			status = JobStartResponse.Status.FAIL;
			break;
		}
		return status;
	}

	/**
	 * @param id
	 * @param job
	 * @param scenario
	 * @param assembly
	 * @return 0 on success, -1 on failure.
	 */
	private int forkProcess(int numPeers, Job job, SimulationScenario scenario) {
		int res = 0;
		String classPath = System.getProperty("java.class.path");
		java.util.List<String> command = new ArrayList<String>();
		command.add("java");
		command.add("-classpath");
		classPath = classPath + File.pathSeparatorChar + job.getDummyJarWithDependenciesName();
		command.add(classPath);
		command.add(job.getMainClass());
		command.add("slave");
		command.add("-" + Configuration.OPT_PEERS + " " + Integer.toString(numPeers)); 

		command.addAll(job.getArgs());
		command.add("-Dlog4j.properties=log4j.properties");
		command.add("-DKOMPICS_HOME=" + Daemon.KOMPICS_HOME);
		command.add("-DMAVEN_REPO_LOCAL=" + Daemon.MAVEN_REPO_LOCAL);

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		Map<String, String> env = processBuilder.environment();
		env.put("KOMPICS_HOME", Daemon.KOMPICS_HOME);
		env.put("MAVEN_REPO_LOCAL", Daemon.MAVEN_REPO_LOCAL);

		if (scenario != null) {
			File file = null;
			ObjectOutputStream oos = null;
			try {
				file = File.createTempFile(SCENARIO_FILENAME, ".bin");
				oos = new ObjectOutputStream(new FileOutputStream(file));
				oos.writeObject(scenario);
				oos.flush();
				oos.close();
				env.put(SCENARIO_FILENAME, file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
				logger.debug(e.getMessage());
			} finally {
				if (oos != null) {
					try {
						oos.close();
					} catch (IOException ignoreException) {
					}
				}
			}
		}

		StringBuffer sb = new StringBuffer();
		for (String s : command) {
			sb.append(s);
			sb.append(" ");
		}
		logger.debug("Executing: " + sb.toString());
		logger.info("Short version: java -jar " + job.getDummyJarWithDependenciesName()
				+ PomUtils.sepStr() + job.getMainClass());

		ProcessWrapper pw = new ProcessWrapper(job.getId(), processBuilder);
		executingProcesses.put(job.getId(), pw);
		executingJobs.put(job.getId(), job);
		new Thread(pw).start();
		return res;
	}

	public Handler<JobStopRequest> handleJobStopRequest = new Handler<JobStopRequest>() {
		public void handle(JobStopRequest event) {

			int id = event.getJobId();
			JobStopResponse.Status status;

			String msg = "Successfully stopped " + id;
			ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null) {
				status = JobStopResponse.Status.COULD_NOT_FIND_PROCESS_HANDLE_TO_STOP_JOB;
				msg = "Failed to stop " + id;
			} else {
				if (pw.destroy() == true) {
					status = JobStopResponse.Status.STOPPED;
				} else {
					status = JobStopResponse.Status.ALREADY_STOPPED;
					msg = "Already stopped " + id;
				}
			}

			JobStopResponse response = new JobStopResponse(event, id, status, msg);
			trigger(response, maven);
		}
	};

	/**
	 * This method should only be called from WrappedThread instances...
	 * 
	 * @param jobId
	 * @param exitValue
	 * @param exitMsg
	 */
	protected synchronized void notifyProcessExiting(int jobId, int exitValue, String exitMsg) {
		trigger(new JobExited(jobId, exitValue, exitMsg), maven);
	}

	// public Handler<JobMessageRequest> handleJobMessageRequest = new
	// Handler<JobMessageRequest>() {
	// public void handle(JobMessageRequest event) {
	// int id = event.getJobId();
	// JobMessageResponse.Status status;
	// ProcessWrapper pw = executingProcesses.get(id);
	// if (pw == null)
	// {
	// status = JobMessageResponse.Status.STOPPED;
	// }
	// else
	// {
	// try {
	// pw.input(event.getMsg());
	//					
	// status = JobMessageResponse.Status.SUCCESS;
	// }
	// catch (IOException e)
	// {
	// status = JobMessageResponse.Status.FAIL;
	// }
	// }
	//				
	// JobMessageResponse response = new JobMessageResponse(id,status,net
	// new DaemonAddress(daemonId, event.getDestination()),
	// event.getSource());
	// trigger(response,net);
	// }
	// };

}
