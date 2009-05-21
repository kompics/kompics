package se.sics.kompics.kdld.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.kdld.daemon.maven.MavenExecException;
import se.sics.kompics.kdld.daemon.maven.MavenWrapper;


public class MavenExecMain{
	
	private static final Logger logger = LoggerFactory
	.getLogger(MavenExecMain.class);


	public static void main(String[] args) {
		
		if (args.length != 1)
		{
			System.out.println("This main is called by the mavenLauncher component.");
			System.out.println("Usage: <prog> pomFilename");
			System.out.println("Num of args was " + args.length);
			System.exit(-1);
		}
		
		String pomFilename = args[0];


		MavenWrapper mw;
		try {
			mw = new MavenWrapper(pomFilename);
			mw.execute("exec:exec");
		} catch (MavenExecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
