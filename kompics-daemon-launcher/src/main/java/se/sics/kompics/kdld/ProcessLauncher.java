package se.sics.kompics.kdld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ProcessLauncher {
	
	private final String classpath = System.getProperty("java.class.path");
	private final String mainClass;
	private final String log4jProperties;
	private final String jarWithDependenciesFile;
	

	public ProcessLauncher(String mainClass, String log4jProperties, String jarWithDependenciesFile) {
		super();
		this.mainClass = mainClass;
		this.log4jProperties = log4jProperties;
		this.jarWithDependenciesFile = jarWithDependenciesFile;
	}

	public int createProcess()
	{
		ProcessBuilder processBuilder = new ProcessBuilder("java",
				"-classpath", classpath, log4jProperties, "-jar", jarWithDependenciesFile,
				mainClass);
		processBuilder.redirectErrorStream(true);

		try {
			Process process = processBuilder.start();
			BufferedReader out = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			BufferedWriter input = new BufferedWriter(new OutputStreamWriter(process
					.getOutputStream()));

		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return 0;
	}
};