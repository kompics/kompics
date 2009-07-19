package se.sics.kompics.wan.plab;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.plab.events.DnsResolverRequest;
import se.sics.kompics.wan.plab.events.DnsResolverResponse;
import se.sics.kompics.wan.plab.events.GetProgressRequest;
import se.sics.kompics.wan.plab.events.GetProgressResponse;

public class DnsResolverComponent extends ComponentDefinition {

	private Negative<DnsResolverPort> dnsPort = negative(DnsResolverPort.class);

	private final Logger logger = LoggerFactory.getLogger(DnsResolverComponent.class);


	private volatile double progress = 0.0;


	public DnsResolverComponent() {

		subscribe(handleDnsResolverRequest, dnsPort);
		subscribe(handleGetProgressRequest, dnsPort);
	}

	private Handler<DnsResolverRequest> handleDnsResolverRequest = new Handler<DnsResolverRequest>() {
		public void handle(DnsResolverRequest event) {
			
			
			
			ParallelDNSLookup dnsLookups = new ParallelDNSLookup(
					PlanetLabConfiguration.DNS_RESOLVER_MAX_THREADS, event);
			dnsLookups.performLookups();
			dnsLookups.join();
		}
	};

	private Handler<GetProgressRequest> handleGetProgressRequest = new Handler<GetProgressRequest>() {
		public void handle(GetProgressRequest event) {
			trigger(new GetProgressResponse(event, progress), dnsPort);
		}
	};


	private class ParallelDNSLookup {
		private final ExecutorService threadPool;

		private final Map<Integer,String> mapHosts;

		private final DnsResolverRequest event;
		
		private final ConcurrentLinkedQueue<Future<Map<Integer,InetAddress>>> tasks;

		public ParallelDNSLookup(int numThreads, DnsResolverRequest event) {
			
			this.mapHosts = event.getHosts();
			this.event = event;

			this.threadPool = Executors.newFixedThreadPool(numThreads);
			this.tasks = new ConcurrentLinkedQueue<Future<Map<Integer,InetAddress>>>();
		}

		public void performLookups() {
			for (Integer nodeId : mapHosts.keySet()) {
				String host = mapHosts.get(nodeId);
				Future<Map<Integer,InetAddress>> task = 
					threadPool.submit(new ResolveIPHandler(nodeId, host));
				tasks.add(task);
			}
		}

		public Map<Integer,InetAddress> join() {
			Map<Integer,InetAddress> addrs = new HashMap<Integer,InetAddress>();
			Future<Map<Integer,InetAddress>> task;
			try {
				int i=0;
				while ((task = tasks.poll()) != null) {
					Map<Integer,InetAddress> result =  task.get();
					addrs.putAll(result);
					i++;
					progress = mapHosts.size() / i ;
				}
				threadPool.shutdown();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			
			
			trigger(new DnsResolverResponse(event, addrs), dnsPort);

			
			return addrs;
		}
	}

	private class ResolveIPHandler implements Callable<Map<Integer,InetAddress>> {
		private String host;
		int nodeId;

		public ResolveIPHandler(int nodeId, String host) {
			this.host = host;
			this.nodeId = nodeId;
		}

		public Map<Integer,InetAddress> call() {

			InetAddress addr;
			try {
				addr = InetAddress.getByName(host);
			} catch (UnknownHostException e) {
				addr = null;
				logger.warn("Couldn't resolve hostname: {} . {}", host, e.getMessage());
			}
			Map<Integer,InetAddress> res = new HashMap<Integer,InetAddress>();
			res.put(nodeId, addr);
			return res;
		}

	}


}
