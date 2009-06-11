package se.sics.kompics.wan.master;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SshTest extends Thread {

	public static final int SSH_CONNECT_TIMEOUT = 15000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 15000;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new SshTest();
	}

	public SshTest() {
		Connection sshConn = new Connection("barb");

		try {

			System.out.println("Connecting");

			sshConn
					.connect(null, SSH_CONNECT_TIMEOUT,
							SSH_KEY_EXCHANGE_TIMEOUT);

			if (sshConn.authenticateWithPublicKey("isdal", new File(
					"H:\\ssh\\identity"), "")) {
				System.out.println("Connected");

				Session session = sshConn.openSession();

				StreamGobbler stdout = new StreamGobbler(session.getStdout());

				OutputStream stdin = session.getStdin();

				// session.execCommand("ls /bin");
				session.startShell();
				// session.execCommand("ls");

				stdin.write("ls -la\n".getBytes());
				stdin.write("echo \"=:=:=EXIT STATUS==$?==\"\n".getBytes());

				session.waitForCondition(ChannelCondition.STDOUT_DATA, 5000);

				Thread.sleep(200);
				while (stdout.available() > 0) {
					byte[] response = new byte[stdout.available()];

					stdout.read(response, 0, response.length);
					System.out.println(new String(response));
					Thread.sleep(200);
				}
				int cond = session.waitForCondition(ChannelCondition.EOF, 5000);
				System.out.println( "EOF.. = " + cond);
				session.waitForCondition(ChannelCondition.EXIT_SIGNAL, 5000);
				System.out.println("exit: " + session.getExitStatus());

				session.close();
				sshConn.close();

			}

		} catch (SocketTimeoutException e) {
			System.err.println("connection timeout: " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
