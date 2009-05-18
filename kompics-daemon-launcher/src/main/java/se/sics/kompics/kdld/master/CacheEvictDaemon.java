package se.sics.kompics.kdld.master;

import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public final class CacheEvictDaemon extends Timeout {

	private final DaemonAddress daemonAddress;

	private final long epoch;

	public CacheEvictDaemon(ScheduleTimeout request, DaemonAddress peerAddress, long epoch) {
		super(request);
		this.daemonAddress = peerAddress;
		this.epoch = epoch;
	}

	public DaemonAddress getDaemonAddress() {
		return daemonAddress;
	}

public long getEpoch() {
		return epoch;
	}
}