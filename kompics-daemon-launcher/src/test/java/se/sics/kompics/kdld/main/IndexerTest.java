package se.sics.kompics.kdld.main;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.kdld.daemon.JobRemoveRequestMsg;
import se.sics.kompics.kdld.daemon.JobRemoveResponseMsg;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequest;
import se.sics.kompics.kdld.daemon.ListJobsLoadedResponse;
import se.sics.kompics.kdld.daemon.indexer.Index;
import se.sics.kompics.kdld.daemon.indexer.Indexer;
import se.sics.kompics.kdld.daemon.indexer.IndexerInit;
import se.sics.kompics.kdld.daemon.indexer.JobFoundLocally;
import se.sics.kompics.kdld.daemon.maven.Maven;
import se.sics.kompics.kdld.daemon.maven.MavenLauncher;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Job;
import se.sics.kompics.kdld.job.JobAssembly;
import se.sics.kompics.kdld.job.JobAssemblyResponse;
import se.sics.kompics.kdld.job.JobExec;
import se.sics.kompics.kdld.job.JobExecResponse;
import se.sics.kompics.kdld.job.JobExited;
import se.sics.kompics.kdld.job.JobReadFromExecuting;
import se.sics.kompics.kdld.job.JobReadFromExecutingResponse;
import se.sics.kompics.kdld.job.JobRemoveRequest;
import se.sics.kompics.kdld.job.JobRemoveResponse;
import se.sics.kompics.kdld.job.JobStopRequest;
import se.sics.kompics.kdld.job.JobStopResponse;
import se.sics.kompics.kdld.job.JobToDummyPom;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

/**
 * Unit test for simple App.
 */
public class IndexerTest implements Serializable {

	private static final long serialVersionUID = -8704832589507459009L;

	public static Semaphore semaphore = new Semaphore(0);

	public static boolean ASSEMBLY = true;

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
		
		public TestIndexerComponent() {

			if (testObj == null) {
				throw new IllegalStateException(
						"Test object should be set before calling component");
			}

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

					dummy = new JobToDummyPom(11, "se.sics.kompics",
							"kompics-manual", "0.4.2-SNAPSHOT",
							"se.sics.kompics.manual.example1.Root", new ArrayList<String>(),
							"sics-snapshot", "http://kompics.sics.se/maven/snapshotrepository");
					
					dummy.createDummyPomFile();
					
					logger.info("Creating a dummy pom");

					// need to call maven assembly:assembly if the jar hasn't
					// been loaded yet.
					// timeout for a few seconds, if no response then send maven
					// assembly:assembly

					if (ASSEMBLY == true) {
						trigger(new JobAssembly(dummy), mavenLauncher.getPositive(Maven.class));
					}

				} catch (DummyPomConstructionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		};

		public Handler<JobExecResponse> handleJobExecResponse = new Handler<JobExecResponse>() {
			public void handle(JobExecResponse event) {

				logger.info("Received job execResponse from job-id: {} ", event.getJobId());

				// ProcessWrapper pw = event.getProcessWrapper();

				// try {
				
				ScheduleTimeout st = new ScheduleTimeout(5000);
				st.setTimeoutEvent(new ExecReadTimeout(event.getJobId(), st));				
				trigger(st, timer.getPositive(Timer.class));
				
				// String read = event.getProcessWrapper().readLineFromOutput();
				// logger.debug("Read from process:" + read);
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

				// IndexerTest.semaphore.release(1);

			}
		};

		public Handler<ExecReadTimeout> handleExecReadTimeout = new Handler<ExecReadTimeout>() {
			public void handle(ExecReadTimeout event) {
				logger.debug("Trying to read from job: " + event.getJobId());
				trigger(new JobReadFromExecuting(event.getJobId()), mavenLauncher
						.getPositive(Maven.class));

				ScheduleTimeout st = new ScheduleTimeout(2000);
				st.setTimeoutEvent(new JobStopTimeout(event.getJobId(), st));				
				trigger(st, timer.getPositive(Timer.class));			
			}
		};
		
		
		public Handler<JobStopTimeout> handleJobStopTimeout = new Handler<JobStopTimeout>() {
			public void handle(JobStopTimeout event) {
				logger.debug("Trying to stop a job: " + event.getJobId());
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
				}
		};

		public Handler<JobReadFromExecutingResponse> handleJobReadFromExecutingResponse = new Handler<JobReadFromExecutingResponse>() {
			public void handle(JobReadFromExecutingResponse event) {
				logger.info("Read from job id:" + event.getJobId() + " - " + event.getMsg());
			}
		};

		public Handler<JobAssemblyResponse> handleJobAssemblyResponse = new Handler<JobAssemblyResponse>() {
			public void handle(JobAssemblyResponse event) {

				logger.debug("assembly:assembly response for job: " + event.getJobId());

				// if success then remove from loadingJobs, add to loadedJobs
				if (event.getStatus() != JobAssemblyResponse.Status.ASSEMBLED) {
					testObj.fail();
				}

				// SimulationScenario simulationScenario = new
				// SimulationScenario()
				// {
				//
				// private static final long serialVersionUID =
				// -5355642917108165919L;
				//
				// {
				// StochasticProcess p1 = new StochasticProcess() {
				//
				// {
				// }
				// };
				// p1.start();
				// }
				// };
				// Job j = loadedJobs.get(event.getJobId());
				// if (j != null){
				// trigger(new JobExec(j,simulationScenario),
				// mavenLauncher.getPositive(Maven.class));
				// }
				// else
				// {
				// logger.debug("job returned by assemblyResponse was null");
				// }

			}
		};

		public Handler<ListJobsLoadedResponse> handleListJobsLoadedResponse = new Handler<ListJobsLoadedResponse>() {
			public void handle(ListJobsLoadedResponse event) {

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
				if (jobsFound == true) {
					testObj.pass();
				} else {
					testObj.fail();
				}

				Iterator<Job> iter = listJobsLoaded.iterator();

				trigger(new JobExec(iter.next(), scenario), mavenLauncher.getPositive(Maven.class));

				// IndexerTest.semaphore.release(1);
			}
		};

		public Handler<JobFoundLocally> handleJobFoundLocally = new Handler<JobFoundLocally>() {
			public void handle(JobFoundLocally event) {
				int id = event.getId();

				logger.info("Received job found locally.");
				if (loadedJobs.containsKey(id) == false) {
					loadedJobs.put(id, event);
					logger.info("Added job to loaded jobs set.");

					Address addr;
					try {
						addr = new Address(InetAddress.getLocalHost(), 3333, 10);
						trigger(new ListJobsLoadedRequest(1, addr, new DaemonAddress(1, addr)),
								indexer.getPositive(Index.class));
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
	}

	public void fail() {
		org.junit.Assert.assertTrue(false);
	}
}