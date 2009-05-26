package se.sics.kompics.wan.daemon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.daemon.indexer.Index;
import se.sics.kompics.wan.daemon.indexer.Indexer;
import se.sics.kompics.wan.daemon.indexer.IndexerInit;
import se.sics.kompics.wan.daemon.indexer.JobsFound;
import se.sics.kompics.wan.daemon.indexer.ListJobsLoadedResponse;
import se.sics.kompics.wan.daemon.masterclient.MasterClient;
import se.sics.kompics.wan.daemon.masterclient.MasterClientInit;
import se.sics.kompics.wan.daemon.masterclient.MasterClientP;
import se.sics.kompics.wan.daemon.maven.Maven;
import se.sics.kompics.wan.daemon.maven.MavenLauncher;
import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.job.JobExited;
import se.sics.kompics.wan.job.JobLoadRequest;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobReadFromExecutingResponse;
import se.sics.kompics.wan.job.JobRemoveRequest;
import se.sics.kompics.wan.job.JobRemoveResponse;
import se.sics.kompics.wan.job.JobStartRequest;
import se.sics.kompics.wan.job.JobStartResponse;
import se.sics.kompics.wan.master.ConnectMasterRequest;
import se.sics.kompics.wan.master.ConnectMasterResponse;
import se.sics.kompics.wan.master.DisconnectMasterRequest;
import se.sics.kompics.wan.master.MasterConfiguration;

public class Daemon extends ComponentDefinition {

	public static final String KOMPICS_HOME;

	public static final String MAVEN_REPO_HOME;

	public static final String MAVEN_HOME;

	public static final String SCENARIO_FILENAME = "scenario";

	public static final String POM_FILENAME = "pom.xml";
	
	private static final Logger logger = LoggerFactory.getLogger(Daemon.class);

	static {
		String kHome = System.getProperty("kompics.home");
		String userHome = System.getProperty("user.home");
		if (userHome != null && kHome == null) {
			System.setProperty("kompics.home", new File(userHome + "/.kompics/").getAbsolutePath());
		} else if (userHome == null && kHome == null) {
			throw new IllegalStateException(
					"kompics.home and user.home environment variables not set.");
		}
		KOMPICS_HOME = System.getProperty("kompics.home");

		if (new File(Daemon.KOMPICS_HOME).exists() == false) {
			if (new File(Daemon.KOMPICS_HOME).mkdirs() == false) {
				logger.warn("Could not create directory: "
						+ Daemon.KOMPICS_HOME);
			}
		}

		String mavenHome = System.getProperty("maven.home");
		if (mavenHome == null) {
			System.setProperty("maven.home", new File(userHome + "/.m2/").getAbsolutePath());
		}
		MAVEN_HOME = System.getProperty("maven.home");

		if (new File(Daemon.MAVEN_HOME).exists() == false) {
			if ((new File(Daemon.MAVEN_HOME).mkdirs()) == false) {
				logger.warn("Couldn't set directory for Maven Home: "
						+ Daemon.MAVEN_HOME + "\nCheck file permissions for this directory.");
			}
		}

		String mavenRepoHome = System.getProperty("maven.repo");
		if (mavenRepoHome == null) {
			System.setProperty("maven.repo", new File(MAVEN_HOME + "/repository/")
					.getAbsolutePath());
		}
		MAVEN_REPO_HOME = System.getProperty("maven.repo");
		if (new File(Daemon.MAVEN_REPO_HOME).exists() == false) {
			if ((new File(Daemon.MAVEN_REPO_HOME).mkdirs()) == false) {
				logger.warn("Couldn't directory for Maven Home: "
						+ Daemon.MAVEN_REPO_HOME + "\nCheck file permissions for this directory.");
			}
		}
	}

	private Positive<Network> net = positive(Network.class);
	private Positive<Timer> timer = positive(Timer.class);
	
	private Component mavenLauncher;
	private Component indexer;
	private Component masterClient;
	
	private DaemonAddress self;
	private Address masterAddress;

	private Map<Integer, Job> loadingJobs = new HashMap<Integer, Job>();
	private Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();
	private Map<Integer, JobStartRequest> executingJobs = new HashMap<Integer, JobStartRequest>();
	private Map<Integer, JobStartRequest> completedJobs = new HashMap<Integer, JobStartRequest>();

