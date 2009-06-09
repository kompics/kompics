package se.sics.kompics.wan.daemon.indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.daemon.Daemon;
import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.util.PomUtils;

import com.sun.org.apache.xpath.internal.XPathAPI;


public class Indexer extends ComponentDefinition {

	public static final char PACKAGE_SEPARATOR = '.';

	private static final String POM_FILE = "pom.xml";

	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}

	private static final Logger logger = LoggerFactory.getLogger(Indexer.class);

	private Negative<IndexPort> indexPort = negative(IndexPort.class);
	private Positive<Timer> timer = positive(Timer.class);

	private long indexingPeriod;
	private boolean indexingStopped = false;
	private UUID indexingTimoutId;
	
	// (pom-filename, job-object) pair
	private Map<String, Job> indexedJobs = new HashMap<String, Job>();

	public Indexer() {

		subscribe(handleIndexShutdown, indexPort);
		subscribe(handleIndexStart, indexPort);
		subscribe(handleListJobsLoadedRequest, indexPort);

		subscribe(handleIndexerTimeout, timer);

		subscribe(handleIndexerInit, control);
		subscribe(handleStart, control);
		subscribe(handleStop, control);
	}

	public Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			logger.debug("Starting Indexing...");

			scheduleIndexing();
		}
	};
	
	public Handler<Stop> handleStop= new Handler<Stop>() {
		public void handle(Stop event) {
			stopIndexing();
		}
	};

	public Handler<IndexerInit> handleIndexerInit = new Handler<IndexerInit>() {
		public void handle(IndexerInit event) {
			logger.info("Initializing indexPeriod as: " + event.getIndexingPeriod());
			indexingPeriod = event.getIndexingPeriod();
		}
	};

	public Handler<IndexStart> handleIndexStart = new Handler<IndexStart>() {
		public void handle(IndexStart event) {

			File kHome = new File(Daemon.KOMPICS_HOME);

			visitAllDirsAndFiles(kHome, "");

			scheduleIndexing();
		}
	};

	public Handler<ListJobsLoadedRequest> handleListJobsLoadedRequest = new Handler<ListJobsLoadedRequest>() {
		public void handle(ListJobsLoadedRequest event) {
			Set<Job> setJobs = new HashSet<Job>(indexedJobs.values());
			trigger(new ListJobsLoadedResponse(event, setJobs), indexPort);
		}
	};

	public Handler<IndexShutdown> handleIndexShutdown = new Handler<IndexShutdown>() {
		public void handle(IndexShutdown event) {
			stopIndexing();
		}
	};
	
	private void stopIndexing()
	{
		logger.info("Indexing stopped.");
		CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(indexingTimoutId);
		trigger(cpt, timer);
		indexingStopped = true;
	}

	private Handler<IndexerTimeout> handleIndexerTimeout = new Handler<IndexerTimeout>() {
		public void handle(IndexerTimeout event) {
			logger.debug("Indexer timeout expired. Indexing...");

			if (indexingStopped == false) {
				File kHome = new File(Daemon.KOMPICS_HOME);
				visitAllDirsAndFiles(kHome, "");
			}
		}
	};

	private void scheduleIndexing() {
		SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(0, indexingPeriod);
		spt.setTimeoutEvent(new IndexerTimeout(spt));
		indexingTimoutId = spt.getTimeoutEvent().getTimeoutId();
		trigger(spt, timer);
	}

	// Process all files and directories under dir
	public void visitAllDirsAndFiles(File dir, String groupArtifactVersion) {
		if (dir.isDirectory()) {

			if (groupArtifactVersion.compareTo("") == 0) // basedir
			{
				groupArtifactVersion = dir.getName();
			} else {
				groupArtifactVersion = groupArtifactVersion + PACKAGE_SEPARATOR + dir.getName();
			}

			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				visitAllDirsAndFiles(new File(dir, children[i]), groupArtifactVersion);
			}
		} else {

			if (dir.getName().compareTo(POM_FILE) == 0) { // Found a pom file
															// locally
				try {
					if (indexedJobs.containsKey(groupArtifactVersion) == false) {
						loadPom(dir, groupArtifactVersion);
					}
				} catch (PomIndexingException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
		}
	}

	/*
	 * Test if the jar for Pom file is downloaded (assume dependent jars also
	 * downloaded if true). If jar file found then send a JobFoundLocally event
	 * to Daemon
	 */
	private void loadPom(File pom, String groupArtifactVersion) throws PomIndexingException {
		Document xmlDoc;
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new PomIndexingException(e.getMessage());
		}

		try {
			// parse the local dummy pom file in KOMPICS_HOME/groupId/artifiactId/version/pom.xml
			xmlDoc = builder.parse(pom);
		} catch (SAXException e) {
			throw new PomIndexingException(e.getMessage());
		} catch (IOException e) {
			throw new PomIndexingException(e.getMessage());
		}

		String mainClass = getElementText(xmlDoc,
				"/project/build/plugins/plugin/configuration/mainClass");

		String jobId = getElementText(xmlDoc, "/project/properties/jobId");

		String repoId = getElementText(xmlDoc, "/project/repositories/repository/id");
		String repoUrl = getElementText(xmlDoc, "/project/repositories/repository/url");

		String groupId = getElementText(xmlDoc, "/project/dependencies/dependency/groupId");
		String version = getElementText(xmlDoc, "/project/dependencies/dependency/version");
		String artifactId = getElementText(xmlDoc, "/project/dependencies/dependency/artifactId");

		String groupPath = PomUtils.groupIdToPath(groupId);
		String sepStr = PomUtils.sepStr();
		String jarFileName = Daemon.MAVEN_REPO_LOCAL + sepStr + groupPath + sepStr + artifactId
				+ sepStr + version + sepStr + artifactId + "-" + version + ".jar";
		File jarFile = new File(jarFileName);

		String dummyJarFileName = Daemon.KOMPICS_HOME + sepStr + groupPath + sepStr + artifactId
		+ sepStr + version + sepStr + "target" + sepStr + "dummy-0.1-jar-with-dependencies.jar";
		File dummyJarFile = new File(dummyJarFileName);
		
		JobFound job;
		if ((indexedJobs.containsKey(jarFileName) == false) 
				&& isValidJar(jarFile)
				&& isValidJar(dummyJarFile)) {
			List<String> args = new ArrayList<String>();
			try {
				job = new JobFound(groupId, artifactId, version,
						mainClass, args, repoId, repoUrl);
			} catch (NumberFormatException e) {
				throw new PomIndexingException(e.getMessage());
			} catch (DummyPomConstructionException e) {
				throw new PomIndexingException(e.getMessage());
			}
			Set<Job> setJobs = new HashSet<Job>();
			setJobs.add(job);
			JobsFound jobs = new JobsFound(setJobs);
			
			indexedJobs.put(groupArtifactVersion, job);
			trigger(jobs, indexPort);
			logger.debug("Indexer found job: {}", groupArtifactVersion);
		}
	}

	private boolean isValidJar(File file) {

		if (file == null || file.exists() == false || file.canRead() == false)
		{
			return false;
		}
		
		boolean isArchive = true;
		ZipFile zipFile = null;

		try {
			zipFile = new ZipFile(file);
		} catch (ZipException zipCurrupted) {
			isArchive = false;
		} catch (IOException anyIOError) {
			isArchive = false;
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException ignored) {
				}
			}
		}
		return isArchive;
	}

	private String getElementText(Document xmlDoc, String element) throws PomIndexingException {
		NodeIterator nl;
		String res;
		try {
			nl = XPathAPI.selectNodeIterator(xmlDoc, element);
			Node n;
			n = nl.nextNode();
			if (n != null) {
				Element e = (Element) n;
				res = e.getTextContent();
			} else {
				throw new PomIndexingException("Could not find element in pom: " + element);
			}

		} catch (TransformerException e1) {
			throw new PomIndexingException(e1.getMessage());
		}
		return res;
	}

}
