package se.sics.kompics.kdld;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ScriptBuilder {
	
	private static String CATALOG_FILE = "http://korsakov.sics.se/maven/daemon-launcher-catalog.xml";
	private static String ARCHETYPE_GROUP_ID = "se.sics.kompics";
	private static String ARCHETYPE_ARTIFACT_ID = "kompics-archetype-dl";
	
	private String pathname;
	private String filename;
	
	public ScriptBuilder(String pathname, String filename, String artifactVersion,
			String groupId, String artifactId, String version, String mainClass
			) throws IOException {
		this.pathname = pathname;
		this.filename = filename;
		
        String osName = System.getProperty("os.name" );
        
        System.out.println(osName);
        
        String[] command = null;
        if( osName.equals( "Windows NT" ) )
        {
        	command = new String[3];
            command[0] = "cmd.exe" ;
            command[1] = "/C" ;
            command[2] = pathname + "\" + filename";
        }
        else if( osName.equals( "Windows 95" ) )
        {
        	command = new String[3];
            command[0] = "command.com" ;
            command[1] = "/C" ;
            command[2] = pathname + "\" + filename";
        }
        else if ( osName.equals( "Linux"))
        {
        	command = new String[1];
        	command[0] = pathname + "/" + filename ;        	
        }

		
		FileWriter writer = new FileWriter(new File(pathname, filename));
		writer.write("#!/bin/bash\n");
		writer.write("rm -rf kompics-manual\n");
		writer
				.write("mvn archetype:generate"
						+ " -DarchetypeCatalog=" + ScriptBuilder.CATALOG_FILE
						+ " -DarchetypeGroupId=" + ScriptBuilder.ARCHETYPE_GROUP_ID
						+ " -DarchetypeArtifactId=" + ScriptBuilder.ARCHETYPE_ARTIFACT_ID
						+ " -DarchetypeVersion=" + artifactVersion
						+ " -DgroupId=" + groupId
						+ " -DartifactId=" + artifactId
						+ " -DmainClass=" + mainClass
						+ " -Dversion=" + version
						+ " -DinteractiveMode=false " + "\n");
		writer.flush();
		writer.close();
		String[] cmd = {"chmod", "+x", pathname + "/" + filename};
		ProcessBuilder pb = new ProcessBuilder(cmd);
		Process process = pb.start(); 
		
		StreamConsumer errorConsumer = new StreamConsumer(process.getErrorStream(), "ERROR");
		StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), "OUTPUT");
		errorConsumer.start();
		outputConsumer.start();
		int exitValue;
		try {
			exitValue = process.waitFor();
			System.out.println("exit value " + exitValue);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public int runScript() throws java.io.IOException {

		String[] cmd = { pathname + "/" + filename};
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(new File(pathname));
		Process process = pb.start();
		
		StreamConsumer errorConsumer = new StreamConsumer(process.getErrorStream(), "ERROR");
		StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), "OUTPUT");
		errorConsumer.start();
		outputConsumer.start();
		int exitValue;
		try {
			exitValue = process.waitFor();
			System.out.println("exit value " + exitValue);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -10;
		}
		return exitValue;
	}
	
	class StreamConsumer extends Thread {
	    InputStream is;
	    String type;

	    StreamConsumer(InputStream is, String type) {
	        this.is = is;
	        this.type = type;
	    }

	    public void run() {
	        try {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null)
	                System.out.println(type + ">" + line);
	            br.close();
	            isr.close();
	        } catch (IOException ioe) {
	            ioe.printStackTrace();  
	        }
	    }
	};
};