	private Map<Integer, MavenLauncher.ProcessWrapper> executingProcesses = new ConcurrentHashMap<Integer, MavenLauncher.ProcessWrapper>();

	private boolean connectedToMaster = false;

	public Daemon() {

		mavenLauncher = create(MavenLauncher.class);
		indexer = create(Indexer.class);
		masterClient = create(MasterClient.class); 

		subscribe(handleInit, control);
		subscribe(handleStart, control);
		
		subscribe(handleJobLoadRequest, net);
		subscribe(handleJobStartRequest, net);
		subscribe(handleShutdown, net);
		subscribe(handleJobStopRequest, net);
		subscribe(handleListJobsLoadedRequest, net);

		subscribe(handleJobLoadResponse, mavenLauncher.getPositive(Maven.class));
		subscribe(handleJobStartResponse, mavenLauncher.getPositive(Maven.class));
		subscribe(handleJobRemoveResponse, mavenLauncher.getPositive(Maven.class));
		subscribe(handleJobExited, mavenLauncher.getPositive(Maven.class));
		subscribe(handleJobReadFromExecutingResponse, mavenLauncher.getPositive(Maven.class));
		
		subscribe(handleJobRemoveRequestMsg, net);
		
		subscribe(handleShutdownTimeout, timer);
		subscribe(handleTimerDaemonShutdown, timer);
		
		subscribe(handleJobsFound, indexer.getPositive(Index.class));
	}

