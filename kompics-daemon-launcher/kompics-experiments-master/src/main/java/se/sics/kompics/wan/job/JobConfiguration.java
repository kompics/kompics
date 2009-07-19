package se.sics.kompics.wan.job;

import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

public class JobConfiguration {
	public InetAddress ip = null;
	{
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
		}
	}
	
	private final int daemonId;
	private final int networkPort;
	private final int webPort;
	private int bootId = Integer.MAX_VALUE;
	private int monitorId = 0;
	private Address bootServerAddress;
	private Address monitorServerAddress;
	private Address peer0Address;
	
	private String webAddress;
	
	private JettyWebServerInit jettyWebServerInit;

	private BootstrapConfiguration bootConfiguration;

	private P2pMonitorConfiguration monitorConfiguration;
	
	public JobConfiguration(int daemonId, int networkPort, int webPort) {
		super();
		this.daemonId = daemonId;
		this.networkPort = networkPort;
		this.webPort = webPort;
		bootServerAddress = new Address(ip, networkPort, bootId);
		monitorServerAddress = new Address(ip, networkPort,
				monitorId);
		peer0Address = new Address(ip, networkPort, 0);
		webAddress = "http://" + ip.getHostAddress() + ":" + webPort + "/";
		jettyWebServerInit = new JettyWebServerInit(ip, webPort,
				webRequestTimeout, webThreads, homePage);
		bootConfiguration = new BootstrapConfiguration(
				bootServerAddress, 60000, 4000, 3, 30000, webPort);
		monitorConfiguration = new P2pMonitorConfiguration(
				monitorServerAddress, 10000, 5000, webPort);
	}


	int webRequestTimeout = 10000;
	int webThreads = 2;
	String homePage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>"
			+ "<a href=\"" + webAddress + bootId + "/"
			+ "\">Bootstrap Server</a><br>";

}
