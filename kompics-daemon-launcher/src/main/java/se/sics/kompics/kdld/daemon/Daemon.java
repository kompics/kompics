package se.sics.kompics.kdld.daemon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Job;
import se.sics.kompics.kdld.job.JobAssembly;
import se.sics.kompics.kdld.job.JobAssemblyResponse;
import se.sics.kompics.kdld.job.JobExec;
import se.sics.kompics.kdld.job.JobExecResponse;
import se.sics.kompics.kdld.job.Maven;
import se.sics.kompics.kdld.job.MavenLauncher;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.epfd.diamondp.FailureDetector;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public class Daemon extends ComponentDefinition {

	public static final String KOMPICS_HOME;
	
	public static final String SCENARIO_FILENAME = "scenario";
	
	static {
		String kHome = System.getProperty("kompics.home");
		String userHome = System.getProperty("user.home");
		if (userHome != null && kHome == null) {
			System.setProperty("kompics.home", new File(userHome + "/.kompics/")
					.getAbsolutePath());
		} 
		KOMPICS_HOME = System.getProperty("kompics.home");
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	
	private static final Logger logger = LoggerFactory.getLogger(Daemon.class);

	private Positive<Network> net = positive(Network.class);
	private Positive<Timer> timer = positive(Timer.class);
	
	private Component mavenLauncher;

	int daemonId;
	private Address self;
	private Address masterAddress;

	private Map<Integer,JobAssembly> loadingJobs = new HashMap<Integer,JobAssembly>();
	private Map<Integer,JobAssembly> loadedJobs = new HashMap<Integer,JobAssembly>();
	private Map<Integer,JobExec> executingJobs = new HashMap<Integer,JobExec>();
	private Map<Integer,JobExec> completedJobs = new HashMap<Integer,JobExec>();

	private Map<Integer,ProcessWrapper> executingProcesses = 
		new ConcurrentHashMap<Integer, ProcessWrapper>();

	private int masterRetryTimeout;
	private int masterRetryCount;
	
	private long indexingPeriod;
	
	private Component fd;
	
	
	public static void main(String[] args) {
		Kompics.createAndStart(Daemon.class, 2);
	}

	public Daemon() {

		fd = create(FailureDetector.class);
		mavenLauncher = create(MavenLauncher.class);
		
		
		subscribe(handleInit, control);
		subscribe(handleJobLoadRequest, net);
		subscribe(handleJobStartRequest, net);
		subscribe(handleShutdown, net);
		subscribe(handleJobStopRequest, net);
	
		subscribe(handleJobAssemblyResponse, mavenLauncher.getNegative(Maven.class));
		subscribe(handleJobExecResponse, mavenLauncher.getNegative(Maven.class));
		
		subscribe(handleShutdownTimeout, timer);
		subscribe(handleIndexerTimeout, timer);
	}

	public Handler<DaemonInit> handleInit = new Handler<DaemonInit>() {
		public void handle(DaemonInit event) {
			daemonId = event.getId();
			self = event.getSelf();
			masterAddress = event.getMasterAddr();
			masterRetryTimeout = event.getMasterRetryTimeout();
			masterRetryCount = event.getMasterRetryCount();
			indexingPeriod = event.getIndexingPeriod();
			
			// set the indexing timer
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
					0, indexingPeriod);
			spt.setTimeoutEvent(new IndexerTimeout(spt));
			trigger(spt, timer);
		}
	};

	
	private Handler<IndexerTimeout> handleIndexerTimeout = new Handler<IndexerTimeout>() {
		public void handle(IndexerTimeout event) {
			logger.debug("Indexer running...");

			// check KOMPICS_HOME for newly downloaded dummy pom projects
			
			
			// set the indexing timer
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
					0, indexingPeriod);
			spt.setTimeoutEvent(new IndexerTimeout(spt));
			trigger(spt, timer);
		}
	};
	
	public Handler<JobAssemblyResponse> handleJobAssemblyResponse = new Handler<JobAssemblyResponse>() {
		public void handle(JobAssemblyResponse event) {

			// if success then remove from loadingJobs, add to loadedJobs
			if (event.getStatus() == JobAssemblyResponse.Status.ASSEMBLED)
			{
				JobAssembly job = loadingJobs.get(event.getJobId());
				loadedJobs.put(event.getJobId(), job);
			}
			// remove job from loadingJobs whatever the result.
			loadingJobs.remove(event.getJobId());
		}
	};

	public Handler<JobExecResponse> handleJobExecResponse = new Handler<JobExecResponse>() {
		public void handle(JobExecResponse event) {
			// if success remove from loadedJobs, add to executingJobs
			if (event.getStatus() == JobExecResponse.Status.SUCCESS)
			{
				JobExec job = executingJobs.get(event.getJobId());
				completedJobs.put(event.getJobId(),job);
			}
			// remove job from loadingJobs whatever the result.
			executingJobs.remove(event.getJobId());
		}
	};
	
	public Handler<JobLoadRequest> handleJobLoadRequest = new Handler<JobLoadRequest>() {
		public void handle(JobLoadRequest event) {
			logger.info("DeployRequest Event Received");

			int id = event.getJobId();
			JobLoadResponse.Status status = JobLoadResponse.Status.LOADING;

			// If msg not a duplicate
			if (loadingJobs.containsKey(id) == false)
			{
				JobAssembly job;
				try {
					job = new JobAssembly(event.getJobId(), event.getRepoId(), event.getRepoUrl(), event
							.getRepoName(), event.getGroupId(), event.getArtifactId(), event
							.getVersion(), event.getMainClass(), event.getArgs());
	
					job.writeToFile(); 
					status = JobLoadResponse.Status.POM_CREATED;
					loadingJobs.put(id, job);
					trigger(job,mavenLauncher.getNegative(Maven.class));
					
				} catch (DummyPomConstructionException e) {
					e.printStackTrace();
					status = JobLoadResponse.Status.FAIL;
				}
			}
			else
			{
				status = JobLoadResponse.Status.DUPLICATE;
			}

			trigger(new JobLoadResponse(id, status, new DaemonAddress(daemonId, event
					.getDestination()), event.getSource()), net);
		}
	};
	
	public Handler<JobStartRequest> handleJobStartRequest = new Handler<JobStartRequest>() {
		public void handle(JobStartRequest event) {
			int id = event.getId();
			Job job = loadedJobs.get(id);
			
			JobStartResponse.Status status = JobStartResponse.Status.SUCCESS;
			if (job == null)
			{
				// see if job is loading, if yes, then wait
				 job = loadingJobs.get(id);
				 if (job == null)
				 {
					 // need to load job first
					 
				 }
				
			}
			else
			{
				JobExec jobExec;
				try {
					jobExec = new JobExec(job,event.getSimulationScenario());
					trigger(jobExec,mavenLauncher.getNegative(Maven.class));
					executingJobs.put(id,jobExec);
				} catch (DummyPomConstructionException e) {
					e.printStackTrace();
					status = JobStartResponse.Status.FAIL;
				}
				
			}
			
			
			JobStartResponse response = new JobStartResponse(id,status, 
					new DaemonAddress(daemonId, event.getDestination()),
					event.getSource());
			trigger(response,net);
		}
	};

