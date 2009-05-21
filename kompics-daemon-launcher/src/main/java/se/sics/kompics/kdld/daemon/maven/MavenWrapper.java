package se.sics.kompics.kdld.daemon.maven;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.kdld.daemon.Daemon;

public class MavenWrapper {

	private MavenEmbedder embedder;
	
	private String pomFilename;
	
	private static final Logger logger = LoggerFactory
	.getLogger(MavenWrapper.class);
	
	public MavenWrapper(String pomFilename) throws MavenExecException
	{
		if (pomFilename == null)
		{
			throw new MavenExecException("pomFilename was null");
		}
		this.pomFilename = pomFilename;
		
		File user = new File( Daemon.MAVEN_HOME, "settings.xml" );
				
        Configuration configuration = new DefaultConfiguration()
        .setUserSettingsFile( user )
        .setClassLoader( Thread.currentThread().getContextClassLoader() );

        ConfigurationValidationResult validationResult = 
        	MavenEmbedder.validateConfiguration( configuration );

        if ( validationResult.isValid() == false)
        {
        	logger.warn("Invalid maven settings.xml configuration");
        }
		try {
			embedder = new MavenEmbedder( configuration );
		} catch (MavenEmbedderException e1) {
			e1.printStackTrace();
			throw new MavenExecException(e1.getMessage());
		}
		
      
	}
	
	public MavenExecutionResult execute(String command)
		throws MavenExecException
	{
		if (command.compareTo("exec:exec") != 0 && command.compareTo("assembly:assembly") != 0) 
		{
			throw new MavenExecException("Invalid command: " + command 
					+ " . Only exec:exec and assembly:assembly supported.");
		}
		
		File pomFile = new File(pomFilename);
        if (pomFile == null || pomFile.exists() == false)
        {
        	logger.error("pomfile problem: " + pomFile);
        	throw new MavenExecException("pomfile problem: " + pomFile);
        }

        File baseDir = new File(pomFile.getParent());
        if (baseDir == null || baseDir.exists() == false)
        {
        	logger.error("baseDir for pomfile problem: " + pomFile);
        	throw new MavenExecException("basedir for pomfile problem: " + pomFile);
        }
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
        .setBaseDirectory( baseDir )
        .setGoals( Arrays.asList( new String[]{command} ) );
	
		request.setInteractiveMode(false);
		request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_FATAL);
		request.setUsePluginUpdateOverride(true);
		request.setShowErrors(false);
        request.setPom(pomFile);

        MavenExecutionResult result = embedder.execute( request );

        if ( result.hasExceptions() )
        {
            try {
				embedder.stop();
			} catch (MavenEmbedderException e) {
				logger.warn("Problem when trying to stop maven embedder: " + e.getMessage());
			} 
	        List exceptions = result.getExceptions();
	        if (!((exceptions == null) || exceptions.isEmpty())) {
	            logger.error("Encountered " + exceptions.size() + "	exception(s).");
	            Iterator it = exceptions.iterator();
	            while (it.hasNext()) {
	                Exception exception = (Exception) it.next();
//	                exception.printStackTrace(System.err);
	            }
	        }
			
       		String failMsg = ((Exception)result.getExceptions().get( 0 )).getMessage();
       		logger.error(failMsg);
       		throw new MavenExecException(failMsg);
        }

        return result;
	}
	
	public String getPomFilename() {
		return pomFilename;
	}
	
	PlexusContainer getPlexusContainer()
	{
		return embedder.getPlexusContainer();
	}
	
	private void dumpProject(MavenExecutionResult result)
	{
        MavenProject project = result.getProject();
        logger.info("<project information>");
        logger.info(project.getGroupId() + ":" + project.getArtifactId() + ":" +
        		project.getVersion() + "  for Job " + project.getId());
        logger.info("Base directory = " + project.getBasedir());
        logger.info("Class = " + project.getClass());
        logger.info("</project information>");
	}
}
