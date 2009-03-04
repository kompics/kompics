package se.sics.kompics.kdld.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import se.sics.kompics.address.Address;

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

	/*
	public void testRoot() {
		String mavenHome = System.getProperty("maven.home");
		String userHome = System.getProperty("user.home");
		if (mavenHome != null) {
			System.setProperty("maven.home", new File(userHome + "/.m2/")
					.getAbsolutePath());
		} else {
			mavenHome = new File(userHome, ".m2").getAbsolutePath();
		}

		String projectName = "blah";

		String tmpDirectory = System.getProperty("java.io.tmpdir");

		System.out.println("Tmp dir = " + tmpDirectory);

		System.out.println("MAVEN_HOME = " + mavenHome);

		File tmpDir = new File(tmpDirectory, projectName);

		File user = new File(mavenHome, "settings.xml");

		System.out.println("Settings file = " + user.toString());

		Configuration configuration = new DefaultConfiguration()
				.setUserSettingsFile(user).setClassLoader(
						Thread.currentThread().getContextClassLoader());

		ConfigurationValidationResult validationResult = MavenEmbedder
				.validateConfiguration(configuration);

		if (validationResult.isValid()) {
			MavenEmbedder embedder = null;
			try {
				embedder = new MavenEmbedder(configuration);
			} catch (MavenEmbedderException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			File manualDir = new File(tmpDirectory , "manual");
			// INSTALL a jar in the local repository after downloading from sics
			MavenExecutionRequest request = new DefaultMavenExecutionRequest()
					.setBaseDirectory(manualDir)
					.setGoals(
							Arrays
									.asList(new String[] { "compile" }));
// install:install-file


			// ArtifactRepository repo =
			// request.addRemoteRepository();

			MavenExecutionResult result = embedder.execute(request);

			if (result.hasExceptions()) {
				// fail();
				try {
					embedder.stop();
				} catch (MavenEmbedderException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String failMsg = ((Exception) result.getExceptions().get(0))
						.getMessage();
				System.err.println(failMsg);
			}

			// ----------------------------------------------------------------------------
			// You may want to inspect the project after the execution.
			// ----------------------------------------------------------------------------

			MavenProject project = result.getProject();

			// Do something with the project

			String groupId = project.getGroupId();

			String artifactId = project.getArtifactId();

			String version = project.getVersion();

			String name = project.getName();

			String environment = project.getProperties().getProperty(
					"environment");

			// assertEquals( "development", environment );

			System.out.println("You are working in the '" + environment
					+ "' environment!");
		} else {
			if (!validationResult.isUserSettingsFilePresent()) {
				System.out.println("The specific user settings file '" + user
						+ "' is not present.");
			} else if (!validationResult.isUserSettingsFileParses()) {
				System.out
						.println("Please check your settings file, it is not well formed XML.");
			}
		}
	}
	*/
	
	public void testHostsFileParser()
	{
		try {
			File f = File.createTempFile("hosts", "dat");
			
			String[] hosts = { "lucan", "evgsics1", "evgsics2"};
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			
			for (String host : hosts)
			{
				bw.write(host);
				bw.newLine();
			}
			String filename = f.getPath();
			
			bw.flush();
			bw.close();
			
			List<Address> addrs = Root.parseHostsFile(filename);
			
			int i = 0;
			
			assertEquals(true, hosts.length == addrs.size());
			for (Address a : addrs)
			{
				System.out.println(a.getIp().getHostName());
				assertEquals(true, a.getIp().getHostName().compareTo(hosts[i++]) == 0);
			}
						
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertEquals(true, false);
		}
		
	}
	
	
    private void copy(InputStream in, OutputStream out) throws IOException {
//        InputStream in = new FileInputStream(src);
//        OutputStream out = new FileOutputStream(dst);
    
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
    
    
    public void testJarDownload()
    {
    	String repo = "http://korsakov.sics.se/maven/repository/";
    	String group = "se/sics/kompics/";
    	String artifact = "kompics-manual";
    	String version = "0.4.0";
    	
    	String tmpDirectory = System.getProperty("java.io.tmpdir");
    	File projDir = new File(tmpDirectory, artifact);
    	projDir.mkdir();
    	
    	URL pomRemote = null;
		URL jarRemote = null;
		try {
			pomRemote = new URL( repo + group + artifact + "/" + version 
						+ "/" + artifact + "-" + version + ".pom");
			jarRemote = new URL(repo + group + artifact + "/" + version 
					+ "/" + artifact + "-" + version + ".jar");
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
}
