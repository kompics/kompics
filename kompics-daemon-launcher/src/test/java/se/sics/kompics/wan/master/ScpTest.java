package se.sics.kompics.wan.master;

import java.io.File;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.commons.configuration.ConfigurationException;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.Credentials;
import se.sics.kompics.wan.master.plab.ExperimentHost;
import se.sics.kompics.wan.master.scp.DownloadUploadMgr;
import se.sics.kompics.wan.master.scp.DownloadUploadPort;
import se.sics.kompics.wan.master.scp.ScpComponent;
import se.sics.kompics.wan.master.scp.ScpPort;
import se.sics.kompics.wan.master.ssh.DownloadFileRequest;
import se.sics.kompics.wan.master.ssh.DownloadFileResponse;
import se.sics.kompics.wan.master.ssh.HaltRequest;
import se.sics.kompics.wan.master.ssh.HaltResponse;
import se.sics.kompics.wan.master.ssh.SshComponent;
import se.sics.kompics.wan.master.ssh.SshConnectRequest;
import se.sics.kompics.wan.master.ssh.SshConnectResponse;
import se.sics.kompics.wan.master.ssh.SshPort;
import se.sics.kompics.wan.master.ssh.UploadFileRequest;
import se.sics.kompics.wan.master.ssh.UploadFileResponse;

public class ScpTest  {

	public static final int SSH_CONNECT_TIMEOUT = 15000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 15000;

	private static Semaphore semaphore = new Semaphore(0);

	private static final int EVENT_COUNT = 1;
	
	
	public static void setTestObj(ScpTest testObj) {
		TestSshComponent.testObj = testObj;
	}
	
	public static class SshConnectTimeout extends Timeout {

		public SshConnectTimeout(ScheduleTimeout request) {
			super(request);
		}
	}


	public static class TestSshComponent extends ComponentDefinition {
		
		private Component sshComponent;
		private Component scpComponent;
		private Component downloadUploadMgComponent;

		private Component timer;
		
		private static ScpTest testObj = null;
		
		private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();
		
		public TestSshComponent() {

			timer = create(JavaTimer.class);
			sshComponent = create(SshComponent.class);
			scpComponent = create(ScpComponent.class);
			downloadUploadMgComponent = create(DownloadUploadMgr.class);
			
			connect(sshComponent.getNegative(DownloadUploadPort.class),
					downloadUploadMgComponent.getPositive(DownloadUploadPort.class));

			connect(downloadUploadMgComponent.getNegative(ScpPort.class),
					scpComponent.getPositive(ScpPort.class));

			subscribe(handleDownloadFileResponse, sshComponent.getPositive(SshPort.class));
			subscribe(handleUploadFileResponse, sshComponent.getPositive(SshPort.class));
			subscribe(handleSshConnectResponse, sshComponent.getPositive(SshPort.class));
			subscribe(handleHaltResponse, sshComponent.getPositive(SshPort.class));
			
			subscribe(handleSshConnectTimeout, timer.getPositive(Timer.class));
			subscribe(handleStart, control);
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {

				// TODO Auto-generated method stub
				Credentials cred = new Credentials("jdowling", "oke2Shoo", 
						"/home/jdowling/.ssh/id_rsa", "");
				ExperimentHost host = new ExperimentHost("lqist.com");
				
				trigger(new SshConnectRequest(cred, host), sshComponent.getPositive(SshPort.class));

				ScheduleTimeout st = new ScheduleTimeout(SSH_CONNECT_TIMEOUT);
				SshConnectTimeout connectTimeout = new SshConnectTimeout(st);
				st.setTimeoutEvent(connectTimeout);

				UUID timerId = connectTimeout.getTimeoutId();
				outstandingTimeouts.add(timerId);
				trigger(st, timer.getPositive(Timer.class));

			}
		};
		
		public Handler<SshConnectTimeout> handleSshConnectTimeout = new Handler<SshConnectTimeout>() {
			public void handle(SshConnectTimeout event) {
				
				if (!outstandingTimeouts.contains(event.getTimeoutId())) {
					return;
				}
				outstandingTimeouts.remove(event.getTimeoutId());

//				testObj.fail(true);
				System.out.println("Ssh connect timeout");
			}
		};
		
		
		public Handler<SshConnectResponse> handleSshConnectResponse = new Handler<SshConnectResponse>() {
			public void handle(SshConnectResponse event) {

				// remotePath  localFileOrDir fileFilter localNameType 
				DownloadFileRequest command = new DownloadFileRequest(event.getSessionId(), 
						"/home/jdowling/blah.lqist", "/home/jdowling/", "", SshComponent.FLAT, 
				10*1000, true);
				trigger(command, sshComponent.getPositive(SshPort.class));
			}
		};
		
		public Handler<DownloadFileResponse> handleDownloadFileResponse = 
			new Handler<DownloadFileResponse>() {
			public void handle(DownloadFileResponse event) {

				System.out.println(event.getFile().getAbsolutePath());
				UploadFileRequest command = new UploadFileRequest(event.getSessionId(), 
						new File("/home/jdowling/blah"), "/home/jdowling/", true, 
						10*1000, true);
				trigger(command, sshComponent.getPositive(SshPort.class));
				
			}
		};
		
		public Handler<UploadFileResponse> handleUploadFileResponse = 
			new Handler<UploadFileResponse>() {
			public void handle(UploadFileResponse event) {

				System.out.println(event.getFile().getAbsolutePath());
				
				trigger(new HaltRequest(event.getSessionId()), 
						sshComponent.getPositive(SshPort.class));
			}
		};
		
		public Handler<HaltResponse> handleHaltResponse = new Handler<HaltResponse>() {
			public void handle(HaltResponse event) {
				testObj.pass();

			}
		};
	};

	
	public ScpTest() {
		
		/*
		Connection sshConn = new Connection("lqist.com");

		try {

			System.out.println("Connecting");

			sshConn
					.connect(null, SSH_CONNECT_TIMEOUT,
							SSH_KEY_EXCHANGE_TIMEOUT);

			if (sshConn.authenticateWithPublicKey("jdowling", new File(
					"/home/jdowling/.ssh/id_rsa"), "")) {
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
		*/
	}

	@org.junit.Test 
	public void testSsh()
	{
		
		setTestObj(this);
		try {
			Configuration.init(new String[]{}, PlanetLabConfiguration.class);
			Kompics.createAndStart(ScpTest.TestSshComponent.class, 1);
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			ScpTest.semaphore.acquire(EVENT_COUNT);
			System.out.println("Exiting unit test....");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		
	}
	
	public void pass() {
		org.junit.Assert.assertTrue(true);
		semaphore.release();
	}

	public void fail(boolean release) {
		org.junit.Assert.assertTrue(false);
		if (release == true) {
			semaphore.release();
		}
	}
}
