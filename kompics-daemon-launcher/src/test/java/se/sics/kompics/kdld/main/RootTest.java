package se.sics.kompics.kdld.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

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

	/**
	 * Rigourous Test :-)
	 */
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

			// INSTALL a jar in the local repository after downloading from sics
			MavenExecutionRequest request = new DefaultMavenExecutionRequest()
					.setBaseDirectory(tmpDir)
					.setGoals(
							Arrays
									.asList(new String[] { "install:install-file" }));

			URI uriPom = null;
			URI uriJar = null;
			try {
				uriPom = new URI(
						"http://korsakov.sics.se/maven/repository/se/sics/kompics/kompics-manual/0.4.0/kompics-manual-0.4.0.pom");
				uriJar = new URI(
						"http://korsakov.sics.se/maven/repository/se/sics/kompics/kompics-manual/0.4.0/kompics-manual-0.4.0.jar");
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			File remotePom = new File(uriPom);
			File localPom = new File(tmpDir, "pom.xml");
			try {
				copy(remotePom, localPom);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			File remoteJarFile = new File(uriJar);
			File localJarFile = new File(tmpDir, "kompics-manual-0.4.0.jar");
			try {
				copy(remoteJarFile, localJarFile);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			request.setPom(localPom);

			request
					.setProperty("file",
							tmpDir + "/kompics-manual-0.4.0.jar");

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
	
    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
    
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
