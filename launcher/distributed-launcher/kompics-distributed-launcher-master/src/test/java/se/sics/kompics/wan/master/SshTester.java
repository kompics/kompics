package se.sics.kompics.wan.master;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;
import se.sics.kompics.wan.ssh.SshComponent;
import se.sics.kompics.wan.ssh.SshCredentials;
import se.sics.kompics.wan.ssh.SshPort;
import se.sics.kompics.wan.ssh.events.SshCommandRequest;
import se.sics.kompics.wan.ssh.events.SshCommandResponse;
import se.sics.kompics.wan.ssh.events.HaltRequest;
import se.sics.kompics.wan.ssh.events.HaltResponse;
import se.sics.kompics.wan.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.ssh.events.SshConnectResponse;

public class SshTester  {
	
	private static final Logger logger = LoggerFactory
	.getLogger(SshTester.class);

	public static final int SSH_CONNECT_TIMEOUT = 30 * 1000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 30 * 1000;

	private static Semaphore semaphore = new Semaphore(0);

	private static final int EVENT_COUNT = 1;
	
	public static void setTestObj(SshTester testObj) {
		TestSshComponent.testObj = testObj;
	}
	
	public static class SshConnectTimeout extends Timeout {

		public SshConnectTimeout(ScheduleTimeout request) {
			super(request);
		}
	}


	public static class TestSshComponent extends ComponentDefinition {
		
		private Component sshComponent;

		private Component timer;
		
		private static SshTester testObj = null;
		
		private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();
		
		public TestSshComponent() {

			timer = create(JavaTimer.class);
			sshComponent = create(SshComponent.class);
			
			subscribe(handleCommandResponse, sshComponent.getPositive(SshPort.class));
			subscribe(handleSshConnectResponse, sshComponent.getPositive(SshPort.class));
			subscribe(handleHaltResponse, sshComponent.getPositive(SshPort.class));
			
			subscribe(handleSshConnectTimeout, timer.getPositive(Timer.class));
			subscribe(handleStart, control);
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {

				// TODO Auto-generated method stub
//				Credentials cred = new SshCredentials("sics_grid4all", "", 
//						"/home/jdowling/.ssh/id_rsa", "");
//				Host host = new ExperimentHost("planetlab3.ani.univie.ac.at");
				
				Credentials cred = new SshCredentials("csl", "", 
						"/home/jdowling/.ssh/id_rsa", "");
				Host host = new ExperimentHost("evgsics1.sics.se");
				
				ScheduleTimeout st = new ScheduleTimeout(SSH_CONNECT_TIMEOUT);
				SshConnectTimeout connectTimeout = new SshConnectTimeout(st);
				st.setTimeoutEvent(connectTimeout);

				UUID timerId = connectTimeout.getTimeoutId();
				outstandingTimeouts.add(timerId);
				
				logger.info("Sending ssh connect request");
				trigger(new SshConnectRequest(cred, timerId, host),  
						sshComponent.getPositive(SshPort.class));

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

				logger.info("Received ssh connect response.");
				
				logger.info("Sending 'ls -al' command.");
				SshCommandRequest command = new SshCommandRequest(UUID.randomUUID(),
						event.getSessionId(), "ls -la", 
						10*1000, true);
				trigger(command, sshComponent.getPositive(SshPort.class));
			}
		};
		
		public Handler<SshCommandResponse> handleCommandResponse = new Handler<SshCommandResponse>() {
			public void handle(SshCommandResponse event) {

				logger.info("Received ssh command response.");
				System.out.println(event.getCommandResponse());
				
				trigger(new HaltRequest(event.getSessionId()), sshComponent.getPositive(SshPort.class));
			}
		};
		
		public Handler<HaltResponse> handleHaltResponse = new Handler<HaltResponse>() {
			public void handle(HaltResponse event) {
				testObj.pass();

			}
		};
	};

	
	public SshTester() {
		
	}

//	@org.junit.Test
        @Ignore
	public void testSsh()
	{
		
		setTestObj(this);
		try {
			Configuration.init(new String[]{}, PlanetLabConfiguration.class);
			Kompics.createAndStart(SshTester.TestSshComponent.class, 1);
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			SshTester.semaphore.acquire(EVENT_COUNT);
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
