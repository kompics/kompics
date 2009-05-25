package se.sics.kompics.wan.main;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.daemon.DaemonAddress;
import se.sics.kompics.wan.daemon.ListJobsLoadedRequestMsg;
import se.sics.kompics.wan.daemon.indexer.Index;
import se.sics.kompics.wan.daemon.indexer.IndexShutdown;
import se.sics.kompics.wan.daemon.indexer.Indexer;
import se.sics.kompics.wan.daemon.indexer.IndexerInit;
import se.sics.kompics.wan.daemon.indexer.JobsFound;
import se.sics.kompics.wan.daemon.maven.Maven;
import se.sics.kompics.wan.daemon.maven.MavenLauncher;
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
import se.sics.kompics.wan.job.JobToDummyPom;

/**
 * Unit test for simple App.
 */
public class IndexerTest implements Serializable {

	private static final long serialVersionUID = -8704832589507459009L;

	public static Semaphore semaphore = new Semaphore(0);

	public static final int EVENT_COUNT = 1;

	public static SimulationScenario scenario = new SimulationScenario() {
		private static final long serialVersionUID = -5355642917108165919L;

	};

	public IndexerTest() {

	}

	
	public static class JobStopTimeout extends Timeout {
		private final int jobId;

		public JobStopTimeout(int jobId, ScheduleTimeout request) {
			super(request);
			this.jobId = jobId;
		}

		public int getJobId() {
			return jobId;
		}
	}
	
	public static class ExecReadTimeout extends Timeout {
		private final int jobId;

		public ExecReadTimeout(int jobId, ScheduleTimeout request) {
			super(request);
			this.jobId = jobId;
		}

		public int getJobId() {
			return jobId;
		}
	}

	public static class TestIndexerComponent extends ComponentDefinition implements Serializable {

		private static final long serialVersionUID = -5967918118211382L;

		public Logger logger = LoggerFactory.getLogger(TestIndexerComponent.class);

		public Component indexer;
		public Component timer;
		private Component mavenLauncher;

		public Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();

		private static IndexerTest testObj = null;

		private JobToDummyPom dummy;
		
		private final HashSet<UUID> outstandingTimeouts;
		
		public TestIndexerComponent() {

			if (testObj == null) {
				throw new IllegalStateException(
						"Test object should be set before calling component");
			}

			outstandingTimeouts = new HashSet<UUID>();
			
			indexer = create(Indexer.class);
			timer = create(JavaTimer.class);
			mavenLauncher = create(MavenLauncher.class);

			connect(indexer.getNegative(Timer.class), timer.getPositive(Timer.class));
			

			subscribe(handleListJobsLoadedResponse, indexer.getPositive(Index.class));
			subscribe(handleJobFoundLocally, indexer.getPositive(Index.class));

			subscribe(handleJobAssemblyResponse, mavenLauncher.getPositive(Maven.class));
			subscribe(handleJobExecResponse, mavenLauncher.getPositive(Maven.class));
			subscribe(handleJobReadFromExecutingResponse, mavenLauncher.getPositive(Maven.class));

			subscribe(handleExecReadTimeout, timer.getPositive(Timer.class));
			subscribe(handleJobStopTimeout, timer.getPositive(Timer.class));
			subscribe(handleJobStopResponse, mavenLauncher.getPositive(Maven.class));
			subscribe(handleJobExited, mavenLauncher.getPositive(Maven.class));
			
			subscribe(handleStart, control);

			trigger(new IndexerInit(5000), indexer.getControl());

			logger.info("Initializing the Indexer");
		}

