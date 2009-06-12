package se.sics.kompics.wan.master;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;

import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;
import se.sics.kompics.wan.master.scp.upload.UploadManager;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshConnection;

public class ScpTest extends Thread {

	public static final int SSH_CONNECT_TIMEOUT = 15000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 15000;

	public static final String PRIVATE_KEY = "/home/jdowling/.ssh/id_rsa";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			Configuration.init(args, PlanetLabConfiguration.class);
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new ScpTest();
	}

	public ScpTest() {
		PlanetLabHost plh = new PlanetLabHost("lqist.com");
		// "sics_gradient", "role",
		Credentials cred = new Credentials("jdowling", "password", 
				PRIVATE_KEY, "key_password");
//		ConnectionController cc = new ConnectionController(cred);
		SshConnection sshConn = new SshConnection(cred, plh);

		Thread t = new Thread(sshConn);
		t.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		CommandSpec commandSpec = new CommandSpec("", 15000, 1, false);
		
//		sshConn.upload(new File("pom.xml"), commandSpec);
		
		UploadManager.getInstance().uploadDir(sshConn, new File("pom.xml"), commandSpec);
		
//		try {
//			RpcClient.getInstance().startClient(InetAddress.getLocalHost(), cred);
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		RpcServer.getInstance().startController(cred);
		
//		RpcClient.getInstance().upload(cred.getAuthMap(), 
//				"/home/jdowling/workspace/kompics-daemon-launcher/pom.xml", 
//				10*1000, false);
		

		
		
	}

}
