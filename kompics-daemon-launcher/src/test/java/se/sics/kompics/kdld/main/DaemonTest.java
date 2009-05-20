package se.sics.kompics.kdld.main;

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
import se.sics.kompics.kdld.daemon.Daemon;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.kdld.daemon.DaemonInit;
import se.sics.kompics.kdld.daemon.JobLoadRequestMsg;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequestMsg;
import se.sics.kompics.kdld.daemon.ListJobsLoadedResponseMsg;
import se.sics.kompics.kdld.daemon.indexer.Index;
import se.sics.kompics.kdld.daemon.indexer.IndexShutdown;
import se.sics.kompics.kdld.daemon.indexer.IndexerInit;
import se.sics.kompics.kdld.daemon.indexer.JobFoundLocally;
import se.sics.kompics.kdld.daemon.maven.Maven;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Job;
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
import se.sics.kompics.kdld.util.DaemonConfiguration;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

/**
 * Unit test for simple App.
 */
public class DaemonTest implements Serializable {

	private static final long serialVersionUID = -8704832589507459009L;

	public static Semaphore semaphore = new Semaphore(0);

	public static final int EVENT_COUNT = 1;

	public static SimulationScenario scenario = new SimulationScenario() {
		private static final long serialVersionUID = -5355642917108165919L;

	};

