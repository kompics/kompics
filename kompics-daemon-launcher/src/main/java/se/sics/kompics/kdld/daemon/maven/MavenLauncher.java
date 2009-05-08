package se.sics.kompics.kdld.daemon.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
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
import se.sics.kompics.kdld.daemon.Daemon;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.kdld.daemon.JobStopRequest;
import se.sics.kompics.kdld.daemon.JobStopResponse;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Job;
import se.sics.kompics.kdld.job.JobAssembly;
import se.sics.kompics.kdld.job.JobAssemblyResponse;
import se.sics.kompics.kdld.job.JobExec;
import se.sics.kompics.kdld.job.JobExecResponse;
import se.sics.kompics.kdld.job.JobExited;
import se.sics.kompics.kdld.job.JobReadFromExecuting;
import se.sics.kompics.kdld.job.JobReadFromExecutingResponse;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.SimulationScenario;

public class MavenLauncher extends ComponentDefinition {

	public static final String SCENARIO_FILENAME = "scenario";

	private static final Logger logger = LoggerFactory.getLogger(MavenLauncher.class);

	private Positive<Network> net = positive(Network.class);

	private Negative<Maven> maven = negative(Maven.class);

	private Map<Integer, Job> queuedJobs = new HashMap<Integer, Job>();
	private Map<Integer, Job> executingJobs = new HashMap<Integer, Job>();

	private Map<Integer, ProcessWrapper> executingProcesses = new ConcurrentHashMap<Integer, ProcessWrapper>();

	public class ProcessWrapper implements Runnable {
		private final int jobId;

		private Process process;

		private BufferedWriter writeToInput;

		private BufferedReader readFromOutput;

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

			try {
				process = processBuilder.start();
				writeToInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
				// XXX configure output to write to Socket for log4j at Master
				readFromOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

				started.set(true);

				exitValue = process.waitFor();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// XXX notify MavenLauncher that process is exiting with event
			notifyProcessExiting(jobId, exitValue);
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
		public final void input(String string) throws IOException {
			writeToInput.write(string);
			writeToInput.write("\n");
			writeToInput.flush();
		}

		public final String readLineFromOutput() throws IOException {
			return readFromOutput.readLine();
		}

		public final String readBufferedOutput() throws IOException {
			CharBuffer cb = CharBuffer.allocate(1000);
			readFromOutput.read(cb);
			return cb.toString();
		}

		
		public final int readCharsFromOutput(char[] cbuf) throws IOException {
			return readFromOutput.read(cbuf);
		}

	}

	public MavenLauncher() {

		subscribe(handleJobAssembleRequest, maven);
		subscribe(handleJobExecRequest, maven);
		subscribe(handleJobStopRequest, maven);
		subscribe(handleJobReadFromExecuting, maven);
	}

	public Handler<JobReadFromExecuting> handleJobReadFromExecuting = 
		new Handler<JobReadFromExecuting>() {
		public void handle(JobReadFromExecuting event) {

			int jobId = event.getJobId();
			ProcessWrapper p = executingProcesses.get(jobId);
			String msg;
			try {
				msg = p.readBufferedOutput();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				msg = e.getMessage();
			}
			JobReadFromExecutingResponse resp = new JobReadFromExecutingResponse(event,event.getJobId(),msg);
			trigger (resp, maven);
		}
	};

	public Handler<JobAssembly> handleJobAssembleRequest = new Handler<JobAssembly>() {
		public void handle(JobAssembly event) {

			int id = event.getId();
			JobAssemblyResponse.Status status;

			// If msg not a duplicate
			if (queuedJobs.containsKey(id) == false && executingJobs.containsKey(id) == false) {
				try {
					event.createDummyPomFile();
					status = JobAssemblyResponse.Status.POM_CREATED;
					queuedJobs.put(id, event);
					status = mvnAssemblyAssembly(event);
				} catch (DummyPomConstructionException e) {
					e.printStackTrace();
					status = JobAssemblyResponse.Status.FAIL;
				}
			} else {
				status = JobAssemblyResponse.Status.DUPLICATE;
			}

			trigger(new JobAssemblyResponse(event, id, status), maven);
		}
	};

