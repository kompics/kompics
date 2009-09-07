package se.sics.kompics.wan.hosts.events;

import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Init;
import se.sics.kompics.wan.ssh.Host;

/**
 * The <code>HostsInit</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class HostsInit extends Init {

	private Set<Host> hosts = new HashSet<Host>();
	
	public HostsInit(Set<Host> hosts) {
		super();
		this.hosts = hosts;
	}
		
	public HostsInit() {
		super();
	}

	public Set<Host> getHosts() {
		return hosts;
	}
}
