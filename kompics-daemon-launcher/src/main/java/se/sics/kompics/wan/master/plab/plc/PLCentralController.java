package se.sics.kompics.wan.master.plab.plc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import se.sics.kompics.wan.master.plab.PlanetLabCredentials;
import se.sics.kompics.wan.master.plab.plc.comon.CoMonManager;


public class PLCentralController {

	private final PLCentralCache cache;

	private CoMonManager manager;

	public PLCentralController(PlanetLabCredentials cred) {
		this.cache = new PLCentralCache(cred);
	}

	public int addHostsToSlice(String[] hostnames) {
		ArrayList<PlanetLabHost> hosts = new ArrayList<PlanetLabHost>();

		for (int i = 0; i < hostnames.length; i++) {
			PlanetLabHost host = this.getHost(hostnames[i]);
			if (host != null) {
				hosts.add(host);
			}
		}

		PlanetLabHost[] newHosts = new PlanetLabHost[hosts.size()];
		newHosts = hosts.toArray(newHosts);
		return cache.addHostsToSlice(newHosts);
	}

	public PlanetLabHost[] getHostsNotInSlice() {

		PlanetLabHost[] hosts = cache.getHostsNotInSlice(cache.getAllHosts());
		System.out.println("controller got " + hosts.length);
		return hosts;
	}

	public PlanetLabHost[] getRandomSites(int num) {

		// query for all available sites

		ConcurrentHashMap<PlanetLabSite, Vector<PlanetLabHost>> siteMapping = cache
				.getSiteToHostMapping();
		Vector<PlanetLabSite> allSites = new Vector<PlanetLabSite>(siteMapping
				.keySet());
		Vector<PlanetLabSite> availableSites = new Vector<PlanetLabSite>();
		// only use the sites we haven't tried to connect to
		siteLoop: for (PlanetLabSite site : allSites) {
			Vector<PlanetLabHost> siteNodes = siteMapping.get(site);

			// loop through all nodes in that site, if any of them has been
			// tried to connect to, continue with the next site
			for (PlanetLabHost host : siteNodes) {
				if (host.getConnectionId() >= 0) {
					continue siteLoop;
				}
			}
			availableSites.add(site);

		}

		// if we queried for more than we can get...
		if (num > availableSites.size()) {
			num = availableSites.size();
		}
		// System.out.println("available sites:" + availableSites.size());
		Vector<PlanetLabHost> selected = new Vector<PlanetLabHost>();
		int tried = 0;
		while (selected.size() < num && availableSites.size() > 0) {
			// select a random site
			int rand = (int) (Math.random() * availableSites.size());
			PlanetLabSite site = availableSites.get(rand);

			// select a random node from that site
			PlanetLabHost host = this.getRandomHostFromSite(site);

			// there might not be any hosts in this site :-(
			if (host != null) {
				selected.add(host);
			}
			// remove that site from available sites
			availableSites.removeElementAt(rand);
			tried++;
		}
		// System.out.println("Selected: " + selected.size() + "/" + tried);
		return selected.toArray(new PlanetLabHost[selected.size()]);
		// return hosts;
	}

	public PlanetLabHost getRandomHostFromSite(PlanetLabHost oldHost) {
		return getRandomHostFromSite(cache.getHostToSiteMapping().get(oldHost));
	}

	public PlanetLabHost getRandomHostFromSite(PlanetLabSite site) {
		Vector<PlanetLabHost> allSiteHosts = cache.getSiteToHostMapping().get(
				site);
		// System.out.println("site: " + siteId + " num=" +
		// allSiteHosts.size());
		Vector<PlanetLabHost> availableSiteHosts = new Vector<PlanetLabHost>();
		for (PlanetLabHost host : allSiteHosts) {
			if (host.getConnectionId() < 0) {
				availableSiteHosts.add(host);
			}
		}

		// if no hosts are available, return null
		if (availableSiteHosts.size() == 0) {
			// System.out.println("no host available " + siteId);
			return null;
		}

		int rand = (int) (Math.random() * availableSiteHosts.size());
		return availableSiteHosts.get(rand);

	}

	/**
	 * Send a query for num random nodes. This function only returns nodes in
	 * "boot" state (more likely to be up)
	 * 
	 * @param num
	 *            number of requested nodes
	 * @return PlanetLabHost[] of num randomly selected nodes, or all available
	 *         nodes if num>=avalable nodes
	 */

	public PlanetLabHost[] getRandomNodes(int num) {
		// System.out.println("Selecting " + num + " nodes");
		Vector<PlanetLabHost> nodes = new Vector<PlanetLabHost>();
		PlanetLabHost[] allNodes = this.getAvailableHosts();

		Vector<PlanetLabHost> selected = new Vector<PlanetLabHost>();

		// add all nodes that doesn't have a connectionId (we haven't tried to
		// connect to the node yet)
		for (int i = 0; i < allNodes.length; i++) {
			if (allNodes[i].getConnectionId() < 0) {
				nodes.add(allNodes[i]);
			}
		}

		if (num >= nodes.size()) {
			return nodes.toArray(new PlanetLabHost[nodes.size()]);
		}

		while (selected.size() < num && nodes.size() > 0) {
			int rand = (int) (Math.random() * nodes.size());
			selected.add(nodes.remove(rand));
			// System.out.println(selected.get(selected.size() - 1).toString());
		}
		return selected.toArray(new PlanetLabHost[selected.size()]);
	}

	/**
	 * filter out all nodes that not are in "boot" state
	 * 
	 * @param hostSiteMapping
	 * @return
	 */
	private PlanetLabHost[] filterNodes(Set<PlanetLabHost> allHosts) {
		Vector<PlanetLabHost> filtered = new Vector<PlanetLabHost>();
		for (PlanetLabHost host : allHosts) {
			if (host.getBoot_state().equals("boot")) {
				// if (cache.getHostToSiteMapping().containsKey(host)) {
				filtered.add(host);
				// }
			}
		}

		return filtered.toArray(new PlanetLabHost[filtered.size()]);
	}

	/**
	 * returns all nodes that are in "boot" state
	 * 
	 * @return
	 */
	public PlanetLabHost[] getAllHosts() {
		return cache.getAllHosts();
	}

	public PlanetLabHost[] getAvailableHosts() {
		return this.filterNodes(cache.getHostToSiteMapping().keySet());
	}

	public PlanetLabHost getHost(String hostname) {
		InetAddress ipAddr;
		try {
			ipAddr = InetAddress.getByName(hostname);
			String ip = ipAddr.getHostAddress();

			PlanetLabHost[] allHosts = getAllHosts();
			for (PlanetLabHost host : allHosts) {
				if (host.getIp() != null) {
					if (host.getIp().equals(ip)) {
						return host;
					}
				}
			}
		} catch (UnknownHostException e) {
			// ignore this, this is handled by the researcher :-)
		}

		// no host found
		return null;
	}

	public PlanetLabHost getHost(int plcHostId) {
		return cache.getHost(plcHostId);
	}

	public PlanetLabSite getSite(PlanetLabHost host) {
		return cache.getSite(host.getSite());
	}

	public void setCoMonData(PlanetLabHost[] hosts) {
		manager = new CoMonManager(hosts);
		Thread t = new Thread(manager);
		t.setName("ComonQuery");
		t.start();
	}

	public double getCoMonProgress() {
		if (manager != null) {
			return manager.getProgress();
		}
		return 1;
	}

}
