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

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.main.event.Deploy;
import se.sics.kompics.kdld.main.event.DeployRequest;
import se.sics.kompics.kdld.main.event.RootInit;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * The <code>Root</code> class

 */
public final class Daemon extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
	.getLogger(Daemon.class);
	
	private static final String KOMPICS_REPOSITORY = "svn://korsakov.sics.se/maven";
	
	private Negative<Kdl> helloPort = negative(Kdl.class); 
	private Positive<Network> net = positive(Network.class); 
	private Positive<Timer> timer = positive(Timer.class);

	private Address self;
	
	public Daemon() {
		subscribe(handleStart, control);
		subscribe(handleInit, control);
		subscribe(handleDeployRequest, net);
		subscribe(handleDeploy, helloPort);
	}

	private Handler<Start> handleStart = new Handler<Start>() {
		public void handle(Start event) {
			logger.info("Root started. Waiting for Commands.");
		}
	};  

	private Handler<RootInit> handleInit = new Handler<RootInit>() {
		public void handle(RootInit event) {
			self = event.getSelf();
			
			 String mavenHome = System.getProperty( "maven.home" );
			 if ( mavenHome != null )
			 {
				 System.setProperty( "maven.home", new File( "~/.m2/").getAbsolutePath() );
			 }
			 else
			 {
				 mavenHome = new File( "~/", ".m2").getAbsolutePath();
			 }

			String projectName = "blah";
			 
			String tmpDirectory = System.getProperty("java.io.tmpdir");
			
			System.out.println("Tmp dir = " + tmpDirectory);
			
			System.out.println("MAVEN_HOME = " + mavenHome);
			
			File tmpDir = new File(tmpDirectory, projectName);
			
	        File user = new File( mavenHome, "settings.xml" );

	        System.out.println("Settings file = " + user.toString());
	        
	        Configuration configuration = new DefaultConfiguration()
	            .setUserSettingsFile( user )
	            .setClassLoader( Thread.currentThread().getContextClassLoader() );

	        ConfigurationValidationResult validationResult = MavenEmbedder.validateConfiguration( configuration );

	        if ( validationResult.isValid() )
	        {
	            MavenEmbedder embedder=null;
				try {
					embedder = new MavenEmbedder( configuration );
				} catch (MavenEmbedderException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

	            MavenExecutionRequest request = new DefaultMavenExecutionRequest()
	                .setBaseDirectory(tmpDir )
	                .setGoals( Arrays.asList( new String[]{"install:install-file"} ) );

	            URI uriPom=null;
	            URI uriJar=null;	            
				try {
					uriPom = new URI("http://korsakov.sics.se/maven/repository/se/sics/kompics/kdld/pom.xml");
					uriJar = new URI("http://korsakov.sics.se/maven/repository/se/sics/kompics/kdld/kdld.jar");
				} catch (URISyntaxException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				File remotePom = new File(uriPom);
				File localPom = new File( tmpDir, "pom.xml" );
				try {
					copy(remotePom,localPom);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	            
	            File remoteJarFile = new File(uriJar);
				File localJarFile = new File( tmpDir, "kdld.jar" );
				try {
					copy(remoteJarFile,localJarFile);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				
	            request.setPom(remotePom);

	            // This doesn't work!
//	            request.setProperty("file", 
//	            		"http://korsakov.sics.se/maven/repository/se/sics/kompics/kdld/kdld.jar");
	            request.setProperty("pomFile",
        		"http://korsakov.sics.se/maven/repository/se/sics/kompics/kdld/kdld.jar");
	            
	            
	            
//	            ArtifactRepository repo = 
//	            request.addRemoteRepository();
	            

	            MavenExecutionResult result = embedder.execute( request );

	            if ( result.hasExceptions() )
	            {
//	            	fail();
	                try {
						embedder.stop();
					} catch (MavenEmbedderException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
               		String failMsg = ((Exception)result.getExceptions().get( 0 )).getMessage();
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

	            String environment = project.getProperties().getProperty( "environment" );

//	            assertEquals( "development", environment );

	            System.out.println( "You are working in the '" + environment + "' environment!" );
	        }
	        else
	        {
	            if ( ! validationResult.isUserSettingsFilePresent() )
	            {
	                System.out.println( "The specific user settings file '" + user + "' is not present." );
	            }
	            else if ( ! validationResult.isUserSettingsFileParses() )
	            {
	                System.out.println( "Please check your settings file, it is not well formed XML." );
	            }
	        }

			
			
		}
	};  

	
	private Handler<DeployRequest> handleDeployRequest = new Handler<DeployRequest>() {
		public void handle(DeployRequest event) {
			logger.info("Hello Event Received");
		}
	};  

	private Handler<Deploy> handleDeploy = new Handler<Deploy>() {
		public void handle(Deploy event) {
			String pomUri = event.getPomUri();
			
//			Address dest = new Address(repoId)
//			if (dest != null)
//				trigger(new DeployRequest(self, dest), net);
//			else
//				System.err.println("Couldn't send hello to neighbour. Couldn't find id: " + id);
		}
	};  

	
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