//	private JobLoadResponse.Status mvnAssemblyAssembly(Job job)
//	{
//		JobLoadResponse.Status status;
//		int res = forkProcess(job, null, true);
//		switch (res)
//		{
//		case 0: status = JobLoadResponse.Status.ASSEMBLED; break;
//		case -1: status = JobLoadResponse.Status.FAIL; break;
//		default:
//			status = JobLoadResponse.Status.FAIL; break;
//		}
//		return status;
//	}
//
//	private JobStartResponse.Status mvnExecExec(Job job, SimulationScenario scenario)
//	{
//		JobStartResponse.Status status;
//		int res = forkProcess(job, scenario, false);
//		switch (res)
//		{
//		case 0: status = JobStartResponse.Status.SUCCESS; break;
//		case -1: status = JobStartResponse.Status.FAIL; break;
//		default: status = JobStartResponse.Status.FAIL; break;
//		}
//		return status;
//	}
	
	/**
	 * @param id
	 * @param job
	 * @param scenario
	 * @param assembly
	 * @return 0 on success, -1 on failure.
	 */
//	private int forkProcess(Job job, SimulationScenario scenario,
//			boolean assembly)
//	{
//		int res=0;
//		String[] args = {};
//		
//	
//		String classPath = System.getProperty("java.class.path");
//		java.util.List<String> command = new ArrayList<String>();
//		command.add("java");
//		command.add("-classpath");
//		command.add(classPath); // the Slave jar file should be on this path
//		command.add("-Dlog4j.properties=log4j.properties");
//		command.add("-DKOMPICS_HOME="
//				+ KOMPICS_HOME);
//		command.add(job.getMainClass());
//		command.add(job.getGroupId());
//		command.add(job.getArtifactId());
//		String assembleC;
//		assembleC = assembly ? "assemble=true" : "assemble=false"; 
//		command.add(assembleC);
//		command.addAll(job.getArgs());
//
//
//		ProcessBuilder processBuilder = new ProcessBuilder(command);
//		
//		processBuilder.redirectErrorStream(true);
//		Map<String,String> env = processBuilder.environment();
//		env.put("KOMPICS_HOME", KOMPICS_HOME);
//		
//		if (scenario != null)
//		{
//			try {
//				File file = File.createTempFile(SCENARIO_FILENAME, ".bin");
//				ObjectOutputStream oos = new ObjectOutputStream(
//						new FileOutputStream(file));
//				oos.writeObject(scenario);
//				oos.flush();
//				oos.close();
//				env.put(SCENARIO_FILENAME, file.getAbsolutePath());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		try {
//			Process p = processBuilder.start();
//			ProcessWrapper pw = new ProcessWrapper(job.getId(), p);
//			executingProcesses.put(job.getId(), pw);
//			new Thread(pw).start();
//			
//		} catch (IOException e1) {
//			e1.printStackTrace();
//			res = -1;
//		}
//		
//		loadingJobs.remove(job.getId());
//		executingJobs.put(job.getId(), job);
//
//		return res;
//	}
//	
	
	public Handler<DaemonShutdown> handleShutdown = new Handler<DaemonShutdown>() {
		public void handle(DaemonShutdown event) {
			
			int timeout = event.getTimeout();
			ScheduleTimeout st = new ScheduleTimeout(timeout);
			st.setTimeoutEvent(new TimerDaemonShutdown(st));
			trigger(st, timer);				
		}
	};
	
	public Handler<TimerDaemonShutdown> handleShutdownTimeout = new Handler<TimerDaemonShutdown>() {
		public void handle(TimerDaemonShutdown event) {
			System.exit(0);
		}
	};

	
	public final void execute(Class<? extends ComponentDefinition> main) {
		File file = null;
		try {
			file = File.createTempFile("scenario", ".bin");
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(file));
			oos.writeObject(this);
			oos.flush();
			oos.close();
			System.setProperty("scenario", file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
//			cl.run(main.getCanonicalName(), null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	
	public Handler<JobStopRequest> handleJobStopRequest = new Handler<JobStopRequest>() {
		public void handle(JobStopRequest event) {

			int id = event.getJobId();
			JobStopResponse.Status status;
			
			ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null)
			{
				status = JobStopResponse.Status.FAILED_TO_STOP;
			}
			else
			{
				if (pw.destroy() == true) {
					status = JobStopResponse.Status.STOPPED;
				}
				else {
					status = JobStopResponse.Status.ALREADY_STOPPED;
				}
			}
			
			JobStopResponse response = new JobStopResponse(id,status, 
					new DaemonAddress(daemonId, event.getDestination()),
					event.getSource());
			trigger(response,net);
		}
	};
	
	
	public Handler<JobMessageRequest> handleJobMessageRequest = new Handler<JobMessageRequest>() {
		public void handle(JobMessageRequest event) {
			int id = event.getJobId();
			JobMessageResponse.Status status;
			ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null)
			{
				status = JobMessageResponse.Status.STOPPED;
			}
			else
			{
				try {
					pw.input(event.getMsg());
				
					status = JobMessageResponse.Status.SUCCESS;
				}
				catch (IOException e)
				{
					status = JobMessageResponse.Status.FAIL;
				}
			}
			
			JobMessageResponse response = new JobMessageResponse(id,status, 
					new DaemonAddress(daemonId, event.getDestination()),
					event.getSource());
			trigger(response,net);
		}
	};
	
	
