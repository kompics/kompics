package se.sics.kompics.wan.master;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.commons.configuration.ConfigurationException;

import org.junit.Ignore;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.hosts.HostsPort;
import se.sics.kompics.wan.hosts.HostsXMLComponent;
import se.sics.kompics.wan.hosts.events.AddNodesRequest;
import se.sics.kompics.wan.hosts.events.AddNodesResponse;
import se.sics.kompics.wan.hosts.events.GetNodesRequest;
import se.sics.kompics.wan.hosts.events.GetNodesResponse;
import se.sics.kompics.wan.hosts.events.HostsInit;
import se.sics.kompics.wan.ssh.ExperimentHost;
import se.sics.kompics.wan.ssh.Host;
import se.sics.kompics.wan.ssh.SshCredentials;

public class Hosts  {

	private static Semaphore semaphore = new Semaphore(0);

	private static final int EVENT_COUNT = 1;
	
	
	public static void setTestObj(Hosts testObj) {
		TestHostsComponent.testObj = testObj;
	}
	

	public static class TestHostsComponent extends ComponentDefinition {
		
		private Component hostsComponent;
		
		private static Hosts testObj = null;
		
		private SshCredentials cred = 
			new SshCredentials("blah@sics.se", "jim", 
					"/home/jdowling/.ssh/id_rsa", "");

		private Set<Host> hosts = new HashSet<Host>();
		
		private int replies = 0;
		
		public TestHostsComponent() {

			hostsComponent = create(HostsXMLComponent.class);
			
			trigger(new HostsInit(new HashSet<Host>()),hostsComponent.getControl());
			
			subscribe(handleAddNodesResponse, hostsComponent.getPositive(HostsPort.class));
			subscribe(handleGetNodesResponse, hostsComponent.getPositive(HostsPort.class));
			
			subscribe(handleStart, control);
		}

		public Handler<Start> handleStart = new Handler<Start>() {
			public void handle(Start event) {
				
				Set<Host> newHosts = new HashSet<Host>();
				newHosts.add(new ExperimentHost("lqist.com"));
				trigger(new AddNodesRequest(newHosts), hostsComponent.getPositive(HostsPort.class));
				
			}
		};

		public Handler<AddNodesResponse> handleAddNodesResponse 
		= new Handler<AddNodesResponse>() {
			public void handle(AddNodesResponse event) {

				boolean res = event.getHostStatus();
				if (res == false)
				{
					testObj.fail(true);
				}
				trigger(new GetNodesRequest(), hostsComponent.getPositive(HostsPort.class));
				
			}
		};
		
		public Handler<GetNodesResponse> handleGetNodesResponse 
		= new Handler<GetNodesResponse>() {
			public void handle(GetNodesResponse event) {
				
				System.out.println("Received nodes: " + event.getHosts().size());
				hosts = event.getHosts();

				testObj.pass();
			}
		};
		
		
		

	};

	
	public Hosts() {
		
	}

//	@org.junit.Test
        @Ignore
	public void testPLab()
	{
		
		setTestObj(this);
		try {
			Configuration.init(new String[]{}, MasterConfiguration.class);
			Kompics.createAndStart(Hosts.TestHostsComponent.class, 2);
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			Hosts.semaphore.acquire(EVENT_COUNT);
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
