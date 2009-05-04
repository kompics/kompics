package se.sics.kompics.kdld.main;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequest;
import se.sics.kompics.kdld.daemon.ListJobsLoadedResponse;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Index;
import se.sics.kompics.kdld.job.Indexer;
import se.sics.kompics.kdld.job.IndexerInit;
import se.sics.kompics.kdld.job.Job;
import se.sics.kompics.kdld.job.JobFoundLocally;
import se.sics.kompics.kdld.job.JobToDummyPom;

public class TestIndexerComponent extends ComponentDefinition {

	public static final String KOMPICS_HOME;

	public static final String MAVEN_REPO_HOME;

	public static final String SCENARIO_FILENAME = "scenario";

	static {
		String kHome = System.getProperty("kompics.home");
		String userHome = System.getProperty("user.home");
		if (userHome != null && kHome == null) {
			System.setProperty("kompics.home", new File(userHome + "/.kompics/").getAbsolutePath());
		} else {
			throw new IllegalStateException(
					"kompics.home and user.home environment variables not set.");
		}
		KOMPICS_HOME = System.getProperty("kompics.home");

		if (new File(TestIndexerComponent.KOMPICS_HOME).exists() == false) {
			if (new File(TestIndexerComponent.KOMPICS_HOME).mkdirs() == false) {
				throw new IllegalStateException("Could not create directory: "
						+ TestIndexerComponent.KOMPICS_HOME);
			}
		}

		if (new File(TestIndexerComponent.KOMPICS_HOME).exists() == false) {

			if ((new File(TestIndexerComponent.KOMPICS_HOME).mkdirs()) == false) {
				throw new IllegalStateException("Couldn't directory for Kompics Home: "
						+ TestIndexerComponent.KOMPICS_HOME + "\nCheck file permissions for this directory.");
			}
		}

		String mavenHome = System.getProperty("maven.home");
		if (mavenHome == null) {
			System.setProperty("maven.home", new File(userHome + "/.m2/repository/")
					.getAbsolutePath());

		} else {
			throw new IllegalStateException(
					"maven.home and user.home environment variables not set.");
		}
		MAVEN_REPO_HOME = System.getProperty("maven.home");

		if (new File(TestIndexerComponent.MAVEN_REPO_HOME).exists() == false) {

			if ((new File(TestIndexerComponent.MAVEN_REPO_HOME).mkdirs()) == false) {
				throw new IllegalStateException("Couldn't directory for Maven Home: "
						+ TestIndexerComponent.MAVEN_REPO_HOME + "\nCheck file permissions for this directory.");
			}
		}

		PropertyConfigurator.configureAndWatch("log4j.properties");
	}

	private static final Logger logger = LoggerFactory.getLogger(TestIndexerComponent.class);

	private Component indexer;

	private Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();


	public TestIndexerComponent() {

		indexer = create(Indexer.class);
		// XXX connect NET port on mavenLauncher apacheMina component

		subscribe(handleListJobsLoadedResponse, indexer.getPositive(Index.class));
		subscribe(handleJobFoundLocally, indexer.getPositive(Index.class));
		subscribe(handleStart, control);
		
		logger.info("Initializing the Indexer");
		trigger(new IndexerInit(100), indexer.getPositive(Index.class));
	}
	
	public Handler<Start> handleStart = new Handler<Start>()
	{
		public void handle(Start event) {		

			try {
				JobToDummyPom dummy = new JobToDummyPom(11,
						"sics-snapshot", 
						"http://kompics.sics.se/maven/snapshotrepository", 
						"SICS Snapshot Repository",
						"se.sics.kompics", 
						"kompics-manual", 
						"0.4.2-SNAPSHOT", 
						"se.sics.kompics.manual.example1.Root",
						new ArrayList<String>());

				logger.info("Creating a dummy pom");
				
				dummy.createDummyPomFile();
			
				// this should return a JobFoundLocally event 
				
			} catch (DummyPomConstructionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
	};


	public Handler<ListJobsLoadedResponse> handleListJobsLoadedResponse = 
		new Handler<ListJobsLoadedResponse>() {
		public void handle(ListJobsLoadedResponse event) {

			Set<Job> listJobsLoaded  = event.getSetJobs();
			
			for (Job j : listJobsLoaded){
				if (loadedJobs.containsKey(j.getId()) == true) {
					logger.info("Found already loaded job: " + j.getId());
				}
				else
				{
					logger.info("ERROR: Found job not loaded: " + j.getId());
				}
			}
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
							 indexer.getNegative(Index.class));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
			}

		}
	};


}
