package se.sics.kompics.wan.daemonmaster.events;

import se.sics.kompics.wan.masterdaemon.events.*;

import se.sics.kompics.address.Address;
import se.sics.kompics.wan.job.Job;

public class JobFoundMsg extends DaemonResponseMsg {


	private static final long serialVersionUID = -4932112336632L;
	private final Job job;

	public JobFoundMsg(Job job, DaemonAddress src, Address dest) {
		super(src, dest);
                this.job = job;
	}

    public Job getJob() {
        return job;
    }
}
