package se.sics.kompics.wan.services.events;


import java.util.HashSet;
import java.util.Set;

import se.sics.kompics.Request;
import se.sics.kompics.wan.services.ExperimentServicesComponent;
import se.sics.kompics.wan.ssh.Credentials;

public class ActionRequest extends Request {

	private final Set<String> hosts;
	
	private final ExperimentServicesComponent.Action action;
	
	private final Credentials cred;
	
	public ActionRequest(Credentials cred, Set<String> hosts, ExperimentServicesComponent.Action action) {
		this.cred = cred;
		this.hosts = new HashSet<String>(hosts);
		this.action = action;
	}
	
	public Set<String> getHosts() {
		return hosts;
	}
	
	@Override
	public int hashCode() {
		int x = action.ordinal();
		int y = 0;
		for (String h : hosts) {
			y += h.hashCode();
		}
		return x + y + 17;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
		{
			return true;
		}
		ActionRequest that = (ActionRequest) obj; 
		
		if (this.action != that.action) {
			return false;
		}
		for (String h1 : hosts)
		{
			boolean found = false;
			for (String h2 : that.hosts) {
				if (h1.compareToIgnoreCase(h2) == 0) {
					found = true;
				}
			}
			if (found == false) {
				return false;
			}
		}
		return true;
	}
	
	public ExperimentServicesComponent.Action getAction() {
		return action;
	}
	
	public Credentials getCred() {
		return cred;
	}
}
