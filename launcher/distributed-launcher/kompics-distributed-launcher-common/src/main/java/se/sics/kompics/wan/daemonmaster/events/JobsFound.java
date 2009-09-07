package se.sics.kompics.wan.daemonmaster.events;


import se.sics.kompics.Event;
import se.sics.kompics.wan.job.Job;

public class JobsFound extends Event {
	
       private final Job job;
	
	public JobsFound(Job job)
	{
		this.job = job;
	}


	public Job getJob() {
		return job;
	}
}