	public Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			// XXX start timer to retry connect to Master
			trigger(new ConnectMasterRequest(), masterClient.getPositive(MasterClientP.class));
		}
	};
	
	public Handler<JobReadFromExecutingResponse> handleJobReadFromExecutingResponse = 
		new Handler<JobReadFromExecutingResponse>() {
		public void handle(JobReadFromExecutingResponse event) {
			
			logger.info("Read from job {} :", event.getJobId());
			trigger(new JobReadFromExecutingResponseMsg(event, self, masterAddress), net);
		}
	};
	
	public Handler<ConnectMasterResponse> handleConnectMasterResponse = new Handler<ConnectMasterResponse>() {
		public void handle(ConnectMasterResponse event) {
			
			if (event.isSucceeded() == true) {
				logger.info("Successful connection to master from {}", self);
				connectedToMaster = true;
			}
			else
			{
				logger.warn("Failed connection to master from {}", self);
			}
		}
	};

	public Handler<DisconnectMasterRequest> handleDisconnectMasterRequest = 
		new Handler<DisconnectMasterRequest>() {
		public void handle(DisconnectMasterRequest event) {
			
			logger.info("Disconnect request sending to master...");
			
			trigger(event, masterClient.getPositive(MasterClientP.class));
		}
	};

	
	public Handler<DaemonInit> handleInit = new Handler<DaemonInit>() {
		public void handle(DaemonInit event) {
			self = new DaemonAddress(event.getId(), event.getSelf());
			masterAddress = event.getMasterAddr();
			
			MasterConfiguration mc = new MasterConfiguration(masterAddress, 
					event.getCacheEvictAfter(), event.getMasterRetryPeriod(),
					event.getMasterRetryCount(), event.getClientKeepAlivePeriod(),
					event.getClientWebPort());
			
			trigger(new IndexerInit(event.getIndexingPeriod()), indexer.getControl());

			trigger(new MasterClientInit(self, mc), 
					masterClient.getControl());
		}
	};

	public Handler<JobLoadResponse> handleJobLoadResponse = new Handler<JobLoadResponse>() {
		public void handle(JobLoadResponse event) {
			logger.info("JobLoadResponse {} received from MavenLauchner", event.getJobId());

			// if success then remove from loadingJobs, add to loadedJobs
			if (event.getStatus() == JobLoadResponse.Status.ASSEMBLED) {
				Job job = loadingJobs.get(event.getJobId());
				loadedJobs.put(event.getJobId(), job);
			}
			// remove job from loadingJobs whatever the result.
			loadingJobs.remove(event.getJobId());
			
			trigger(new JobLoadResponseMsg(event, self, masterAddress), net);
		}
	};

	public Handler<JobStartResponse> handleJobStartResponse = new Handler<JobStartResponse>() {
		public void handle(JobStartResponse event) {
			// if success, add to executingJobs
			if (event.getStatus() != JobStartResponse.Status.SUCCESS) {
				executingJobs.remove(event.getJobId());				
			}
		
			trigger(new JobStartResponseMsg(event, self, masterAddress), net);
		}
	};

	public Handler<JobLoadRequestMsg> handleJobLoadRequest = new Handler<JobLoadRequestMsg>() {
		public void handle(JobLoadRequestMsg event) {
			logger.info("Job {} Load Request Received for " + event.getGroupId() + "."
					+ event.getArtifactId() + "-" + event.getVersion(), event.getJobId());

			int id = event.getJobId();
			JobLoadResponse.Status status;

			// If msg not a duplicate
			if (loadingJobs.containsKey(id) == false) {
				// if job not already loaded
				if (loadedJobs.containsKey(id) == false)
				{
					JobLoadRequest job;
					try {
						job = new JobLoadRequest(event.getGroupId(), event
								.getArtifactId(), event.getVersion(), event.getMainClass(), event
								.getArgs(), event.getRepoId(), event.getRepoUrl());
	
						job.createDummyPomFile();
						status = JobLoadResponse.Status.POM_CREATED;
						loadingJobs.put(id, job);
						trigger(job, mavenLauncher.getPositive(Maven.class));
	
					} catch (DummyPomConstructionException e) {
						// XXX store stack trace and send back in msg
//						String msg = e.printStackTrace();
						status = JobLoadResponse.Status.FAIL;
						trigger(new JobLoadResponseMsg(id, status, self, event.getSource()), net);
					}
				}
			} else {
				status = JobLoadResponse.Status.DUPLICATE;
				trigger(new JobLoadResponseMsg(id, status, self, event.getSource()), net);
			}

			
		}
	};

	public Handler<JobStartRequestMsg> handleJobStartRequest = new Handler<JobStartRequestMsg>() {
		public void handle(JobStartRequestMsg event) {
			int jobId = event.getJobId();
			int slaveId = event.getSlaveId();
			
			Job job = loadedJobs.get(jobId);

			JobStartResponse.Status status = JobStartResponse.Status.SUCCESS;
			if (job == null) {
				// see if job is loading, if yes, then wait
				job = loadingJobs.get(jobId);
				if (job == null) {
					// need to load job first
					status = JobStartResponse.Status.NOT_LOADED;
				}
				JobStartResponseMsg response = 
					new JobStartResponseMsg(jobId, slaveId, status, self, event.getSource());
				trigger(response, net);
			} else {
				JobStartRequest jobExec = new JobStartRequest(event.getSlaveId(), 
						event.getNumDaemons(), job, event.getSimulationScenario());
				trigger(jobExec, mavenLauncher.getPositive(Maven.class));
				executingJobs.put(jobId, jobExec);
			}

		}
	};


	public Handler<DaemonShutdownMsg> handleShutdown = new Handler<DaemonShutdownMsg>() {
		public void handle(DaemonShutdownMsg event) {

			int timeout = event.getTimeout();
			ScheduleTimeout st = new ScheduleTimeout(timeout);
			st.setTimeoutEvent(new TimerDaemonShutdown(st));
			trigger(st, timer);			
		}
	};
	
	public Handler<JobExited> handleJobExited = new Handler<JobExited>() {
		public void handle(JobExited event) {

			logger.info("Job {} has exited with status {}", event.getJobId(), event.getStatus());
			
			JobStartRequest job = executingJobs.remove(event.getJobId());
			completedJobs.put(job.getId(), job);
			
			JobExitedMsg e = new JobExitedMsg(event, self, masterAddress);
			trigger(e, net);			
		}
	};

	public Handler<TimerDaemonShutdown> handleTimerDaemonShutdown = new Handler<TimerDaemonShutdown>() {
		public void handle(TimerDaemonShutdown event) {

			destroy(masterClient);
			destroy(indexer);
			destroy(mavenLauncher);

		}
	};
	

	
	public Handler<JobRemoveRequestMsg> handleJobRemoveRequestMsg = new Handler<JobRemoveRequestMsg>() {
		public void handle(JobRemoveRequestMsg event) {
			
			Job job = loadedJobs.get(event.getJobId());
			if (job == null)
			{
				trigger (new JobRemoveResponseMsg(event.getJobId(), JobRemoveResponseMsg.Status.NOT_LOADED,
						self, masterAddress), net);
			}
			else
			{
				trigger(new JobRemoveRequest(job), mavenLauncher.getNegative(Maven.class));
			}
		}
	};
	
	
	public Handler<JobRemoveResponse> handleJobRemoveResponse = new Handler<JobRemoveResponse>() {
		public void handle(JobRemoveResponse event) {
			// Assuming that the reply is sent to the Master here.
			trigger(new JobRemoveResponseMsg(event.getJobId(), event.getStatus(),
					event.getMsg(), self, masterAddress), net);
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
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(this);
			oos.flush();
			oos.close();
			System.setProperty("scenario", file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			// cl.run(main.getCanonicalName(), null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public Handler<JobStopRequestMsg> handleJobStopRequest = new Handler<JobStopRequestMsg>() {
		public void handle(JobStopRequestMsg event) {

			int id = event.getJobId();
			JobStopResponseMsg.Status status;

			MavenLauncher.ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null) {
				status = JobStopResponseMsg.Status.FAILED_TO_STOP;
			} else {
				if (pw.destroy() == true) {
					status = JobStopResponseMsg.Status.STOPPED;
				} else {
					status = JobStopResponseMsg.Status.ALREADY_STOPPED;
				}
			}

			JobStopResponseMsg response = new JobStopResponseMsg(id, status, self, event.getSource());
			trigger(response, net);
		}
	};

	public Handler<ListJobsLoadedRequestMsg> handleListJobsLoadedRequest = new Handler<ListJobsLoadedRequestMsg>() {
		public void handle(ListJobsLoadedRequestMsg event) {

			Set<Job> jobsLoaded = new HashSet<Job>(loadedJobs.values());
			storeJobs(jobsLoaded);
			
			trigger(new JobsFoundMsg(jobsLoaded, self, event.getSource()), net);
		}
	};
	
	private void storeJobs(Set<Job> jobsLoaded)
	{
		for (Job job : jobsLoaded)
		{
			loadedJobs.put(job.getId(), job);
		}
	}

	public Handler<ListJobsLoadedResponse> handleListJobsLoadedResponse = new Handler<ListJobsLoadedResponse>() {
		public void handle(ListJobsLoadedResponse event) {
			logger.debug("ListJobsLoadedResponse received");
			
			Set<Job> jobsLoaded = new HashSet<Job>(loadedJobs.values());
			storeJobs(jobsLoaded);
			
			trigger(new JobsFoundMsg(jobsLoaded, self, masterAddress), net);
		}
	};
	
	public Handler<JobsFound> handleJobsFound = new Handler<JobsFound>() {
		public void handle(JobsFound event) {
			logger.debug("{} JobsFound and adding them to Daemon cache.", event.getSetJobs().size());
			
			for (Job job : event.getSetJobs())
			{
				loadedJobs.put(job.getId(), job);
			}
			
			// Could send event to Master indicating job found here
			// Fire-and-forget, as master can query list of jobs
			trigger(new JobsFoundMsg(event.getSetJobs(), self, masterAddress), net);
		}
	};

	

	public Handler<JobMessageRequest> handleJobMessageRequest = new Handler<JobMessageRequest>() {
		public void handle(JobMessageRequest event) {
			int id = event.getJobId();
			JobMessageResponse.Status status;
			MavenLauncher.ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null) {
				status = JobMessageResponse.Status.STOPPED;
			} else {
				try {
					pw.input(event.getMsg());

					status = JobMessageResponse.Status.SUCCESS;
				} catch (IOException e) {
					status = JobMessageResponse.Status.FAIL;
				}
			}

			JobMessageResponse response = new JobMessageResponse(id, status, self, event
					.getSource());
			trigger(response, net);
		}
	};

}