	public Handler<JobExec> handleJobExecRequest = new Handler<JobExec>() {
		public void handle(JobExec event) {
			int id = event.getId();
			Job job = queuedJobs.get(id);

			JobExecResponse.Status status = JobExecResponse.Status.SUCCESS;
			if (job == null) {
				status = JobExecResponse.Status.FAIL;
			} else {
				status = mvnExecExec(event, event.getScenario());
			}

			// ProcessWrapper p = executingProcesses.get(id);
			JobExecResponse response = new JobExecResponse(event, id, status);
			trigger(response, maven);
		}
	};

	private JobAssemblyResponse.Status mvnAssemblyAssembly(JobAssembly job) {
		JobAssemblyResponse.Status status = JobAssemblyResponse.Status.ASSEMBLED;
		try {
			MavenWrapper mw = new MavenWrapper(job.getPomFile().getAbsolutePath());
			mw.execute("assembly:assembly");
		} catch (MavenExecException e) {
			status = JobAssemblyResponse.Status.FAIL;
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return status;
	}

	private JobExecResponse.Status forkDummyExec(JobExec job, SimulationScenario scenario) {
		JobExecResponse.Status status;
		int res = forkProcess(job, scenario);
		switch (res) {
		case 0:
			status = JobExecResponse.Status.SUCCESS;
			break;
		case -1:
			status = JobExecResponse.Status.FAIL;
			break;
		default:
			status = JobExecResponse.Status.FAIL;
			break;
		}
		return status;
	}

	private JobExecResponse.Status mvnExecExec(JobExec job, SimulationScenario scenario) {
		JobExecResponse.Status status = JobExecResponse.Status.SUCCESS;

		try {
			MavenWrapper mw = new MavenWrapper(job.getPomFile().getAbsolutePath());
			mw.execute("exec:exec");
		} catch (MavenExecException e) {
			status = JobExecResponse.Status.FAIL;
			e.printStackTrace();
			logger.error(e.getMessage());
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
	private int forkProcess(Job job, SimulationScenario scenario) {
		int res = 0;
		String classPath = System.getProperty("java.class.path");
		java.util.List<String> command = new ArrayList<String>();
		command.add("java");
		command.add("-classpath");
		classPath = classPath + File.pathSeparatorChar + job.getDummyJarWithDependenciesName();
		command.add(classPath);
		command.add(job.getMainClass());
		command.addAll(job.getArgs());
		command.add("-Dlog4j.properties=log4j.properties");
		command.add("-DKOMPICS_HOME=" + Daemon.KOMPICS_HOME);
		command.add("-DMAVEN_HOME=" + Daemon.MAVEN_HOME);
		command.add("-DMAVEN_REPO_HOME=" + Daemon.MAVEN_REPO_HOME);

		ProcessBuilder processBuilder = new ProcessBuilder(command);

		processBuilder.redirectErrorStream(true);
		Map<String, String> env = processBuilder.environment();
		env.put("KOMPICS_HOME", Daemon.KOMPICS_HOME);

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

		// try {
		ProcessWrapper pw = new ProcessWrapper(job.getId(), processBuilder);
		executingProcesses.put(job.getId(), pw);
		executingJobs.put(job.getId(), job);
		new Thread(pw).start();
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// res = -1;
		// } finally {
		queuedJobs.remove(job.getId());
		// }

		return res;
	}

	public Handler<JobStopRequest> handleJobStopRequest = new Handler<JobStopRequest>() {
		public void handle(JobStopRequest event) {

			int id = event.getJobId();
			JobStopResponse.Status status;

			ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null) {
				status = JobStopResponse.Status.FAILED_TO_STOP;
			} else {
				if (pw.destroy() == true) {
					status = JobStopResponse.Status.STOPPED;
				} else {
					status = JobStopResponse.Status.ALREADY_STOPPED;
				}
			}

			JobStopResponse response = new JobStopResponse(id, status, new DaemonAddress(event
					.getDaemonId(), event.getDestination()), event.getSource());
			trigger(response, net);
		}
	};

	protected synchronized void notifyProcessExiting(int jobId, int exitValue) {
		JobExited.Status status = (exitValue == 0) ? JobExited.Status.EXITED_NORMALLY
				: JobExited.Status.EXITED_WITH_ERROR;
		trigger(new JobExited(jobId, status), maven);
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
	// JobMessageResponse response = new JobMessageResponse(id,status,
	// new DaemonAddress(daemonId, event.getDestination()),
	// event.getSource());
	// trigger(response,net);
	// }
	// };

}
