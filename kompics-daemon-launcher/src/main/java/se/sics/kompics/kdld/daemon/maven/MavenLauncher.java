package se.sics.kompics.kdld.daemon.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
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
import se.sics.kompics.kdld.daemon.JobStopRemoteResponse;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Job;
import se.sics.kompics.kdld.job.JobAssembly;
import se.sics.kompics.kdld.job.JobAssemblyResponse;
import se.sics.kompics.kdld.job.JobExec;
import se.sics.kompics.kdld.job.JobExecResponse;
import se.sics.kompics.kdld.job.JobExited;
import se.sics.kompics.kdld.job.JobReadFromExecuting;
import se.sics.kompics.kdld.job.JobReadFromExecutingResponse;
import se.sics.kompics.kdld.job.JobStopRequest;
import se.sics.kompics.kdld.job.JobStopResponse;
import se.sics.kompics.kdld.util.PomUtils;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.SimulationScenario;

public class MavenLauncher extends ComponentDefinition {

	public static final String SCENARIO_FILENAME = "scenario";

	private static final Logger logger = LoggerFactory.getLogger(MavenLauncher.class);

	private Positive<Network> net = positive(Network.class);

	private Negative<Maven> maven = negative(Maven.class);

//	private Map<Integer, Job> assembledJobs = new HashMap<Integer, Job>();
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
			}
			finally {
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
		public final void input(String string) throws IOException {
			writeToInput.write(string);
			writeToInput.write("\n");
			writeToInput.flush();
		}

		public final String readLineFromOutput() throws IOException {
			return readFromOutput.readLine();
		}

		public final CharBuffer readBufferedOutput(CharBuffer cb) throws IOException {
			readFromOutput.read(cb);
			return cb;
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
			if (p == null)
			{
				throw new IllegalStateException("Process p not found for jobId: " + jobId);
			}
			
			String msg;
			CharBuffer cb=CharBuffer.allocate(1000);
			try {
				cb = p.readBufferedOutput(cb);
//				msg = p.readLineFromOutput();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				cb = CharBuffer.allocate(e.getMessage().length());
				cb.put(e.getMessage());
			}
			cb.rewind();
			msg = cb.toString();
			JobReadFromExecutingResponse resp = 
				new JobReadFromExecutingResponse(event,event.getJobId(), msg);
			trigger (resp, maven);
		}
	};

	
	
	public Handler<JobAssembly> handleJobAssembleRequest = new Handler<JobAssembly>() {
		public void handle(JobAssembly event) {

			int id = event.getId();
			JobAssemblyResponse.Status status;

			// If msg not a duplicate
//			if (assembledJobs.containsKey(id) == false && executingJobs.containsKey(id) == false) {
				try {
					event.createDummyPomFile();
					status = JobAssemblyResponse.Status.POM_CREATED;
					status = mvnAssemblyAssembly(event);
//					assembledJobs.put(id, event);					
				} catch (DummyPomConstructionException e) {
					e.printStackTrace();
					status = JobAssemblyResponse.Status.FAIL;
				} catch (MavenExecException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
					status = JobAssemblyResponse.Status.FAIL;
				}
				
//			} else {
//				status = JobAssemblyResponse.Status.DUPLICATE;
//			}

			trigger(new JobAssemblyResponse(event, id, status), maven);
		}
	};

	public Handler<JobExec> handleJobExecRequest = new Handler<JobExec>() {
		public void handle(JobExec event) {
			int id = event.getId();

			JobExecResponse.Status status = JobExecResponse.Status.SUCCESS;
			if (event == null) {
				status = JobExecResponse.Status.FAIL;
			} else {
//				status = mvnExecExec(event, event.getScenario());
				forkDummyExec(event, event.getScenario());
			}

			// ProcessWrapper p = executingProcesses.get(id);
			JobExecResponse response = new JobExecResponse(event, id, status);
			trigger(response, maven);
		}
	};

	private JobAssemblyResponse.Status mvnAssemblyAssembly(JobAssembly job) throws 
		MavenExecException {
		JobAssemblyResponse.Status status = JobAssemblyResponse.Status.ASSEMBLED;
		MavenWrapper mw = new MavenWrapper(job.getPomFile().getAbsolutePath());
		mw.execute("assembly:assembly");
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

	private void mvnExecExec(JobExec job, SimulationScenario scenario) 
		throws MavenExecException
		{
			MavenWrapper mw = new MavenWrapper(job.getPomFile().getAbsolutePath());
			executingJobs.put(job.getId(), job);
			mw.execute("exec:exec");
			
//		} catch (MavenExecException e) {
//			status = JobExecResponse.Status.FAIL;
//			e.printStackTrace();
//			logger.error(e.getMessage());
//		}
//		return status;
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
		logger.info("Short version: java -jar " + job.getDummyJarWithDependenciesName() +
				PomUtils.sepStr() + job.getMainClass());

		ProcessWrapper pw = new ProcessWrapper(job.getId(), processBuilder);
		executingProcesses.put(job.getId(), pw);
		executingJobs.put(job.getId(), job);
		new Thread(pw).start();
//		assembledJobs.remove(job.getId());
		return res;
	}

	public Handler<JobStopRequest> handleJobStopRequest = new Handler<JobStopRequest>() {
		public void handle(JobStopRequest event) {

			int id = event.getJobId();
			JobStopResponse.Status status;

			String msg = "Successfully stopped " + id;
			ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null) {
				status = JobStopResponse.Status.FAILED_TO_STOP;
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
	 * @param jobId
	 * @param exitValue
	 * @param exitMsg
	 */
	protected synchronized void notifyProcessExiting(int jobId, int exitValue, String exitMsg) {
		JobExited.Status status = (exitValue == 0) ? JobExited.Status.EXITED_NORMALLY
				: JobExited.Status.EXITED_WITH_ERROR;
		trigger(new JobExited(jobId, status, exitMsg), maven);
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
