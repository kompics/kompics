package se.sics.kompics.wan.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.JobToDummyPom;
import se.sics.kompics.wan.util.HostsParser;
import se.sics.kompics.wan.util.HostsParserException;

/**
 * Unit test for simple App.
 */
public class ParsersTest extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public ParsersTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(ParsersTest.class);
	}

	public void testGeneratePomFile() {
		String repoId = "sics-snapshot-XXX";
		String repoUrl = "http://kompics.sics.se/maven/snapshotrepository";
		String groupId = "se.sics.kompics";
		String artifactId = "kompics-manual";
		String version = "0.4.2-SNAPSHOT";
		
		String mainClass = "se.sics.kompics.manual.example1.Root";
		
		try {
			List<String> args = new ArrayList<String>();
			args.add("jim");
			JobToDummyPom dp = new JobToDummyPom(groupId, artifactId, version, mainClass, args,
					repoId, repoUrl);
			dp.createDummyPomFile();
			
			assertTrue(new File(dp.getPomFilename()).exists());
		} catch (DummyPomConstructionException e) {
			System.err.println(e.getMessage());
			assertTrue(false);
		}
		
	}
	


	public void testHostsFileParser() {
		try {
			File f = File.createTempFile("hosts", "dat");

			String[] hosts = { "lucan:12123:12", "evgsics1:123", "evgsics2" };
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f)));

			Arrays.sort(hosts);
			for (String host : hosts) {
				bw.write(host);
				bw.newLine();
			}
			
			String filename = f.getPath();

			bw.flush();
			bw.close();

			Set<Address> addrs = HostsParser.parseAddresses(filename);

			int i = 0;

			assertEquals(true, hosts.length == addrs.size());
			for (Address a : addrs) {
				System.out.println(a.getIp().getHostName() + ":" + a.getPort() + ":" + a.getId());
				String[] addrParts = hosts[i++].split(":");
				assertEquals(true, a.getIp().getHostName()
						.compareTo(addrParts[0]) == 0);
				if (addrParts.length >= 2) {
					assertEquals(true, a.getPort() == Integer.parseInt(addrParts[1]));
				}
				if (addrParts.length >= 3) {
					assertEquals(true, a.getId() == Integer.parseInt(addrParts[2]));
				}
			}

		} catch (HostsParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertEquals(true, false);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertEquals(true, false);
		} 

	}
	
	/*
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
