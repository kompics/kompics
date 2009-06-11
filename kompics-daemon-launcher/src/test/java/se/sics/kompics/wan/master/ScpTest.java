package se.sics.kompics.wan.master;

import java.io.File;

import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.plc.PlanetLabHost;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.ConnectionController;
import se.sics.kompics.wan.master.ssh.SshConnection;

public class ScpTest extends Thread {

	public static final int SSH_CONNECT_TIMEOUT = 15000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 15000;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new ScpTest();
	}

	public ScpTest() {
		PlanetLabHost plh = new PlanetLabHost("lqist.com");
		Credentials creds = new Credentials("jdowling", "password", "sics_gradient", "role",
				"/home/jdowling/.ssh/id_rsa", "key_password");
		ConnectionController cc = new ConnectionController(creds);
		SshConnection sshConn = new SshConnection(cc, plh);

		CommandSpec commandSpec = new CommandSpec("ls -al", 15000, 1, false);
		sshConn.upload(new File("pom.xml"), commandSpec);

	}

}
