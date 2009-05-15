package se.sics.kompics.kdld.daemon;

import java.util.Set;

import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.job.Job;

public class ListJobsLoadedResponseMsg extends DaemonResponseMessage {

	private static final long serialVersionUID = -5282256565613009927L;

	private final Set<Job> setJobs;

	public ListJobsLoadedResponseMsg(Set<Job> setJobs, DaemonAddress src, Address dest) {
		super(src, dest);
		this.setJobs = setJobs;
	}

	public Set<Job> getSetJobs() {
		return setJobs;
	}
	
}
