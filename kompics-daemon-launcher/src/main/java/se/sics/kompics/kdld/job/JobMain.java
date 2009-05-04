package se.sics.kompics.kdld.job;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.PropertyConfigurator;
import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobMain {
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}

	private static String envPath;
	private static String kompicsPath;
	private static String filePath;

	private static String groupId;
	private static String artifactId;
	private static String sepStr;

//	private static SimulationScenario scenario;

	private static final Logger logger = LoggerFactory.getLogger(JobMain.class);
	
	public static void main(String[] args) {
		
		if (args.length < 3)
		{
			logger.error("usage: <prog> groupId artifactId assembly|exec [...]");
			System.exit(0);
		}
		groupId = args[0];
		artifactId = args[1];
		String assemblyC = args[2];
		boolean assembly = (assemblyC.compareTo("assembly") == 0) ? true : false;
		boolean exec = (assemblyC.compareTo("exec") == 0) ? true : false;
		if (!(assembly || exec))
		{
			logger.error("usage: <prog> groupId artifactId assembly|exec [...]");
			System.exit(0);
		}
		
//		try {
//			scenario = SimulationScenario.load(System.getProperty(Daemon.SCENARIO_FILENAME));
//		} catch (SimulationScenarioLoadException e) {
//			e.printStackTrace();
//			logger.error("Problem loading SimulationScenario file: " + e.getMessage());
//			System.exit(-1);
//		}

		String groupPath = groupId.replace('.', File.separatorChar);

		char[] separator = new char[1];
		separator[0] = File.separatorChar;
		sepStr = new String(separator);

		filePath = groupPath + sepStr + artifactId;
		kompicsPath = System.getProperty("kompics.home"); // "java.io.tmpdir"
		envPath = kompicsPath;
		try {
			if (exec == true) {
				execExec();
			}
			else if (assembly == true)
			{
				assemblyAssembly();
			}
		} catch (DummyPomConstructionException e) {
			// TODO send fault back to Daemon??
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
	
	public static void assemblyAssembly() throws DummyPomConstructionException {
		mvn("assembly:assembly");
	}

	private static void execExec() throws DummyPomConstructionException {
		mvn("exec:exec");
	}

	private static void mvn(String command) throws DummyPomConstructionException {
		Configuration configuration = new DefaultConfiguration().setUserSettingsFile(
				MavenEmbedder.DEFAULT_USER_SETTINGS_FILE).setClassLoader(
				Thread.currentThread().getContextClassLoader()).setGlobalSettingsFile(
				MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE);

		MavenEmbedder embedder = null;
		try {
			embedder = new MavenEmbedder(configuration);
		} catch (MavenEmbedderException e1) {
			e1.printStackTrace();
			throw new DummyPomConstructionException(e1.getMessage());
		}

		File pomDir = new File(envPath, filePath);
		MavenExecutionRequest requestExec = new DefaultMavenExecutionRequest().setBaseDirectory(
				pomDir).setGoals(Arrays.asList(new String[] { command }));
		requestExec.setInteractiveMode(false);
		requestExec.setLoggingLevel(1);
		requestExec.setShowErrors(true);
		mavenExec(requestExec, embedder);
	}

	private static void mavenExec(MavenExecutionRequest request, MavenEmbedder embedder)
			throws DummyPomConstructionException {
		MavenExecutionResult result = embedder.execute(request);
		// ----------------------------------------------------------------------------
		// You may want to inspect the project after the execution.
		// ----------------------------------------------------------------------------
		if (mavenResult(result, embedder) == false) {
			throw new DummyPomConstructionException("Maven exec:exec problem");
		}
		MavenProject project = result.getProject();

		// Do something with the project
		String gId = project.getGroupId();
		String aId = project.getArtifactId();
		String v = project.getVersion();
		String name = project.getName();
		String environment = project.getProperties().getProperty("environment");
		System.out.println("You are working in the '" + environment + "' environment! " + gId + ":"
				+ aId + ":" + v + " - " + name);

	}

	private static boolean mavenResult(MavenExecutionResult result, MavenEmbedder embedder)
			throws DummyPomConstructionException {
		if (result.hasExceptions()) {
			try {
				String failMsg = ((Exception) result.getExceptions().get(0)).getMessage();
				System.err.println(failMsg);
				embedder.stop();
			} catch (MavenEmbedderException e) {
				throw new DummyPomConstructionException(e.getMessage());
			}
			return false;
		}
		return true;
	}

}