		public static void setTestObj(IndexerTest testObj) {
			TestIndexerComponent.testObj = testObj;
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {
				logger.info("Starting TestIndexer");

				try {

					dummy = new JobToDummyPom("se.sics.kompics",
							"kompics-manual", "0.4.2-SNAPSHOT",
							"se.sics.kompics.manual.example1.Root", new ArrayList<String>(),
							"sics-snapshot", "http://kompics.sics.se/maven/snapshotrepository");
					
					dummy.createDummyPomFile();
					
					logger.info("Creating a dummy pom");

					// need to call maven assembly:assembly if the jar hasn't
					// been loaded yet.
					// timeout for a few seconds, if no response then send maven
					// assembly:assembly

					trigger(new JobLoadRequest(dummy), mavenLauncher.getPositive(Maven.class));

				} catch (DummyPomConstructionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		};

		public Handler<JobStartResponse> handleJobExecResponse = new Handler<JobStartResponse>() {
			public void handle(JobStartResponse event) {

				logger.info("Received job execResponse from job-id: {} ", event.getJobId());

				
				ScheduleTimeout st = new ScheduleTimeout(5000);
				ExecReadTimeout execReadTimeout = new ExecReadTimeout(event.getJobId(), st);
				st.setTimeoutEvent(execReadTimeout);	
				
				UUID timerId = execReadTimeout.getTimeoutId();
				outstandingTimeouts.add(timerId);
				
				trigger(st, timer.getPositive(Timer.class));
				
			}
		};

		public Handler<ExecReadTimeout> handleExecReadTimeout = new Handler<ExecReadTimeout>() {
			public void handle(ExecReadTimeout event) {
				
				if (!outstandingTimeouts.contains(event.getTimeoutId())) {
					return;
				}
				outstandingTimeouts.remove(event.getTimeoutId());

				
				logger.debug("Retrying to read from job: " + event.getJobId());
				trigger(new JobReadFromExecutingRequest(event.getJobId()), mavenLauncher
						.getPositive(Maven.class));

				ScheduleTimeout st = new ScheduleTimeout(2000);
				st.setTimeoutEvent(new JobStopTimeout(event.getJobId(), st));				
				trigger(st, timer.getPositive(Timer.class));			
			}
		};
		
		
		public Handler<JobStopTimeout> handleJobStopTimeout = new Handler<JobStopTimeout>() {
			public void handle(JobStopTimeout event) {
				logger.warn("Trying to stop a job: " + event.getJobId());
				trigger(new JobStopRequest(event.getJobId()), mavenLauncher.getPositive(Maven.class));
				
				
			}
		};

		public Handler<JobExited> handleJobExited = new Handler<JobExited>() {
			public void handle(JobExited event) {
				logger.debug("Job exited: " + event.getJobId());
				
				trigger(new JobRemoveRequest(dummy), mavenLauncher.getPositive(Maven.class));
			}
		};

		
		public Handler<JobStopResponse> handleJobStopResponse = new Handler<JobStopResponse>() {
			public void handle(JobStopResponse event) {
				logger.debug("Job stopped : " + event.getJobId() + " : " + event.getStatus() +
						" : " + event.getMsg());
				
				trigger(new IndexShutdown(), indexer.getPositive(Index.class));
				trigger(new Stop(), indexer.getControl());

				TestIndexerComponent.testObj.pass();
			}
		};

		public Handler<JobReadFromExecutingResponse> handleJobReadFromExecutingResponse = new Handler<JobReadFromExecutingResponse>() {
			public void handle(JobReadFromExecutingResponse event) {
				logger.info("Read from job id:" + event.getJobId() + " - " + event.getMsg());
			}
		};

		public Handler<JobLoadResponse> handleJobAssemblyResponse = new Handler<JobLoadResponse>() {
			public void handle(JobLoadResponse event) {

				logger.debug("assembly:assembly response for job: " + event.getJobId());

				// if success then remove from loadingJobs, add to loadedJobs
				if (event.getStatus() != JobLoadResponse.Status.ASSEMBLED) {
					testObj.fail(true);
				}

			}
		};

		public Handler<JobsFound> handleListJobsLoadedResponse = new Handler<JobsFound>() {
			public void handle(JobsFound event) {

				Set<Job> listJobsLoaded = event.getSetJobs();

				boolean jobsFound = true;
				for (Job j : listJobsLoaded) {
					if (loadedJobs.containsKey(j.getId()) == true) {
						logger.info("Found already loaded job: " + j.getId());
					} else {
						logger.info("ERROR: Found job not loaded: " + j.getId());
						jobsFound = false;
					}
				}
				if (jobsFound == false) {
					testObj.fail(false);
				}

				Iterator<Job> iter = listJobsLoaded.iterator();

				trigger(new JobStartRequest(1, 1, iter.next(), scenario), mavenLauncher.getPositive(Maven.class));

				// IndexerTest.semaphore.release(1);
			}
		};

		public Handler<JobsFound> handleJobFoundLocally = new Handler<JobsFound>() {
			public void handle(JobsFound event) {
				
				for (Job job : event.getSetJobs())
				{
					int id = job.getId();
	
					logger.info("Received job {} found locally.", id);
					if (loadedJobs.containsKey(id) == false) {
						loadedJobs.put(id, job);
						logger.info("Added job {} to loaded jobs set.", id);
	
						Address addr;
						try {
							addr = new Address(InetAddress.getLocalHost(), 3333, 10);
							trigger(new ListJobsLoadedRequestMsg(addr, new DaemonAddress(1, addr)),
									indexer.getPositive(Index.class));
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
	
					}
				}

			}
		};

		private void removeJob(int jobId)
		{
				Job job = loadedJobs.get(jobId);
				trigger(new JobRemoveRequest(job), mavenLauncher.getNegative(Maven.class));
		}

		public Handler<JobRemoveResponse> handleJobRemoveResponse = new Handler<JobRemoveResponse>() {
			public void handle(JobRemoveResponse event) {
				
				logger.info("Job remove response was:" + event.getMsg() + " - " + event.getStatus());
				
				trigger(new IndexShutdown(), indexer.getNegative(Index.class));
				
				trigger(new Stop(), indexer.getControl());
			}
		};
	}

	@org.junit.Test
	public void testIndexer() {

		TestIndexerComponent.setTestObj(this);

		Kompics.createAndStart(TestIndexerComponent.class, 2);

		try {
			IndexerTest.semaphore.acquire(EVENT_COUNT);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Kompics.shutdown();
	}

	public void pass() {
		org.junit.Assert.assertTrue(true);
		IndexerTest.semaphore.release();
	}

	public void fail(boolean release) {
		org.junit.Assert.assertTrue(false);
		if (release == true) {
			IndexerTest.semaphore.release();
		}
	}
}