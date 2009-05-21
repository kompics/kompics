package se.sics.kompics.kdld.main;

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
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.daemon.Daemon;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.kdld.daemon.DaemonInit;
import se.sics.kompics.kdld.daemon.JobExitedMsg;
import se.sics.kompics.kdld.daemon.JobLoadRequestMsg;
import se.sics.kompics.kdld.daemon.JobLoadResponseMsg;
import se.sics.kompics.kdld.daemon.JobMessageRequest;
import se.sics.kompics.kdld.daemon.JobReadFromExecutingRequestMsg;
import se.sics.kompics.kdld.daemon.JobReadFromExecutingResponseMsg;
import se.sics.kompics.kdld.daemon.JobStartRequestMsg;
import se.sics.kompics.kdld.daemon.JobStartResponseMsg;
import se.sics.kompics.kdld.daemon.JobStopResponseMsg;
import se.sics.kompics.kdld.daemon.JobsFoundMsg;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequestMsg;
import se.sics.kompics.kdld.daemon.maven.Maven;
import se.sics.kompics.kdld.job.Job;
import se.sics.kompics.kdld.job.JobLoadResponse;
import se.sics.kompics.kdld.job.JobRemoveRequest;
import se.sics.kompics.kdld.job.JobRemoveResponse;
import se.sics.kompics.kdld.job.JobStopRequest;
import se.sics.kompics.kdld.util.Configuration;
import se.sics.kompics.kdld.util.DaemonConfiguration;
import se.sics.kompics.kdld.util.LocalIPAddressNotFound;
import se.sics.kompics.network.Message;
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

		private Positive<Network> net = positive(Network.class);
		
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

			try {
				src = new Address(DaemonConfiguration.getIp(), DaemonConfiguration.getPort(),
						11);
				dest = new DaemonAddress(DaemonConfiguration.getDaemonId(), DaemonConfiguration
						.getPeer0Address());
			} catch (LocalIPAddressNotFound e) {
				e.printStackTrace();
				testObj.fail(true);
			}

			
			try {
				trigger(new MinaNetworkInit(DaemonConfiguration.getPeer0Address()), network
						.getControl());
				connect(daemon.getNegative(Timer.class), timer.getPositive(Timer.class));
				connect(daemon.getNegative(Network.class), network.getPositive(Network.class),
						new MessageDestinationFilter(dest.getPeerAddress()));

				DaemonInit dInit = new DaemonInit(DaemonConfiguration.getDaemonId(),
						DaemonConfiguration.getPeer0Address(), DaemonConfiguration
								.getMasterAddress(), DaemonConfiguration.getDaemonRetryPeriod(),
						DaemonConfiguration.getDaemonRetryCount(), DaemonConfiguration
								.getDaemonIndexingPeriod());
				trigger(dInit, daemon.getControl());
			} catch (LocalIPAddressNotFound e) {
				e.printStackTrace();
				throw new IllegalStateException(e.getMessage());
			}

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
				JobLoadRequestMsg msg = new JobLoadRequestMsg(groupId, artifactId, version, repoId,
						repoUrl, mainClass, new ArrayList<String>(), src, dest);

				logger.info("JobLoadRequestMsg being sent..");

				trigger(msg, network.getPositive(Network.class));

			}
		};

		public Handler<JobStartResponseMsg> handleJobStartResponseMsg = new Handler<JobStartResponseMsg>() {
			public void handle(JobStartResponseMsg event) {

				logger.info("Received job execResponse from job-id: {} ", event.getJobId());

				// Read from executing job
				trigger(new JobMessageRequest(event.getJobId(), "Hi there",
						event.getDestination(), 
						new DaemonAddress(event.getDaemonId(), event.getSource())
						), network.getPositive(Network.class));
				
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
				trigger(new JobReadFromExecutingRequestMsg(event.getJobId(), src, dest), 
						network.getPositive(Network.class));

				ScheduleTimeout st = new ScheduleTimeout(2000);
				st.setTimeoutEvent(new JobStopTimeout(event.getJobId(), st));
				trigger(st, timer.getPositive(Timer.class));
			}
		};

		public Handler<JobStopTimeout> handleJobStopTimeout = new Handler<JobStopTimeout>() {
			public void handle(JobStopTimeout event) {
				logger.warn("Trying to stop a job: " + event.getJobId());
				trigger(new JobStopRequest(event.getJobId()), daemon.getPositive(Maven.class));

			}
		};

		public Handler<JobExitedMsg> handleJobExited = new Handler<JobExitedMsg>() {
			public void handle(JobExitedMsg event) {
				logger.debug("Job exited: " + event.getJobId());

				// trigger(new JobRemoveRequest(dummy),
				// daemon.getPositive(Maven.class));
			}
		};

		public Handler<JobStopResponseMsg> handleJobStopResponse = new Handler<JobStopResponseMsg>() {
			public void handle(JobStopResponseMsg event) {
				logger.debug("Job stopped : " + event.getJobId() + " : " + event.getStatus()
						+ " : " + event.getMsg());

				TestDaemonComponent.testObj.pass();
			}
		};

		public Handler<JobLoadResponseMsg> handleJobLoadResponseMsg = 
			new Handler<JobLoadResponseMsg>() {
			public void handle(JobLoadResponseMsg event) {

				logger.info("Job {} loaded with status {}", event.getJobId(), event.getStatus());
				
				// if success then remove from loadingJobs, add to loadedJobs
				if (event.getStatus() == JobLoadResponse.Status.FAIL || event.getStatus() == JobLoadResponse.Status.DUPLICATE) {
					logger.warn("JobLoadRequest for {} not success. Status: {}", event.getJobId(), event.getStatus());
					testObj.fail(true);
				}
				else
				{
					DaemonAddress daemonAddr = 
						new DaemonAddress(event.getDaemonId(), event.getSource());
					// list loaded jobs
					trigger(new ListJobsLoadedRequestMsg(event.getDestination(), daemonAddr),
							network.getPositive(Network.class));
				}

			}
		};

		
