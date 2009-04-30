package se.sics.kompics.kdld.main;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.junit.Ignore;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.Job;
import se.sics.kompics.kdld.job.JobAssembly;

/**
 * Unit test for simple App.
 */
public class RootTest extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public RootTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(RootTest.class);
	}

	public void dom() {
		String repoId = "sics-snapshot";
		String repoUrl = "http://kompics.sics.se/maven/snapshotrepository";
		String repoName = "SICS Snapshot Repository";
		
		String groupId = "se.sics.kompics";
		String artifactId = "kompics-manual";
		String version = "0.4.2-SNAPSHOT";
		
		String mainClass = "se.sics.kompics.manual.example1.Root";
		
		try {
			List<String> args = new ArrayList<String>();
			args.add("jim");
			Job dp = new JobAssembly(1, repoId, repoUrl, repoName, groupId, artifactId, version, mainClass, args);
			dp.writeToFile();
//			dp.execExec();
		} catch (DummyPomConstructionException e) {
			System.err.println(e.getMessage());
		}
	}
	

	/*
	@Ignore
	public void testRoot() throws IOException {
		String mavenHome = System.getProperty("maven.home");
		String userHome = System.getProperty("user.home");
		if (mavenHome != null) {
			System.setProperty("maven.home", new File(userHome + "/.m2/")
					.getAbsolutePath());
		} else {
			mavenHome = new File(userHome, ".m2").getAbsolutePath();
		}

		String tmpDirName = System.getProperty("java.io.tmpdir");
		System.out.println("Tmp dir = " + tmpDirName);
		File tmpDir = new File(tmpDirName); // , artifactId

		// ScriptBuilder sb;
		// sb = new ScriptBuilder("/tmp/", "archer.sh", "0.4.2-SNAPSHOT",
		// "se.sics.kompics",
		// "kompics-manual", "0.4.2-SNAPSHOT",
		// "se.sics.kompics.manual.example1.Root");
		// assertEquals(sb.runScript(),0);

		String groupId = "se.sics.kompics";
		String artifactId = "kompics-manual";
		String version = "0.4.2-SNAPSHOT";

		String[] keyVals = { "archetypeCatalog",
				"http://korsakov.sics.se/maven/daemon-launcher-catalog.xml",
				"archetypeGroupId", "se.sics.kompics", "archetypeArtifactId",
				"kompics-archetype-dl", "archetypeVersion", "0.4.2-SNAPSHOT",
				"groupId", groupId, "artifactId", artifactId, "version",
				version, "interactiveMode", "false", "mainClass",
				"se.sics.kompics.manual.example1.Root", };

		Properties properties = new Properties();
		for (int i = 0; i < keyVals.length; i += 2) {
			properties.setProperty(keyVals[i], keyVals[i + 1]);
		}

		File user = new File(mavenHome, "settings.xml");
		File global = new File("/etc/maven2/settings.xml");

		System.out.println("Settings file = " + user.toString());

		// List<EventMonitor> l = new ArrayList<EventMonitor>();
		// EventMonitor e = new DefaultEventMonitor(new ConsoleLogger(
		// ConsoleLogger.LEVEL_DEBUG, "logger"));
		// l.add(e);

		Configuration configuration = new DefaultConfiguration()
				// .setUserSettingsFile(user)
				.setUserSettingsFile(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE)
				.setClassLoader(Thread.currentThread().getContextClassLoader())
				.setGlobalSettingsFile(
						MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE)
		// .setGlobalSettingsFile( global )
		// .setEventMonitors(l)
		;
		// .setLocalRepository(kompicsRepo)

		ConfigurationValidationResult validationResult = MavenEmbedder
				.validateConfiguration(configuration);

		// if (validationResult.isValid()) {
		MavenEmbedder embedder = null;

		try {
			embedder = new MavenEmbedder(configuration);
		} catch (MavenEmbedderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String id = "", url = "http://kompics.sics.se/maven/snapshotrepository/";
		ArtifactRepository kompicsRepo = new DefaultArtifactRepository(id, url,
				new DefaultRepositoryLayout());

		List<Server> servers = new ArrayList<Server>();
		Server s = new Server();

		// mvn deploy:deploy-file
		// -Dfile=/tmp/kompics-manual/target/dummy-0.1-jar-with-dependencies.jar
		// -DrepositoryId=kompics-site
		// -Durl=scp://korsakov.sics.se/var/www/maven/snapshotrepository
		// -DpomFile=pom.xml -Dpackaging="jar"

		File manualDir = new File(tmpDirName, "kompics-manual");


		MavenExecutionRequest requestExec = new DefaultMavenExecutionRequest()
				.setBaseDirectory(manualDir).setGoals(
						Arrays.asList(new String[] { "exec:exec" }));
		requestExec.setInteractiveMode(false);
		requestExec.setLoggingLevel(1);
		requestExec.setShowErrors(true);
		mavenExec(requestExec, embedder);

		// } else {
		// if (!validationResult.isUserSettingsFilePresent()) {
		// System.out.println("The user settings file is not present.");
		// } else if (!validationResult.isUserSettingsFileParses()) {
		// System.out
		// .println("Please check your settings file, it is not well formed XML.");
		// }
		// else if (! validationResult.isGlobalSettingsFilePresent())
		// {
		// System.err.println("No global settings file present");
		//				
		// }
		// else
		// {
		// System.err.println("Some unknown validation problem: ");
		//				
		// }
		// }
	}

*/


	public void testHostsFileParser() {
		try {
			File f = File.createTempFile("hosts", "dat");

			String[] hosts = { "lucan", "evgsics1", "evgsics2" };
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f)));

			for (String host : hosts) {
				bw.write(host);
				bw.newLine();
			}
			String filename = f.getPath();

			bw.flush();
			bw.close();

			List<Address> addrs = Root.parseHostsFile(filename);

			int i = 0;

			assertEquals(true, hosts.length == addrs.size());
			for (Address a : addrs) {
				System.out.println(a.getIp().getHostName());
				assertEquals(true, a.getIp().getHostName()
						.compareTo(hosts[i++]) == 0);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertEquals(true, false);
		}

	}

	private void copy(InputStream in, OutputStream out) throws IOException {
		// InputStream in = new FileInputStream(src);
		// OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.flush();
		in.close();
		out.close();
	}

	private void BinaryFileDownload() throws Exception {
		String repo = "http://korsakov.sics.se/maven/repository/";
		String group = "se/sics/kompics/";
		String artifact = "kompics-manual";
		String version = "0.4.0";

		URL u = new URL(repo + group + artifact + "/" + version + "/"
				+ artifact + "-" + version + ".jar");
		URLConnection uc = u.openConnection();
		String contentType = uc.getContentType();
		int contentLength = uc.getContentLength();

		System.out.println("Length: " + contentLength);

		if (contentType.startsWith("text/") || contentLength == -1) {
			throw new IOException("This is not a binary file.");
		}
		InputStream raw = uc.getInputStream();
		InputStream in = new BufferedInputStream(raw);
		byte[] data = new byte[contentLength];
		int bytesRead = 0;
		int offset = 0;
		while (offset < contentLength) {
			bytesRead = in.read(data, offset, data.length - offset);
			if (bytesRead == -1)
				break;
			offset += bytesRead;
		}
		in.close();

		if (offset != contentLength) {
			throw new IOException("Only read " + offset + " bytes; Expected "
					+ contentLength + " bytes");
		}

		String tmpDirectory = System.getProperty("java.io.tmpdir");
		File projDir = new File(tmpDirectory, artifact);
		projDir.mkdir();

		String filename = u.getFile().substring(
				u.getFile().lastIndexOf('/') + 1);
		File outFile = new File(projDir, filename);

		System.out.println("File: " + outFile.getPath());
		FileOutputStream out = new FileOutputStream(outFile);
		out.write(data);
		out.flush();
		out.close();
	}

	public void jarDownload() {
		String repo = "http://korsakov.sics.se/maven/snapshotrepository/";
		String group = "se/sics/kompics/";
		String artifact = "kompics-manual";
		String version = "0.4.0";

		String tmpDirectory = System.getProperty("java.io.tmpdir");
		File projDir = new File(tmpDirectory, artifact);
		projDir.mkdir();

		URL pomRemote = null;
		URL jarRemote = null;
		try {
			pomRemote = new URL(repo + group + artifact + "/" + version + "/"
					+ artifact + "-" + version + ".pom");
			jarRemote = new URL(repo + group + artifact + "/" + version + "/"
					+ artifact + "-" + version + ".jar");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		File localPom = new File(projDir, "pom.xml");
		try {

			localPom.createNewFile();
			copy(pomRemote.openStream(), new FileOutputStream(localPom));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		File localJarFile = new File(projDir, artifact + "-" + version + ".jar");
		try {
			localJarFile.createNewFile();
			copy(jarRemote.openStream(), new FileOutputStream(localJarFile));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/*
	public void testMojo() throws IOException {
		MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

		executionRequest.setGoals(Arrays
				.asList(new String[] { "archetype:generate" }));

		Properties params = new Properties();

		String validProjectLocation = System.getProperty("java.io.tmpdir");

		params.put("groupId", "sics.se");
		params.put("artifactId", "mojo");
		params.put("version", "1.0");
		envPath = System.getProperty("java.io.tmpdir");

		// params.put("archetypeGroupId", "se.sics.kompics");
		// params.put("archetypeArtifactId", "kompics-archetype-dl");
		// params.put("archetypeVersion", "0.4.2-SNAPSHOT");
		// http://korsakov.sics.se/maven/snapshotrepository/

		// http://repo1.maven.org/maven2/org/apache/maven/archetype/maven-archetype/
		params.put("archetypeGroupId", "org.apache.maven.plugins");
		params.put("archetypeArtifactId", "maven-archetype-plugin");
		params.put("archetypeVersion", "2.0-alpha-4");

		params.put("remoteRepositories", "http://repo1.maven.org/maven2/");
		params.put("user.dir", validProjectLocation);

		executionRequest.setProperties(params);

		executionRequest.setBaseDirectory(new File(validProjectLocation));

		MavenEmbedder embedder = null;

		Configuration configuration = new DefaultConfiguration()
				.setUserSettingsFile(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE)
				.setClassLoader(Thread.currentThread().getContextClassLoader())
				.setGlobalSettingsFile(
						MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE);
		try {
			embedder = new MavenEmbedder(configuration);
		} catch (MavenEmbedderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		MavenExecutionResult result = embedder.execute(executionRequest);
		if (result.getExceptions().size() > 0) {
			System.err.println("Exception(s) thrown executing maven");
		}
	}
*/
	
	
	/*
	 * 
	 * // INSTALL a jar in the local repository after downloading from sics
	 * MavenExecutionRequest requestCompile = new
	 * DefaultMavenExecutionRequest(); // .setBaseDirectory(tmpDir)
	 * requestCompile.setBaseDirectory(manualDir);
	 * 
	 * requestCompile.setProperties(properties); requestCompile.setGoals(
	 * Arrays .asList(new String[] {"archetype:generate",
	 * "assembly:assembly"})); // .asList(new String[]
	 * {"assembly:assembly"})); requestCompile.setInteractiveMode(false);
	 * requestCompile.addEventMonitor( new DefaultEventMonitor(new
	 * ConsoleLogger( ConsoleLogger.LEVEL_INFO, "logger")))
	 * .setErrorReporter(new DefaultCoreErrorReporter()); //
	 * .setServers(servers) requestCompile.setLoggingLevel(1);
	 * requestCompile.setShowErrors(true);
	 * 
	 * // .setLocalRepositoryPath( //
	 * embedder.getLocalRepository().getUrl()).setSettings(arg0) //
	 * .setSettings( // settings).setProperties(properties).addEventMonitor(
	 * // new DefaultEventMonitor(new ConsoleLogger( //
	 * ConsoleLogger.LEVEL_DISABLED, "logger"))); // ArtifactRepository repo
	 * = request.addRemoteRepository();
	 * 
	 * mavenExec(requestCompile, embedder);
	 */

}
