package se.sics.kompics.kdld.job;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import se.sics.kompics.kdld.daemon.Daemon;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.kdld.daemon.ListJobsLoadedRequest;
import se.sics.kompics.kdld.daemon.ListJobsLoadedResponse;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import com.sun.org.apache.xpath.internal.XPathAPI;

public class Indexer extends ComponentDefinition {

	public static final char PACKAGE_SEPARATOR = '.';	
	
	private static final File POM_FILE = new File("pom.xml");
	
	
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	
	private static final Logger logger = LoggerFactory.getLogger(Indexer.class);

	private Negative<Index> indexPort = negative(Index.class);
	private Positive<Network> net = positive(Network.class);
	private Positive<Timer> timer = positive(Timer.class);
	
	private long indexingPeriod;
	
	
	// (pom-filename, job-object) pair
	private Map<String, Job> localJobs = new HashMap<String, Job>();

	public Indexer() {

		subscribe(handleIndexStop, indexPort);
		subscribe(handleIndexStart, indexPort);
		subscribe(handleIndexerTimeout, timer);
		subscribe(handleIndexerInit, control);
		subscribe(handleStart, control);
		subscribe(handleListJobsLoadedRequest, indexPort);
	}

	public Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
//			trigger(new IndexStart(), indexPort);
		}
	};
	
	public Handler<IndexerInit> handleIndexerInit = new Handler<IndexerInit>() {
		public void handle(IndexerInit event) {

		indexingPeriod = event.getIndexingPeriod();
		}
	};
	
	public Handler<IndexStart> handleIndexStart = new Handler<IndexStart>() {
		public void handle(IndexStart event) {
			
			File kHome = new File(Daemon.KOMPICS_HOME);
			
			visitAllDirsAndFiles(kHome, "");
			
			
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
					0, indexingPeriod);
			spt.setTimeoutEvent(new IndexerTimeout(spt));
			trigger(spt, timer);
		}
	};
	

	public Handler<ListJobsLoadedRequest> handleListJobsLoadedRequest = new Handler<ListJobsLoadedRequest>() {
		public void handle(ListJobsLoadedRequest event) {
			
			Set<Job> setJobs = new HashSet<Job>(localJobs.values());
			DaemonAddress src = new DaemonAddress(event.getDaemonId(), event.getDestination());
			trigger(new ListJobsLoadedResponse(setJobs, src, event.getDestination()), net);
		}
	};
	
	
	
	public Handler<IndexStop> handleIndexStop = new Handler<IndexStop>() {
		public void handle(IndexStop event) {

			

		}
	};
	
	private Handler<IndexerTimeout> handleIndexerTimeout = new Handler<IndexerTimeout>() {
		public void handle(IndexerTimeout event) {
			logger.debug("Indexer running...");

			// check KOMPICS_HOME for newly downloaded dummy pom projects
			
			
			// set the indexing timer to index again
			SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
					0, indexingPeriod);
			spt.setTimeoutEvent(new IndexerTimeout(spt));
			trigger(spt, timer);
		}
	};
	
	
	
	
	   // Process all files and directories under dir
    public void visitAllDirsAndFiles(File dir, String groupVersion) {
        if (dir.isDirectory()) {
        	
            // XXX GroupId, Version, Artifact
        	
        	if (groupVersion.compareTo("") == 0)
        	{
        		groupVersion = dir.getName();
        	}
        	else
        	{
            	groupVersion = groupVersion + PACKAGE_SEPARATOR + dir.getName();
        	}
        	
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                visitAllDirsAndFiles(new File(dir, children[i]), groupVersion);
            }
        }
        else
        {
        	if (dir.compareTo(POM_FILE) == 0)
        	{
        		// Found a pom file locally.
        		try {
        			if (localJobs.containsKey(dir.getAbsolutePath()) == false)
        			{
        				loadPom(dir, groupVersion); 
        			}
				} catch (PomIndexingException e) {
					e.printStackTrace();
				}
        	}
        }
    }

/*
 * Test if the jar for Pom file is downloaded (assume dependent jars also downloaded if true).
 * If jar file found then send a JobFoundLocally event to Daemon    
 */
    private void loadPom(File pom, String groupVersion) throws PomIndexingException
    {
//    	String version = groupVersion.substring(groupVersion.lastIndexOf(PACKAGE_SEPARATOR));
    	// XXX read in using XPath as XML file
    	Document xmlDoc;
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new PomIndexingException(e.getMessage());
		}

		try {
			// parse the local dummy pom file
			xmlDoc = builder.parse(pom);
		} catch (SAXException e) {
			throw new PomIndexingException(e.getMessage());
		} catch (IOException e) {
			throw new PomIndexingException(e.getMessage());
		}
		
		String mainClass = 
			getElementText(xmlDoc, "/project/build/plugins/plugin/configuration/mainClass");

		String jobId = 
			getElementText(xmlDoc, "/project/properties/jobId");
		
		String repoId = getElementText(xmlDoc, "/project/repositories/repository/id");
		String repoName = getElementText(xmlDoc, "/project/repositories/repository/name");
		String repoUrl = getElementText(xmlDoc, "/project/repositories/repository/url");

		String groupId = getElementText(xmlDoc, "/project/dependencies/dependency/groupId");
		String version = getElementText(xmlDoc, "/project/dependencies/dependency/artifactId");
		String artifactId = getElementText(xmlDoc, "/project/dependencies/dependency/version");
    	
		
		String groupPath = groupId.replace('.', File.separatorChar);
		char[] separator = new char[1];
		separator[0] = File.separatorChar;
		String sepStr = new String(separator);
    	String jarFileName = Daemon.MAVEN_REPO_HOME + sepStr + groupPath + sepStr +
    						version + sepStr + artifactId + "-" + version + ".jar";
    	File jarFile = new File(jarFileName);
    	
    	JobFoundLocally job;
    	if ((localJobs.containsKey(jarFileName) == false) && 
    			jarFile.exists()  && jarFile.canRead() && isZipOrJarArchive(jarFile))
    	{
    		List<String> args = new ArrayList<String>();
			try {
				job = new JobFoundLocally(Integer.parseInt(jobId), repoId, repoUrl, repoName,
						groupId, artifactId, version, mainClass, args);
			} catch (NumberFormatException e) {
				throw new PomIndexingException(e.getMessage());
			} catch (DummyPomConstructionException e) {
				throw new PomIndexingException(e.getMessage());
			}
			
			localJobs.put(pom.getAbsolutePath(), job);    		
    		trigger(job, indexPort);
    	}
    }
    
    private boolean isZipOrJarArchive(File file) {
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
                } catch (IOException ignored) {}
            }
        }
        return isArchive;
    }

    
    private String getElementText(Document xmlDoc, String element) throws PomIndexingException
    {
		NodeIterator nl;
		String res;
		try {
			nl = XPathAPI.selectNodeIterator(xmlDoc, element);
			Node n;
			n = nl.nextNode();
			if (n != null) {
				Element e = (Element) n;
				res = e.getTextContent();
			}
			else
			{
				throw new PomIndexingException("Could not find element in pom: " + element);
			}
			
		} catch (TransformerException e1) {
			throw new PomIndexingException(e1.getMessage());
		}
		return res;
    }

}
