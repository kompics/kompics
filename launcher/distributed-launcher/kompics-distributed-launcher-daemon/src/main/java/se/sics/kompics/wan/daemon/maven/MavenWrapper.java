package se.sics.kompics.wan.daemon.maven;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import java.util.Properties;
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
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MavenWrapper {

    private MavenEmbedder embedder;
    private String pomFilename;
    private static final Logger logger = LoggerFactory.getLogger(MavenWrapper.class);
    private static final Properties PACKAGING_PROPERTIES = new Properties();

    static {
        PACKAGING_PROPERTIES.put("maven.test.skip", "true");
    }

    public MavenWrapper(String pomFilename) throws MavenExecException {
        if (pomFilename == null) {
            throw new MavenExecException("pomFilename was null");
        }
        this.pomFilename = pomFilename;

        Configuration configuration = new DefaultConfiguration()
//                .setUserSettingsFile(MavenEmbedder.DEFAULT_USER_SETTINGS_FILE)
//                .setGlobalSettingsFile(MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE)
                .setClassLoader(Thread.currentThread().getContextClassLoader());


        File user = MavenEmbedder.DEFAULT_USER_SETTINGS_FILE;

        if (user.exists() == false) {
            logger.warn("{} maven configuration file Not Found", user.getAbsolutePath());
        } else {
            configuration.setUserSettingsFile(user);
        }

        ConfigurationValidationResult validationResult =
                MavenEmbedder.validateConfiguration(configuration);

        if (validationResult.isValid() == false) {
            logger.warn("Invalid configuration. Check {} maven configuration file for problem.", user.getAbsolutePath());
            throw new MavenExecException("Invalid configuration. Check {} maven configuration file for problem.");
        }

        logger.info("Creating MavenEmbedder...");
        try {
            embedder = new MavenEmbedder(configuration);
        } catch (MavenEmbedderException e1) {
            e1.printStackTrace();
            ComponentLookupException ce = (ComponentLookupException) e1.getCause();
            logger.warn("Component Exception: {}", ce.getMessage());
            throw new MavenExecException(e1.getMessage());
        }
        logger.info("Successfully created MavenEmbedder...");
    }

    public MavenExecutionResult execute(String command, Map<String,String> props)
            throws MavenExecException {
        if (command.compareTo("exec:exec") != 0 && command.compareTo("assembly:assembly") != 0) {
            throw new MavenExecException("Invalid command: " + command + " . Only exec:exec and assembly:assembly supported.");
        }

        File pomFile = new File(pomFilename);
        if (pomFile == null || pomFile.exists() == false) {
            logger.error("pomfile problem: " + pomFile);
            throw new MavenExecException("pomfile problem: " + pomFile);
        }

        File baseDir = new File(pomFile.getParent());
        logger.info("Executing maven: " + command + " in directory:" + baseDir.getAbsolutePath() + " for pom file: " + pomFilename);
        if (baseDir == null || baseDir.exists() == false) {
            logger.error("baseDir for pomfile problem: " + pomFile);
            throw new MavenExecException("basedir for pomfile problem: " + pomFile);
        }
        MavenExecutionRequest request =
                new DefaultMavenExecutionRequest().setBaseDirectory(baseDir).setGoals(Arrays.asList(new String[]{command}));

        request.setInteractiveMode(false);
        request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_WARN);
        request.setUsePluginUpdateOverride(true);
        request.setShowErrors(true);
        request.setPom(pomFile);
        request.setRecursive(true);
//        request.setUpdateSnapshots(true);

        if (props != null) {
            for (String key : props.keySet()) {
                PACKAGING_PROPERTIES.put(key, props.get(key));
            }
        }

        request.setProperties(PACKAGING_PROPERTIES);

        MavenExecutionResult result = embedder.execute(request);

        if (result.hasExceptions()) {
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
                    exception.printStackTrace(System.err);
                }
            }

            String failMsg = ((Exception) result.getExceptions().get(0)).getMessage();
            logger.error(failMsg);
            throw new MavenExecException(failMsg);
        }

        return result;
    }

    public String getPomFilename() {
        return pomFilename;
    }

    PlexusContainer getPlexusContainer() {
        return embedder.getPlexusContainer();
    }

    private void dumpProject(MavenExecutionResult result) {
        MavenProject project = result.getProject();
        logger.info("<project information>");
        logger.info(project.getGroupId() + ":" + project.getArtifactId() + ":" +
                project.getVersion() + "  for Job " + project.getId());
        logger.info("Base directory = " + project.getBasedir());
        logger.info("Class = " + project.getClass());
        logger.info("</project information>");
    }
}
