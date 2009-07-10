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
import se.sics.kompics.wan.plab.PLabComponent;
import se.sics.kompics.wan.plab.PLabHost;
import se.sics.kompics.wan.plab.PLabPort;
import se.sics.kompics.wan.plab.PlanetLabCredentials;
import se.sics.kompics.wan.plab.events.GetAllHostsResponse;
import se.sics.kompics.wan.plab.events.QueryPLabSitesResponse;
import se.sics.kompics.wan.ssh.Credentials;
import se.sics.kompics.wan.ssh.ExperimentHost;

public class PLabTest  {

	public static final int PLAB_CONNECT_TIMEOUT = 30 * 1000;

	private static Semaphore semaphore = new Semaphore(0);

	private static final int EVENT_COUNT = 1;
	
	
	public static void setTestObj(PLabTest testObj) {
		TestPLabComponent.testObj = testObj;
	}
	
	public static class PLabConnectTimeout extends Timeout {

		public PLabConnectTimeout(ScheduleTimeout request) {
			super(request);
		}
	}


	public static class TestPLabComponent extends ComponentDefinition {
		
		private Component pLabComponent;

		private Component timer;
		
		private static PLabTest testObj = null;
		
		private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();
		
		public TestPLabComponent() {

			timer = create(JavaTimer.class);
			pLabComponent = create(PLabComponent.class);
			
			subscribe(handleGetAllHostsResponse, pLabComponent.getPositive(PLabPort.class));
			subscribe(handleQueryPLabSitesResponse, pLabComponent.getPositive(PLabPort.class));
			
			
			subscribe(handleStart, control);
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {

				// TODO Auto-generated method stub
				PlanetLabCredentials cred = 
					new PlanetLabCredentials("", "", "",
							"user",
						"/home/jdowling/.ssh/id_rsa", "");
				PLabHost host = new PLabHost("lqist.com", 4);
				
//				trigger(new SshConnectRequest(cred, host), pLabComponent.getPositive(SshPort.class));

				ScheduleTimeout st = new ScheduleTimeout(PLAB_CONNECT_TIMEOUT);
				PLabConnectTimeout connectTimeout = new PLabConnectTimeout(st);
				st.setTimeoutEvent(connectTimeout);

				UUID timerId = connectTimeout.getTimeoutId();
				outstandingTimeouts.add(timerId);
				trigger(st, timer.getPositive(Timer.class));

			}
		};
		
		public Handler<QueryPLabSitesResponse> handleQueryPLabSitesResponse = new Handler<QueryPLabSitesResponse>() {
			public void handle(QueryPLabSitesResponse event) {
			}
		};
		
		public Handler<PLabConnectTimeout> handlePLabConnectTimeout = new Handler<PLabConnectTimeout>() {
			public void handle(PLabConnectTimeout event) {
				
				if (!outstandingTimeouts.contains(event.getTimeoutId())) {
					return;
				}
				outstandingTimeouts.remove(event.getTimeoutId());

				testObj.fail(true);
				System.out.println("PLab connect timeout");
			}
		};
		
		
		
		public Handler<GetAllHostsResponse> handleGetAllHostsResponse = new Handler<GetAllHostsResponse>() {
			public void handle(GetAllHostsResponse event) {

			}
		};
		
	};

	
	public PLabTest() {
		
	}

	@org.junit.Test 
	public void testPLab()
	{
		
		setTestObj(this);
		try {
			Configuration.init(new String[]{}, PlanetLabConfiguration.class);
			Kompics.createAndStart(PLabTest.TestPLabComponent.class, 1);
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			PLabTest.semaphore.acquire(EVENT_COUNT);
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
