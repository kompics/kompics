package se.sics.kompics.wan.master;

import java.util.HashSet;
import java.util.Set;
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
import se.sics.kompics.wan.plab.events.GetHostsInSliceRequest;
import se.sics.kompics.wan.plab.events.GetHostsInSliceResponse;
import se.sics.kompics.wan.plab.events.PLabInit;
import se.sics.kompics.wan.plab.events.QueryPLabSitesResponse;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.SshComponent;
import se.sics.kompics.wan.ssh.SshPort;
import se.sics.kompics.wan.ssh.events.SshConnectRequest;
import se.sics.kompics.wan.ssh.events.SshConnectResponse;

public class PLabTest  {

	public static final int PLAB_CONNECT_TIMEOUT = 30 * 1000;

	private static Semaphore semaphore = new Semaphore(0);

	private static final int EVENT_COUNT = 1;
	
	
	public static void setTestObj(PLabTest testObj) {
		TestPLabComponent.testObj = testObj;
	}
	
	public static class PLabConnectTimeout extends Timeout {

		private final int numRetries;
		
		public PLabConnectTimeout(ScheduleTimeout request, int numRetries) {
			super(request);
			this.numRetries = numRetries;
		}
		
		public int getNumRetries() {
			return numRetries;
		}
	}


	public static class TestPLabComponent extends ComponentDefinition {
		
		private Component pLabComponent;
		
		private Component sshComponent;

		private Component timer;
		
		private static PLabTest testObj = null;
		
		private final HashSet<UUID> outstandingTimeouts = new HashSet<UUID>();

		private PlanetLabCredentials cred = 
			new PlanetLabCredentials("kost@sics.se", "kostjap", "sics_grid4all",
					"/home/jdowling/.ssh/id_rsa", "");

		private Set<PLabHost> hosts = new HashSet<PLabHost>();
		
		private int replies = 0;
		
		public TestPLabComponent() {

			timer = create(JavaTimer.class);
			pLabComponent = create(PLabComponent.class);
			sshComponent = create(SshComponent.class);
			
			trigger(new PLabInit(cred),pLabComponent.getControl());
			
			subscribe(handleQueryPLabSitesResponse, pLabComponent.getPositive(PLabPort.class));
			subscribe(handleGetNodesForSliceResponse, pLabComponent.getPositive(PLabPort.class));
			
			subscribe(handleSshConnectResponse, sshComponent.getPositive(SshPort.class));
			
			subscribe(handleStart, control);
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {

				GetHostsInSliceRequest req = new GetHostsInSliceRequest(cred, true);
				trigger(req, pLabComponent.getPositive(PLabPort.class));

			}
		};
		
		public Handler<GetHostsInSliceResponse> handleGetNodesForSliceResponse 
		= new Handler<GetHostsInSliceResponse>() {
			public void handle(GetHostsInSliceResponse event) {
				
				hosts = event.getHosts();
				
				for (PLabHost h : hosts) {

					trigger(new SshConnectRequest(cred, UUID.randomUUID(), new ExperimentHost(h)), 
							sshComponent.getPositive(SshPort.class));

					try {
						PlanetLabConfiguration.getNetworkIntensiveTicket();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					
					ScheduleTimeout st = new ScheduleTimeout(PLAB_CONNECT_TIMEOUT);
					PLabConnectTimeout connectTimeout = new PLabConnectTimeout(st, 0);
					st.setTimeoutEvent(connectTimeout);

					UUID timerId = connectTimeout.getTimeoutId();
					outstandingTimeouts.add(timerId);
					trigger(st, timer.getPositive(Timer.class));				

					PlanetLabConfiguration.releaseNetworkIntensiveTicket();
				}
				
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
		
		public Handler<SshConnectResponse> handleSshConnectResponse = new Handler<SshConnectResponse>() {
			public void handle(SshConnectResponse event) {
				
				System.out.println("Connected to: " + event.getHostname());
				
				replies++;
				if (replies == hosts.size()) {
					testObj.pass();
				}
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
			Kompics.createAndStart(PLabTest.TestPLabComponent.class, 2);
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
