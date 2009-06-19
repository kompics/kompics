package se.sics.kompics.wan.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.DaemonConfiguration;
import se.sics.kompics.wan.config.MasterAddressConfiguration;
import se.sics.kompics.wan.daemon.Daemon;
import se.sics.kompics.wan.daemon.DaemonAddress;
import se.sics.kompics.wan.daemon.DaemonInit;
import se.sics.kompics.wan.daemon.JobExitedMsg;
import se.sics.kompics.wan.daemon.JobLoadRequestMsg;
import se.sics.kompics.wan.daemon.JobLoadResponseMsg;
import se.sics.kompics.wan.daemon.JobReadFromExecutingRequestMsg;
import se.sics.kompics.wan.daemon.JobReadFromExecutingResponseMsg;
import se.sics.kompics.wan.daemon.JobStartRequestMsg;
import se.sics.kompics.wan.daemon.JobStartResponseMsg;
import se.sics.kompics.wan.daemon.JobStopRequestMsg;
import se.sics.kompics.wan.daemon.JobStopResponseMsg;
import se.sics.kompics.wan.daemon.JobsFoundMsg;
import se.sics.kompics.wan.daemon.ListJobsLoadedRequestMsg;
import se.sics.kompics.wan.daemon.maven.MavenPort;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobRemoveRequest;
import se.sics.kompics.wan.job.JobRemoveResponse;
import se.sics.kompics.wan.job.JobStartResponse;
import se.sics.kompics.wan.job.JobStopResponse;

/**
 * Unit test for simple App.
 */
public class DaemonTest implements Serializable {

	private static final long serialVersionUID = -8704832589507459009L;

	private static final boolean HIDE_MAVEN_EMBEDDER_OUTPUT = true;
	
	private static Semaphore semaphore = new Semaphore(0);

	private static final int EVENT_COUNT = 1;

