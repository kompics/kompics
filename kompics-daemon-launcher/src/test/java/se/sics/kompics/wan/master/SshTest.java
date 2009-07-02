package se.sics.kompics.wan.master;

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
import se.sics.kompics.wan.master.ssh.HaltRequest;
import se.sics.kompics.wan.master.ssh.HaltResponse;
import se.sics.kompics.wan.master.ssh.CommandRequest;
import se.sics.kompics.wan.master.ssh.CommandResponse;
import se.sics.kompics.wan.master.ssh.SshComponent;
import se.sics.kompics.wan.master.ssh.SshConnectRequest;
import se.sics.kompics.wan.master.ssh.SshConnectResponse;
import se.sics.kompics.wan.master.ssh.SshPort;

public class SshTest  {

	public static final int SSH_CONNECT_TIMEOUT = 30 * 1000;

	public static final int SSH_KEY_EXCHANGE_TIMEOUT = 30 * 1000;

	private static Semaphore semaphore = new Semaphore(0);

	private static final int EVENT_COUNT = 1;
	
	
	public static void setTestObj(SshTest testObj) {
		TestSshComponent.testObj = testObj;
	}
	
	public static class SshConnectTimeout extends Timeout {

		public SshConnectTimeout(ScheduleTimeout request) {
			super(request);
		}
	}


	public static class TestSshComponent extends ComponentDefinition {
		
//		private Positive<SshPort> sshPort;

		private Component sshComponent;

		private Component timer;
		
		private static SshTest testObj = null;
		
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

				CommandRequest command = new CommandRequest(event.getSessionId(), "ls -la", 
						10*1000, true);
				trigger(command, sshComponent.getPositive(SshPort.class));
			}
		};
		
		public Handler<CommandResponse> handleCommandResponse = new Handler<CommandResponse>() {
			public void handle(CommandResponse event) {

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

	
	public SshTest() {
		
	}

	@org.junit.Test 
	public void testSsh()
	{
		
		setTestObj(this);
		try {
			Configuration.init(new String[]{}, PlanetLabConfiguration.class);
			Kompics.createAndStart(SshTest.TestSshComponent.class, 1);
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			SshTest.semaphore.acquire(EVENT_COUNT);
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
