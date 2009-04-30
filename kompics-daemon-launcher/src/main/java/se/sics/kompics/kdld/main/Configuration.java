package se.sics.kompics.kdld.main;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.epfd.diamondp.FailureDetectorConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.p2p.overlay.random.cyclon.CyclonConfiguration;
import se.sics.kompics.p2p.overlay.structured.chord.ChordConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

public class Configuration {
	public InetAddress ip = null;
	{
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
		}
	}
	int networkPort = 2210;
	int webPort = 8080;
	int bootId = Integer.MAX_VALUE;
	int monitorId = Integer.MAX_VALUE - 1;

	Address bootServerAddress = new Address(ip, networkPort, bootId);
	Address monitorServerAddress = new Address(ip, networkPort, monitorId);
	Address peer0Address = new Address(ip, networkPort, 0);

	int webRequestTimeout = 5000;
	int webThreads = 2;
	String webAddress = "http://" + ip.getHostAddress() + ":" + webPort + "/";
	String homePage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>"
			+ "<a href=\"" + webAddress + bootId + "/"
			+ "\">Bootstrap Server</a><br>" + "<a href=\"" + webAddress
			+ monitorId + "/" + "\">Monitor Server</a>";

	JettyWebServerInit jettyWebServerInit = new JettyWebServerInit(ip, webPort,
			webRequestTimeout, webThreads, homePage);

	BootstrapConfiguration bootConfiguration = new BootstrapConfiguration(
			bootServerAddress, 60000, 4000, 3, 30000, webPort);

	P2pMonitorConfiguration monitorConfiguration = new P2pMonitorConfiguration(
			monitorServerAddress, 10000, 2000, webPort);

	FailureDetectorConfiguration fdConfiguration = new FailureDetectorConfiguration(
			1000, 5000, 1000, 0);

	ChordConfiguration chordConfiguration = new ChordConfiguration(13, 13,
			1000, 1000, 3000, 20);

	CyclonConfiguration cyclonConfiguration = new CyclonConfiguration(5, 15,
			1000, 5000, new BigInteger("2").pow(13), 20);

	/* ========== Configuration for the DistributedSystemLauncher ========== */

	public BootstrapConfiguration getBootstrapConfiguration() {
		Address bootAddress = new Address(ip, networkPort - 1, bootId);
		return new BootstrapConfiguration(bootAddress, bootConfiguration
				.getCacheEvictAfter(),
				bootConfiguration.getClientRetryPeriod(), bootConfiguration
						.getClientRetryCount(), bootConfiguration
						.getClientKeepAlivePeriod(), webPort);
	}

	public P2pMonitorConfiguration getMonitorConfiguration() {
		Address monitorAddress = new Address(ip, networkPort - 2, monitorId);
		return new P2pMonitorConfiguration(monitorAddress, monitorConfiguration
				.getViewEvictAfter(), monitorConfiguration
				.getClientUpdatePeriod(), webPort);
	}

	public ChordConfiguration getChordConfiguration() {
		return chordConfiguration;
	}

	public FailureDetectorConfiguration getFailureDetectorConfiguration() {
		return fdConfiguration;
	}

	public String getPeerWebAddress(int id) {
		return "http://" + ip.getHostAddress() + ":" + (webPort + id) + "/";
	}

	public Address getPeerNetworkAddress(int id) {
		return new Address(ip, networkPort + id, id);
	}

	public JettyWebServerInit getPeerWebInit(int id) {
		String homePage = "<h2>Welcome to the Kompics P2P Framework!</h2>"
				+ "<a href=\"" + getBootstrapWebAddress() + bootId + "/"
				+ "\">Bootstrap Server</a><br>" + "<a href=\""
				+ getMonitorWebAddress() + monitorId + "/"
				+ "\">Monitor Server</a><br>" + "<a href=\""
				+ getPeerWebAddress(id) + id + "/" + "\">This Peer</a>";

		return new JettyWebServerInit(ip, webPort + id, webRequestTimeout,
				webThreads, homePage);
	}

	public String getBootstrapWebAddress() {
		return "http://" + ip.getHostAddress() + ":" + (webPort - 1) + "/";
	}

	public JettyWebServerInit getBootstrapWebInit() {
		String bootWebAddress = getBootstrapWebAddress();
		String homePage = "<h2>Welcome to the Kompics Bootstrap Server!</h2>"
				+ "<a href=\"" + bootWebAddress + bootId
				+ "/\">Bootstrap Server</a>";

		return new JettyWebServerInit(ip, webPort - 1, webRequestTimeout,
				webThreads, homePage);
	}

	public String getMonitorWebAddress() {
		return "http://" + ip.getHostAddress() + ":" + (webPort - 2) + "/";
	}

	public JettyWebServerInit getMonitorWebInit() {
		String monitorWebAddress = getMonitorWebAddress();
		String homePage = "<h2>Welcome to the Kompics Monitor Server!</h2>"
				+ "<a href=\"" + monitorWebAddress + monitorId
				+ "/\">Chord Monitor Server</a>";

		return new JettyWebServerInit(ip, webPort - 2, webRequestTimeout,
				webThreads, homePage);
	}
}
