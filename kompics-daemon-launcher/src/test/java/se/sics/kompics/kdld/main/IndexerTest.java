package se.sics.kompics.kdld.main;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
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
import se.sics.kompics.kdld.job.JobToDummyPom;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

/**
 * Unit test for simple App.
 */
public class IndexerTest {

	public static Semaphore semaphore = new Semaphore(0);

	public static final int EVENT_COUNT = 1;



	public IndexerTest() {

	}

	public static class TestIndexerComponent extends ComponentDefinition {

		public Logger logger = LoggerFactory.getLogger(TestIndexerComponent.class);

		public Component indexer;
		public Component timer;
		private Component mavenLauncher;
		
		public Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();

		private static IndexerTest testObj = null;

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
			
			subscribe(handleStart, control);

			trigger(new IndexerInit(10000), indexer.getControl());

			logger.info("Initializing the Indexer");
		}

		public static void setTestObj(IndexerTest testObj) {
			TestIndexerComponent.testObj = testObj;
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {
				logger.info("Starting TestIndexer");

				try {
					JobToDummyPom dummy = new JobToDummyPom(11, "sics-snapshot",
							"http://kompics.sics.se/maven/snapshotrepository",
							"SICS Snapshot Repository", "se.sics.kompics", "kompics-manual",
							"0.4.2-SNAPSHOT", "se.sics.kompics.manual.example1.Root",
							new ArrayList<String>());

					logger.info("Creating a dummy pom");

					// need to call maven assembly:assembly if the jar hasn't been loaded yet.
					// timeout for a few seconds, if no response then send maven assembly:assembly
					trigger(new JobAssembly(dummy), mavenLauncher.getPositive(Maven.class));
					
				} catch (DummyPomConstructionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		};
		
		public Handler<JobAssemblyResponse> handleJobAssemblyResponse = new Handler<JobAssemblyResponse>() {
			public void handle(JobAssemblyResponse event) {

				// if success then remove from loadingJobs, add to loadedJobs
				if (event.getStatus() != JobAssemblyResponse.Status.ASSEMBLED) {
					testObj.fail();
				}
				
				SimulationScenario simulationScenario = new SimulationScenario()
				{
					
				};
				Job j = loadedJobs.get(event.getJobId());
				if (j != null){
					trigger(new JobExec(j,simulationScenario), mavenLauncher.getPositive(Maven.class));
				}
				else
				{
					logger.debug("job returned by assemblyResponse was null");
				}
				
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
				IndexerTest.semaphore.release(1);
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

	}

	@org.junit.Test
	public void testIndexer() {

		TestIndexerComponent.setTestObj(this);

		Kompics.createAndStart(TestIndexerComponent.class, 1);

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