	public DaemonTest() {

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

	public static class TestDaemonComponent extends ComponentDefinition
			implements Serializable {

		private static final long serialVersionUID = -5967918118211382L;

		public Logger logger = LoggerFactory
				.getLogger(TestDaemonComponent.class);

		public Component timer;
		private Component daemon;
		private Component network;

		public Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();

		private static DaemonTest testObj = null;

		private final HashSet<UUID> outstandingTimeouts;

		public TestDaemonComponent() {

			if (testObj == null) {
				throw new IllegalStateException(
						"Test object should be set before calling component");
			}

			outstandingTimeouts = new HashSet<UUID>();

			timer = create(JavaTimer.class);
			daemon = create(Daemon.class);
			network = create(MinaNetwork.class);

			trigger(new MinaNetworkInit(DaemonConfiguration.getPeer0Address()),
					network.getControl());
			connect(daemon.getNegative(Timer.class), timer
					.getPositive(Timer.class));
			connect(daemon.getNegative(Network.class), network
					.getPositive(Network.class));
			DaemonInit dInit = new DaemonInit(
					DaemonConfiguration.getDaemonId(), DaemonConfiguration
							.getPeer0Address(), DaemonConfiguration
							.getMasterAddress(), DaemonConfiguration
							.getDaemonRetryPeriod(), DaemonConfiguration
							.getDaemonRetryCount(), DaemonConfiguration
							.getDaemonIndexingPeriod());
			trigger(dInit, daemon.getControl());

			subscribe(handleLoadJobResponse, daemon.getPositive(Maven.class));
			subscribe(handleJobStartResponse, daemon.getPositive(Maven.class));
			subscribe(handleJobReadFromExecutingResponse, daemon
					.getPositive(Maven.class));

			subscribe(handleExecReadTimeout, timer.getPositive(Timer.class));
			subscribe(handleJobStopTimeout, timer.getPositive(Timer.class));
			subscribe(handleJobStopResponse, daemon.getPositive(Maven.class));
			subscribe(handleJobExited, daemon.getPositive(Maven.class));

			subscribe(handleStart, control);

		}

		public static void setTestObj(DaemonTest testObj) {
			TestDaemonComponent.testObj = testObj;
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {
				logger.info("Starting TestIndexer");

				Address src = null;
				DaemonAddress dest = null;

				JobLoadRequestMsg msg = new JobLoadRequestMsg(11,
						"se.sics.kompics", "kompics-manual", "0.4.2-SNAPSHOT",
						"sics-snapshot",
						"http://kompics.sics.se/maven/snapshotrepository",
						"se.sics.kompics.manual.example1.Root",
						new ArrayList<String>(), src, dest);

				logger.info("JobLoadRequestMsg being sent..");

				trigger(msg, daemon.getPositive(Maven.class));

			}
		};

		public Handler<JobExecResponse> handleJobStartResponse = new Handler<JobExecResponse>() {
			public void handle(JobExecResponse event) {

				logger.info("Received job execResponse from job-id: {} ", event
						.getJobId());

				ScheduleTimeout st = new ScheduleTimeout(5000);
				ExecReadTimeout execReadTimeout = new ExecReadTimeout(event
						.getJobId(), st);
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
				trigger(new JobReadFromExecuting(event.getJobId()), daemon
						.getPositive(Maven.class));

				ScheduleTimeout st = new ScheduleTimeout(2000);
				st.setTimeoutEvent(new JobStopTimeout(event.getJobId(), st));
				trigger(st, timer.getPositive(Timer.class));
			}
		};

		public Handler<JobStopTimeout> handleJobStopTimeout = new Handler<JobStopTimeout>() {
			public void handle(JobStopTimeout event) {
				logger.warn("Trying to stop a job: " + event.getJobId());
				trigger(new JobStopRequest(event.getJobId()), daemon
						.getPositive(Maven.class));

			}
		};

		public Handler<JobExited> handleJobExited = new Handler<JobExited>() {
			public void handle(JobExited event) {
				logger.debug("Job exited: " + event.getJobId());

				// trigger(new JobRemoveRequest(dummy),
				// daemon.getPositive(Maven.class));
			}
		};

		public Handler<JobStopResponse> handleJobStopResponse = new Handler<JobStopResponse>() {
			public void handle(JobStopResponse event) {
				logger.debug("Job stopped : " + event.getJobId() + " : "
						+ event.getStatus() + " : " + event.getMsg());

				// trigger(new IndexShutdown(),
				// indexer.getPositive(Index.class));
				// trigger(new Stop(), indexer.getControl());

				TestDaemonComponent.testObj.pass();
			}
		};

		public Handler<JobReadFromExecutingResponse> handleJobReadFromExecutingResponse = new Handler<JobReadFromExecutingResponse>() {
			public void handle(JobReadFromExecutingResponse event) {
				logger.info("Read from job id:" + event.getJobId() + " - "
						+ event.getMsg());
			}
		};

		public Handler<JobAssemblyResponse> handleLoadJobResponse = new Handler<JobAssemblyResponse>() {
			public void handle(JobAssemblyResponse event) {

				logger.debug("assembly:assembly response for job: "
						+ event.getJobId());

				// if success then remove from loadingJobs, add to loadedJobs
				if (event.getStatus() != JobAssemblyResponse.Status.ASSEMBLED) {
					testObj.fail(true);
				}

			}
		};

		public Handler<ListJobsLoadedResponseMsg> handleListJobsLoadedResponse = new Handler<ListJobsLoadedResponseMsg>() {
			public void handle(ListJobsLoadedResponseMsg event) {

				Set<Job> listJobsLoaded = event.getSetJobs();

				boolean jobsFound = true;
				for (Job j : listJobsLoaded) {
					if (loadedJobs.containsKey(j.getId()) == true) {
						logger.info("Found already loaded job: " + j.getId());
					} else {
						logger
								.info("ERROR: Found job not loaded: "
										+ j.getId());
						jobsFound = false;
					}
				}
				if (jobsFound == false) {
					testObj.fail(false);
				}

				Iterator<Job> iter = listJobsLoaded.iterator();

				trigger(new JobExec(iter.next(), scenario), daemon
						.getPositive(Maven.class));

				// IndexerTest.semaphore.release(1);
			}
		};

		public Handler<JobFoundLocally> handleJobFoundLocally = new Handler<JobFoundLocally>() {
			public void handle(JobFoundLocally event) {
				int id = event.getId();

				logger.info("Received job {} found locally.", id);
				if (loadedJobs.containsKey(id) == false) {
					loadedJobs.put(id, event);
					logger.info("Added job {} to loaded jobs set.", id);

					// Address addr;
					// try {
					// addr = new Address(InetAddress.getLocalHost(), 3333, 10);
					// trigger(new ListJobsLoadedRequestMsg(1, addr, new
					// DaemonAddress(1, addr)),
					// indexer.getPositive(Index.class));
					// } catch (UnknownHostException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }

				}

			}
		};

		private void removeJob(int jobId) {
			Job job = loadedJobs.get(jobId);
			trigger(new JobRemoveRequest(job), daemon.getNegative(Maven.class));
		}

		public Handler<JobRemoveResponse> handleJobRemoveResponse = new Handler<JobRemoveResponse>() {
			public void handle(JobRemoveResponse event) {

				logger.info("Job remove response was:" + event.getMsg() + " - "
						+ event.getStatus());

				// trigger(new IndexShutdown(),
				// indexer.getNegative(Index.class));
				//				
				// trigger(new Stop(), indexer.getControl());
			}
		};
	}

	@org.junit.Test
	public void testIndexer() {

		TestDaemonComponent.setTestObj(this);

		Kompics.createAndStart(TestDaemonComponent.class, 2);

		try {
			DaemonTest.semaphore.acquire(EVENT_COUNT);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Kompics.shutdown();
	}

	public void pass() {
		org.junit.Assert.assertTrue(true);
		DaemonTest.semaphore.release();
	}

	public void fail(boolean release) {
		org.junit.Assert.assertTrue(false);
		if (release == true) {
			DaemonTest.semaphore.release();
		}
	}
}