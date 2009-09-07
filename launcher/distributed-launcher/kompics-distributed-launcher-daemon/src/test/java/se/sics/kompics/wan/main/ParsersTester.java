package se.sics.kompics.wan.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.JobToDummyPom;
import se.sics.kompics.wan.util.AddressParser;

/**
 * Unit test for simple App.
 */
public class ParsersTester extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public ParsersTester(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(ParsersTester.class);
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

			String[] hosts = { "12@lucan.sics.se:12123", "evgsics1.sics.se:123", "evgsics2.sics.se" };
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

			TreeSet<Address> addrs = AddressParser.parseAddressesFile(filename);

			int i = 0;

			assertEquals(true, hosts.length == addrs.size());
			for (Address a : addrs) {
				System.out.println(a.getId() + "@" + a.getIp().getHostName() + ":" + a.getPort() );
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertEquals(true, false);
		} 

	}
	
	
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