	static SimulationScenario scenario = new SimulationScenario() {

		private static final long serialVersionUID = -111714340642367999L;
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

	public static class TestDaemonComponent extends ComponentDefinition implements Serializable {

		private static final long serialVersionUID = -5967918118211382L;

		public Logger logger = LoggerFactory.getLogger(TestDaemonComponent.class);

		private Component timer;
		private Component daemon;
		private Component network;

		public Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();

		private static DaemonTest testObj = null;

		private final HashSet<UUID> outstandingTimeouts;

		private Address src = null;

		private DaemonAddress dest = null;

		final class MessageDestinationFilter extends ChannelFilter<Message, Address> {
			public MessageDestinationFilter(Address address) {
				super(Message.class, address, true);
			}

			public Address getValue(Message event) {
				return event.getDestination();
			}
		}

		public TestDaemonComponent() {

			if (testObj == null) {
				throw new IllegalStateException(
						"Test object should be set before calling component");
			}

			outstandingTimeouts = new HashSet<UUID>();

			timer = create(JavaTimer.class);
			daemon = create(Daemon.class);
			network = create(MinaNetwork.class);

			src = new Address(DaemonConfiguration.getIp(), DaemonConfiguration.getPort(), 11);
			dest = new DaemonAddress(DaemonConfiguration.getDaemonId(), DaemonConfiguration
					.getPeer0Address());

			trigger(new MinaNetworkInit(DaemonConfiguration.getPeer0Address()), network
					.getControl());
			connect(daemon.getNegative(Timer.class), timer.getPositive(Timer.class));
			connect(daemon.getNegative(Network.class), network.getPositive(Network.class),
					new MessageDestinationFilter(dest.getPeerAddress()));

			DaemonInit dInit = new DaemonInit(DaemonConfiguration.getDaemonId(),
					DaemonConfiguration.getPeer0Address(), 
					MasterAddressConfiguration.getMasterAddress(),
					DaemonConfiguration.getDaemonRetryPeriod(), 
					DaemonConfiguration.getDaemonRetryCount(), 
					DaemonConfiguration.getDaemonIndexingPeriod(),
					DaemonConfiguration.getDaemonRetryPeriod());
			trigger(dInit, daemon.getControl());

			subscribe(handleJobLoadResponseMsg, network.getPositive(Network.class));
			subscribe(handleJobStartResponseMsg, network.getPositive(Network.class));
			subscribe(handleJobStopResponse, network.getPositive(Network.class));
			subscribe(handleJobExited, network.getPositive(Network.class));
			subscribe(handleJobsFound, network.getPositive(Network.class));
			subscribe(handleJobReadFromExecutingResponseMsg, network.getPositive(Network.class));

			subscribe(handleExecReadTimeout, timer.getPositive(Timer.class));
			subscribe(handleJobStopTimeout, timer.getPositive(Timer.class));

			subscribe(handleStart, control);

		}

		public static void setTestObj(DaemonTest testObj) {
			TestDaemonComponent.testObj = testObj;
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {
				logger.info("Preparing JobLoadRequestMsg to be sent to Daemon");

				String groupId = "se.sics.kompics";
				String artifactId = "kompics-manual";
				String version = "0.4.2-SNAPSHOT";
				String repoId = "sics-snapshot";
				String repoUrl = "http://kompics.sics.se/maven/snapshotrepository";
				String mainClass = "se.sics.kompics.manual.example1.Root";
/*
				JobLoadRequestMsg msg = new JobLoadRequestMsg(groupId, artifactId, version, repoId,
						repoUrl, mainClass, new ArrayList<String>(), HIDE_MAVEN_EMBEDDER_OUTPUT, src, dest);
				logger.info("JobLoadRequestMsg being sent..");
				trigger(msg, network.getPositive(Network.class));
*/
			}
		};

		public Handler<JobStartResponseMsg> handleJobStartResponseMsg = new Handler<JobStartResponseMsg>() {
			public void handle(JobStartResponseMsg event) {

				logger.info("Received job start Response {} from job-id: {} ", 
						event.getSource(), event.getJobId());

				
				if (event.getStatus() != JobStartResponse.Status.SUCCESS)
				{
					testObj.fail(true);
				}

				// Read from executing job
				trigger(new JobReadFromExecutingRequestMsg(event.getJobId(),  event.getDestination(),
						new DaemonAddress(event.getDaemonId(), event.getSource())), network
						.getPositive(Network.class));

				ScheduleTimeout st = new ScheduleTimeout(15000);
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
				trigger(new JobReadFromExecutingRequestMsg(event.getJobId(), src, dest), network
						.getPositive(Network.class));

				ScheduleTimeout st = new ScheduleTimeout(3*1000);
				st.setTimeoutEvent(new JobStopTimeout(event.getJobId(), st));
				trigger(st, timer.getPositive(Timer.class));
			}
		};

		public Handler<JobStopTimeout> handleJobStopTimeout = new Handler<JobStopTimeout>() {
			public void handle(JobStopTimeout event) {
				logger.warn("Trying to stop a job: " + event.getJobId());
				trigger(new JobStopRequestMsg(event.getJobId(), src, dest), network
						.getPositive(Network.class));

			}
		};

		public Handler<JobExitedMsg> handleJobExited = new Handler<JobExitedMsg>() {
			public void handle(JobExitedMsg event) {
				logger.debug("Job {} exited with exit value {} ", event.getJobId(), event.getExitValue());

				// trigger(new JobRemoveRequest(dummy),
				// daemon.getPositive(Maven.class));
			}
		};

		public Handler<JobStopResponseMsg> handleJobStopResponse = new Handler<JobStopResponseMsg>() {
			public void handle(JobStopResponseMsg event) {
				
				if (event.getStatus() == JobStopResponse.Status.STOPPED) {
				logger.debug("Job stopped : " + event.getJobId() + " : " + event.getStatus()
						+ " : " + event.getMsg());
				TestDaemonComponent.testObj.pass();
				}
				else {
					logger.debug("Failed to STOP JOB: " + event.getJobId() + " : " + event.getStatus()
							+ " : " + event.getMsg());
					TestDaemonComponent.testObj.fail(true);
					
				}
			}
		};

		public Handler<JobLoadResponseMsg> handleJobLoadResponseMsg = new Handler<JobLoadResponseMsg>() {
			public void handle(JobLoadResponseMsg event) {

				logger.info("Job {} loaded with status {}", event.getJobId(), event.getStatus());

				// if success then remove from loadingJobs, add to loadedJobs
				if (event.getStatus() == JobLoadResponse.Status.FAIL
						|| event.getStatus() == JobLoadResponse.Status.DUPLICATE) {
					logger.error("JobLoadRequest for {} not success. Status: {}", event.getJobId(),
							event.getStatus());
					testObj.fail(true);
				} else {
					DaemonAddress daemonAddr = new DaemonAddress(event.getDaemonId(), event
							.getSource());
					// list loaded jobs
					trigger(new ListJobsLoadedRequestMsg(event.getDestination(), daemonAddr),
							network.getPositive(Network.class));
				}

			}
		};

		public Handler<JobsFoundMsg> handleJobsFound = new Handler<JobsFoundMsg>() {
			public void handle(JobsFoundMsg event) {

				DaemonAddress daemonAddr = new DaemonAddress(event.getDaemonId(), event.getSource());
				for (Job job : event.getSetJobs()) {

					int id = job.getId();

					logger.info("Received job {} found.", id);
					if (loadedJobs.containsKey(id) == false) {
						loadedJobs.put(id, job);
						logger.info("Added job {} to loaded jobs set.", id);

						trigger(new JobStartRequestMsg(job.getId(), 1, scenario, event
								.getDestination(), daemonAddr), network.getPositive(Network.class));

					}
				}

			}
		};

		private void removeJob(int jobId) {
			Job job = loadedJobs.get(jobId);
			trigger(new JobRemoveRequest(job), daemon.getNegative(MavenPort.class));
		}

		public Handler<JobReadFromExecutingResponseMsg> handleJobReadFromExecutingResponseMsg = new Handler<JobReadFromExecutingResponseMsg>() {
			public void handle(JobReadFromExecutingResponseMsg event) {

				logger.info("Read from job {} :", event.getJobId());
				logger.info(event.getMsg());
			}
		};
		
	

		public Handler<JobRemoveResponse> handleJobRemoveResponse = new Handler<JobRemoveResponse>() {
			public void handle(JobRemoveResponse event) {

				logger
						.info("Job remove response was:" + event.getMsg() + " - "
								+ event.getStatus());

				// trigger(new IndexShutdown(),
				// indexer.getNegative(Index.class));
				//				
				// trigger(new Stop(), indexer.getControl());
			}
		};
	}

	// @Ignore
	@org.junit.Test 
	public void testDaemon() {

		TestDaemonComponent.setTestObj(this);

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		try {
			Configuration.init(new String[] {}, DaemonConfiguration.class);
			Kompics.createAndStart(DaemonTest.TestDaemonComponent.class, 1);
		} catch (ConfigurationException e1) {
			e1.printStackTrace();
		}

		try {
			DaemonTest.semaphore.acquire(EVENT_COUNT);
			System.out.println("Exiting unit test....");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.getMessage());
		}

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