//		public Handler<ListJobsLoadedResponseMsg> handleListJobsLoadedResponse = new Handler<ListJobsLoadedResponseMsg>() {
//			public void handle(ListJobsLoadedResponseMsg event) {
//
//				logger.info("ListJobsLoadedResponse returned {} jobs", event.getSetJobs().size());
//				Set<Job> listJobsLoaded = event.getSetJobs();
//
//				DaemonAddress daemonAddr = new DaemonAddress(event.getDaemonId(), event.getSource());
//				
//				SimulationScenario scenario = new SimulationScenario() {
//					private static final long serialVersionUID = -5355642917108165919L;
//				};
//
//				boolean jobsFound = true;
//				for (Job j : listJobsLoaded) {
//					if (loadedJobs.containsKey(j.getId()) == true) {
//						logger.info("JobStartRequestMsg sent for job: " + j.getId());						
//						// start a job
//						trigger(new JobStartRequestMsg(j.getId(), scenario, event.getDestination(),
//								daemonAddr), network.getPositive(Network.class));
//
//					} else {
//						logger.info("ERROR: Found job not loaded: " + j.getId());
//						jobsFound = false;
//					}
//				}
//				if (jobsFound == false) {
//					testObj.fail(false);
//				}
//
//				Iterator<Job> iter = listJobsLoaded.iterator();
//
//				// IndexerTest.semaphore.release(1);
//			}
//		};

		public Handler<JobsFoundMsg> handleJobsFound = new Handler<JobsFoundMsg>() {
			public void handle(JobsFoundMsg event) {
				
				SimulationScenario scenario = new SimulationScenario() {
					private static final long serialVersionUID = -5355642917108165919L;
				};
				DaemonAddress daemonAddr = new DaemonAddress(event.getDaemonId(), event.getSource());
				for (Job job : event.getSetJobs())
				{
				
					int id = job.getId();
	
					logger.info("Received job {} found.", id);
					if (loadedJobs.containsKey(id) == false) {
						loadedJobs.put(id, job);
						logger.info("Added job {} to loaded jobs set.", id);	
	
						trigger(new JobStartRequestMsg(job.getId(), scenario, event.getDestination(),
						daemonAddr), network.getPositive(Network.class));
	
					}
				}

			}
		};

		private void removeJob(int jobId) {
			Job job = loadedJobs.get(jobId);
			trigger(new JobRemoveRequest(job), daemon.getNegative(Maven.class));
		}

		public Handler<JobReadFromExecutingResponseMsg> handleJobReadFromExecutingResponseMsg = 
			new Handler<JobReadFromExecutingResponseMsg>() {
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

	@org.junit.Test
	public void testDaemon() {

		TestDaemonComponent.setTestObj(this);

		try {
			Configuration.init(new String[] {}, DaemonConfiguration.class);
			Kompics.createAndStart(TestDaemonComponent.class, 2);
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