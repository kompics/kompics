package se.sics.kompics.core.scheduler;

public class Spinlock {

//	public TASSpinlock spinlock = new TASSpinlock();
	public TATASSpinlock spinlock = new TATASSpinlock();

	public void lock() {
		spinlock.lock();
	}

	public void unlock() {
		spinlock.unlock();
	}
}