//	private ProcessLauncher createProcess(int id, int idx, 
//			String classPath,
//			Class<? extends ComponentDefinition> main,
//			long now, Semaphore semaphore) {
//		ProcessLauncher processLauncher = new ProcessLauncher(classPath,
//				main, "-Dlog4j.properties=log4j.properties", id,
//				idx, this, now, semaphore);
//		return processLauncher;
//	}
	
	/*
	 * 
	 * public void jarDownload(String jarFileName, String artifact) { // String
	 * repo = "http://korsakov.sics.se/maven/snapshotrepository/"; // String
	 * group = "se/sics/kompics/"; // String artifact = "kompics-manual"; //
	 * String version = "0.4.0";
	 * 
	 * String tmpDirectory = System.getProperty("java.io.tmpdir"); File projDir
	 * = new File(tmpDirectory, artifact); projDir.mkdir();
	 * 
	 * URL jarRemote = null; try { jarRemote = new URL(jarFileName); } catch
	 * (MalformedURLException e) { e.printStackTrace(); }
	 * 
	 * File localJarFile = new File(projDir, artifact + ".jar"); try {
	 * localJarFile.createNewFile(); copy(jarRemote.openStream(), new
	 * FileOutputStream(localJarFile)); } catch (IOException e1) {
	 * e1.printStackTrace(); } }
	 * 
	 * 
	 * 
	 * private void copy(InputStream in, OutputStream out) throws IOException {
	 * 
	 * // Transfer bytes from in to out byte[] buf = new byte[1024]; int len;
	 * while ((len = in.read(buf)) > 0) { out.write(buf, 0, len); } out.flush();
	 * in.close(); out.close(); }
	 * 
	 * 
	 * private void BinaryFileDownload() throws Exception { String repo =
	 * "http://korsakov.sics.se/maven/repository/"; String group =
	 * "se/sics/kompics/"; String artifact = "kompics-manual"; String version =
	 * "0.4.0";
	 * 
	 * URL u = new URL(repo + group + artifact + "/" + version + "/" + artifact
	 * + "-" + version + ".jar"); URLConnection uc = u.openConnection(); String
	 * contentType = uc.getContentType(); int contentLength =
	 * uc.getContentLength();
	 * 
	 * System.out.println("Length: " + contentLength);
	 * 
	 * if (contentType.startsWith("text/") || contentLength == -1) { throw new
	 * IOException("This is not a binary file."); } InputStream raw =
	 * uc.getInputStream(); InputStream in = new BufferedInputStream(raw);
	 * byte[] data = new byte[contentLength]; int bytesRead = 0; int offset = 0;
	 * while (offset < contentLength) { bytesRead = in.read(data, offset,
	 * data.length - offset); if (bytesRead == -1) break; offset += bytesRead; }
	 * in.close();
	 * 
	 * if (offset != contentLength) { throw new IOException("Only read " +
	 * offset + " bytes; Expected " + contentLength + " bytes"); }
	 * 
	 * String tmpDirectory = System.getProperty("java.io.tmpdir"); File projDir
	 * = new File(tmpDirectory, artifact); projDir.mkdir();
	 * 
	 * String filename = u.getFile().substring( u.getFile().lastIndexOf('/') +
	 * 1); File outFile = new File(projDir, filename);
	 * 
	 * System.out.println("File: " + outFile.getPath()); FileOutputStream out =
	 * new FileOutputStream(outFile); out.write(data); out.flush(); out.close();
	 * }
	 */
}
